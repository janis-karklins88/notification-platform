package lv.janis.notification_platform.ingest.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lv.janis.notification_platform.config.JpaAuditingConfig;
import lv.janis.notification_platform.ingest.adapter.out.persistence.EventJpaRepository;
import lv.janis.notification_platform.ingest.adapter.out.persistence.EventRepositoryAdapter;
import lv.janis.notification_platform.ingest.application.port.in.IngestCommand;
import lv.janis.notification_platform.ingest.application.port.in.IngestResult;
import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.outbox.adapter.out.persistence.OutboxEventJpaRepository;
import lv.janis.notification_platform.outbox.adapter.out.persistence.OutboxEventRepositoryAdapter;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;
import lv.janis.notification_platform.shared.metrics.NotificationMetrics;
import lv.janis.notification_platform.tenant.adapter.out.persistence.TenantJpaRepository;
import lv.janis.notification_platform.tenant.adapter.out.persistence.TenantRepositoryAdapter;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

@DataJpaTest
@Import({ JpaAuditingConfig.class, IngestServiceIntegrationTest.Config.class })
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:notification_platform_ingest_service;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false" })
@AutoConfigureTestDatabase(replace = Replace.NONE)
class IngestServiceIntegrationTest {
  private static final Instant NOW = Instant.parse("2026-02-08T10:00:00Z");

  @Autowired
  private IngestService ingestService;

  @Autowired
  private TenantJpaRepository tenantRepository;

  @Autowired
  private EventJpaRepository eventRepository;

  @Autowired
  private OutboxEventJpaRepository outboxEventRepository;

  @MockitoBean
  private NotificationMetrics notificationMetrics;

  @Test
  void ingestPersistsEventAndMatchingOutboxEvent() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-ingest", "Tenant Ingest", TenantStatus.ACTIVE));
    ObjectNode payload = new ObjectMapper().createObjectNode().put("orderId", "123");

    IngestResult result = ingestService.ingest(new IngestCommand(
        tenant.getId(),
        "order.created",
        payload,
        " idem-1 ",
        "api",
        "trace-1"));

    assertFalse(result.duplicate());

    Optional<Event> storedEvent = eventRepository.findByTenant_IdAndIdempotencyKey(tenant.getId(), "idem-1");
    assertTrue(storedEvent.isPresent());
    assertEquals(result.eventId(), storedEvent.get().getId());
    assertEquals("order.created", storedEvent.get().getEventType());
    assertEquals(payload, storedEvent.get().getPayload());

    Optional<OutboxEvent> storedOutbox = outboxEventRepository
        .findByTenant_IdAndAggregateTypeAndAggregateIdAndEventType(
            tenant.getId(),
            OutboxEventAggregateType.EVENT,
            storedEvent.get().getId(),
            OutboxEventType.EVENT_ACCEPTED);
    assertTrue(storedOutbox.isPresent());
    assertEquals(OutboxStatus.PENDING, storedOutbox.get().getStatus());
    assertEquals(storedEvent.get().getId(), storedOutbox.get().getAggregateId());
    assertEquals(storedEvent.get().getId().toString(), storedOutbox.get().getPayload().get("eventId").asText());
    assertEquals(tenant.getId().toString(), storedOutbox.get().getPayload().get("tenantId").asText());
    assertEquals("order.created", storedOutbox.get().getPayload().get("eventType").asText());

    verify(notificationMetrics).incrementEventAccepted();
  }

  @Test
  void ingestWithExistingIdempotencyKeyReturnsDuplicateAndDoesNotCreateSecondOutboxEvent() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-ingest-idem", "Tenant Ingest Idem", TenantStatus.ACTIVE));
    ObjectNode payload = new ObjectMapper().createObjectNode().put("orderId", "123");

    IngestResult first = ingestService.ingest(new IngestCommand(
        tenant.getId(),
        "order.created",
        payload,
        "same-key",
        "api",
        "trace-1"));

    IngestResult second = ingestService.ingest(new IngestCommand(
        tenant.getId(),
        "order.created",
        payload,
        " same-key ",
        "api",
        "trace-2"));

    assertFalse(first.duplicate());
    assertTrue(second.duplicate());
    assertEquals(first.eventId(), second.eventId());
    assertEquals(1, eventRepository.count());
    assertEquals(1, outboxEventRepository.count());
    verify(notificationMetrics, times(1)).incrementEventAccepted();
  }

  @TestConfiguration
  static class Config {
    @Bean
    IngestService ingestService(
        EventRepositoryAdapter eventRepositoryAdapter,
        TenantRepositoryAdapter tenantRepositoryAdapter,
        OutboxEventRepositoryAdapter outboxEventRepositoryAdapter,
        ObjectMapper objectMapper,
        Clock clock,
        NotificationMetrics notificationMetrics) {
      return new IngestService(
          eventRepositoryAdapter,
          tenantRepositoryAdapter,
          outboxEventRepositoryAdapter,
          objectMapper,
          clock,
          notificationMetrics);
    }

    @Bean
    EventRepositoryAdapter eventRepositoryAdapter(EventJpaRepository eventJpaRepository) {
      return new EventRepositoryAdapter(eventJpaRepository);
    }

    @Bean
    TenantRepositoryAdapter tenantRepositoryAdapter(TenantJpaRepository tenantJpaRepository) {
      return new TenantRepositoryAdapter(tenantJpaRepository);
    }

    @Bean
    OutboxEventRepositoryAdapter outboxEventRepositoryAdapter(OutboxEventJpaRepository outboxEventJpaRepository) {
      return new OutboxEventRepositoryAdapter(outboxEventJpaRepository);
    }

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    Clock clock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
