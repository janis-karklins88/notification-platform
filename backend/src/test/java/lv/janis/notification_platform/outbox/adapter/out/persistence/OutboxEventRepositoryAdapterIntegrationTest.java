package lv.janis.notification_platform.outbox.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Optional;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import lv.janis.notification_platform.config.JpaAuditingConfig;
import lv.janis.notification_platform.outbox.application.port.out.OutboxFilter;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;
import lv.janis.notification_platform.tenant.adapter.out.persistence.TenantJpaRepository;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

@DataJpaTest
@Import({JpaAuditingConfig.class, OutboxEventRepositoryAdapterIntegrationTest.Config.class})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:notification_platform_outbox_repo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class OutboxEventRepositoryAdapterIntegrationTest {

  @Autowired
  private OutboxEventRepositoryAdapter outboxEventRepositoryAdapter;

  @Autowired
  private TenantJpaRepository tenantRepository;

  @Autowired
  private OutboxEventJpaRepository outboxEventRepository;

  @Test
  void findAllSupportsCombinedFilters() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-outbox-filter", "Tenant Outbox", TenantStatus.ACTIVE));
    Tenant otherTenant = tenantRepository.save(new Tenant("tenant-outbox-other", "Tenant Outbox Other", TenantStatus.ACTIVE));

    OutboxEvent kept = saveEvent(tenant, OutboxStatus.PENDING, OutboxEventAggregateType.EVENT, OutboxEventType.EVENT_ACCEPTED);
    saveEvent(tenant, OutboxStatus.FAILED, OutboxEventAggregateType.EVENT, OutboxEventType.DELIVERY_CREATED_WEBHOOK);
    saveEvent(otherTenant, OutboxStatus.PENDING, OutboxEventAggregateType.EVENT, OutboxEventType.EVENT_ACCEPTED);

    OutboxFilter filter = new OutboxFilter(
        OutboxStatus.PENDING,
        tenant.getId(),
        OutboxEventType.EVENT_ACCEPTED,
        OutboxEventAggregateType.EVENT,
        kept.getAggregateId(),
        null,
        null);

    Page<OutboxEvent> result = outboxEventRepositoryAdapter.findAll(filter, PageRequest.of(0, 25));

    assertEquals(1, result.getTotalElements());
    assertEquals(kept.getId(), result.getContent().get(0).getId());
  }

  @Test
  void findAllSupportsTimeRangeFilter() throws InterruptedException {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-outbox-chrono", "Tenant Outbox Chrono", TenantStatus.ACTIVE));
    OutboxEvent first = saveEvent(tenant, OutboxStatus.PENDING, OutboxEventAggregateType.DELIVERY, OutboxEventType.DELIVERY_CREATED_WEBHOOK);
    Thread.sleep(700);
    OutboxEvent second = saveEvent(tenant, OutboxStatus.PENDING, OutboxEventAggregateType.DELIVERY, OutboxEventType.DELIVERY_CREATED_WEBHOOK);

    Instant from = second.getCreatedAt().minusMillis(1);
    Instant to = second.getCreatedAt().plusMillis(1);

    OutboxFilter filter = new OutboxFilter(
        OutboxStatus.PENDING,
        tenant.getId(),
        OutboxEventType.DELIVERY_CREATED_WEBHOOK,
        OutboxEventAggregateType.DELIVERY,
        null,
        from,
        to);

    var filtered = outboxEventRepositoryAdapter.findAll(filter, PageRequest.of(0, 50)).getContent();
    assertEquals(1, filtered.size());
    assertEquals(second.getId(), filtered.get(0).getId());
    assertEquals(first.getCreatedAt().isBefore(from), true);
  }

  @Test
  void findByTenantIdAndAggregateTypeAndAggregateIdAndEventTypeSupportsNegativeLookup() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-outbox-single", "Tenant Outbox Single", TenantStatus.ACTIVE));

    OutboxEvent match = saveEvent(tenant, OutboxStatus.PENDING, OutboxEventAggregateType.EVENT, OutboxEventType.EVENT_ACCEPTED);

    Optional<OutboxEvent> found = outboxEventRepositoryAdapter.findByTenantIdAndAggregateTypeAndAggregateIdAndEventType(
        tenant.getId(),
        OutboxEventAggregateType.EVENT,
        match.getAggregateId(),
        OutboxEventType.EVENT_ACCEPTED);
    assertEquals(match.getId(), found.get().getId());

    Optional<OutboxEvent> notFound =
        outboxEventRepositoryAdapter.findByTenantIdAndAggregateTypeAndAggregateIdAndEventType(
            tenant.getId(),
            OutboxEventAggregateType.DELIVERY,
            match.getAggregateId(),
            OutboxEventType.EVENT_ACCEPTED);
    assertEquals(Optional.empty(), notFound);
  }

  private OutboxEvent saveEvent(
      Tenant tenant,
      OutboxStatus status,
      OutboxEventAggregateType aggregateType,
      OutboxEventType eventType) {
    OutboxEvent event = new OutboxEvent(tenant, aggregateType, UUID.randomUUID(), eventType, payload("payload"), Instant.now());
    if (status == OutboxStatus.PUBLISHED) {
      event.markPublished(Instant.now());
    } else if (status == OutboxStatus.IN_PROGRESS) {
      event.markInProgress(Instant.now());
    } else if (status == OutboxStatus.FAILED) {
      event.markFailed("temporary", Instant.now());
    }
    return outboxEventRepository.save(event);
  }

  private static JsonNode payload(String text) {
    return new TextNode(text);
  }

  @TestConfiguration
  static class Config {
    @Bean
    OutboxEventRepositoryAdapter outboxEventRepositoryAdapter(OutboxEventJpaRepository outboxEventJpaRepository) {
      return new OutboxEventRepositoryAdapter(outboxEventJpaRepository);
    }
  }
}
