package lv.janis.notification_platform.adminapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import lv.janis.notification_platform.config.JpaAuditingConfig;
import lv.janis.notification_platform.outbox.adapter.out.persistence.OutboxEventRepositoryAdapter;
import lv.janis.notification_platform.adminapi.application.port.in.ListOutboxEventsQuery;
import lv.janis.notification_platform.outbox.application.port.out.OutboxFilter;
import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;
import lv.janis.notification_platform.outbox.adapter.out.persistence.OutboxEventJpaRepository;
import lv.janis.notification_platform.tenant.adapter.out.persistence.TenantJpaRepository;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

@DataJpaTest
@Import({ JpaAuditingConfig.class, OutboxMonitoringServiceIntegrationTest.Config.class })
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:notification_platform_outbox_monitoring;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false" })
@AutoConfigureTestDatabase(replace = Replace.NONE)
class OutboxMonitoringServiceIntegrationTest {

  @Autowired
  private OutboxEventRepositoryAdapter outboxEventRepositoryAdapter;

  @Autowired
  private TenantJpaRepository tenantRepository;

  @Autowired
  private OutboxMonitoringService outboxMonitoringService;

  @Test
  void listOutboxEventsFiltersByStatusTenantAndEventTypeUsingJpaSpecs() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-alpha", "Tenant Alpha", TenantStatus.ACTIVE));
    Tenant otherTenant = tenantRepository.save(new Tenant("tenant-beta", "Tenant Beta", TenantStatus.ACTIVE));

    OutboxEvent keptPending = saveWithStatusAndType(
        tenant,
        OutboxEventAggregateType.EVENT,
        OutboxEventType.EVENT_ACCEPTED,
        OutboxStatus.PENDING);

    saveWithStatusAndType(
        tenant,
        OutboxEventAggregateType.DELIVERY,
        OutboxEventType.DELIVERY_CREATED_WEBHOOK,
        OutboxStatus.PENDING);

    saveWithStatusAndType(
        otherTenant,
        OutboxEventAggregateType.EVENT,
        OutboxEventType.EVENT_ACCEPTED,
        OutboxStatus.PUBLISHED);

    var query = new ListOutboxEventsQuery(
        0,
        100,
        OutboxStatus.PENDING,
        tenant.getId(),
        OutboxEventType.EVENT_ACCEPTED,
        OutboxEventAggregateType.EVENT,
        keptPending.getAggregateId(),
        null,
        null);

    Page<OutboxEvent> pendingForTenant = outboxMonitoringService.listOutboxEvents(query);
    assertEquals(1, pendingForTenant.getTotalElements());
    assertEquals(keptPending.getId(), pendingForTenant.getContent().get(0).getId());
  }

  @Test
  void listOutboxEventsSupportsTimeRangeFilter() throws InterruptedException {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-chrono", "Tenant Chrono", TenantStatus.ACTIVE));

    saveWithStatusAndType(
        tenant,
        OutboxEventAggregateType.EVENT,
        OutboxEventType.EVENT_ACCEPTED,
        OutboxStatus.PENDING);
    Thread.sleep(1200);

    OutboxEvent second = saveWithStatusAndType(
        tenant,
        OutboxEventAggregateType.EVENT,
        OutboxEventType.EVENT_ACCEPTED,
        OutboxStatus.PENDING);

    Instant from = second.getCreatedAt().minusMillis(1);
    Instant to = second.getCreatedAt().plusMillis(1);
    OutboxFilter narrow = new OutboxFilter(
        OutboxStatus.PENDING,
        tenant.getId(),
        OutboxEventType.EVENT_ACCEPTED,
        OutboxEventAggregateType.EVENT,
        null,
        from,
        to);
    List<OutboxEvent> filtered = outboxEventRepositoryAdapter
        .findAll(narrow, org.springframework.data.domain.PageRequest.of(0, 50)).toList();

    assertEquals(1, filtered.size());
    assertEquals(second.getId(), filtered.get(0).getId());
  }

  private OutboxEvent saveWithStatusAndType(
      Tenant tenant,
      OutboxEventAggregateType aggregateType,
      OutboxEventType eventType,
      OutboxStatus initialStatus) {
    JsonNode payload = new TextNode("payload");
    OutboxEvent event = new OutboxEvent(
        tenant,
        aggregateType,
        UUID.randomUUID(),
        eventType,
        payload,
        Instant.now());

    if (initialStatus == OutboxStatus.PUBLISHED) {
      event.markPublished(Instant.now());
    } else if (initialStatus == OutboxStatus.FAILED) {
      event.markFailed("retry", Instant.now());
    } else if (initialStatus == OutboxStatus.IN_PROGRESS) {
      event.markInProgress(Instant.now());
    } else if (initialStatus == OutboxStatus.PENDING) {
      // keep defaults
    }

    return outboxEventRepositoryAdapter.save(event);
  }

  @TestConfiguration
  static class Config {
    @Bean
    OutboxMonitoringService outboxMonitoringService(OutboxEventRepositoryPort repositoryPort) {
      return new OutboxMonitoringService(repositoryPort);
    }

    @Bean
    OutboxEventRepositoryAdapter outboxEventRepositoryAdapter(OutboxEventJpaRepository outboxEventJpaRepository) {
      return new OutboxEventRepositoryAdapter(outboxEventJpaRepository);
    }
  }
}
