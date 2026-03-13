package lv.janis.notification_platform.delivery.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;

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
import lv.janis.notification_platform.delivery.application.port.out.DeliveryFilter;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.DeliveryStatus;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.ingest.adapter.out.persistence.EventJpaRepository;
import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.routing.adapter.out.persistence.SubscriptionJpaRepository;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.tenant.adapter.out.persistence.TenantJpaRepository;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

@DataJpaTest
@Import({JpaAuditingConfig.class, DeliveryRepositoryAdapterIntegrationTest.Config.class})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:notification_platform_delivery_repo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class DeliveryRepositoryAdapterIntegrationTest {

  @Autowired
  private DeliveryRepositoryAdapter deliveryRepositoryAdapter;

  @Autowired
  private TenantJpaRepository tenantRepository;

  @Autowired
  private EndpointJpaRepository endpointRepository;

  @Autowired
  private SubscriptionJpaRepository subscriptionRepository;

  @Autowired
  private EventJpaRepository eventRepository;

  @Test
  void findAllSupportsCombinedFilters() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-filter", "Tenant Filter", TenantStatus.ACTIVE));
    Tenant otherTenant = tenantRepository.save(new Tenant("tenant-other", "Tenant Other", TenantStatus.ACTIVE));

    Endpoint webhookEndpoint = endpointRepository.save(new Endpoint(tenant, EndpointType.WEBHOOK, config("webhook")));
    Endpoint smsEndpoint = endpointRepository.save(new Endpoint(tenant, EndpointType.SMS, config("sms")));
    Endpoint otherEndpoint = endpointRepository.save(new Endpoint(otherTenant, EndpointType.WEBHOOK, config("other")));

    Subscription webhookSubscription = subscriptionRepository.save(new Subscription(tenant, "event.webhook", webhookEndpoint));
    Subscription smsSubscription = subscriptionRepository.save(new Subscription(tenant, "event.sms", smsEndpoint));
    Subscription otherSubscription = subscriptionRepository.save(new Subscription(otherTenant, "event.other", otherEndpoint));

    Event webhookEvent = eventRepository.save(new Event(tenant, "event.webhook", "idem-webhook", config("payload"), "src", "trace"));
    Event smsEvent = eventRepository.save(new Event(tenant, "event.sms", "idem-sms", config("payload"), "src", "trace"));
    Event otherEvent = eventRepository.save(new Event(otherTenant, "event.other", "idem-other", config("payload"), "src", "trace"));

    Delivery matchingDelivery = deliveryRepositoryAdapter.save(new Delivery(tenant, webhookEvent, webhookSubscription, webhookEndpoint));
    Delivery ignoredStatus = deliveryRepositoryAdapter.save(new Delivery(tenant, smsEvent, smsSubscription, smsEndpoint));
    ignoredStatus.markDelivered(Instant.parse("2026-01-02T00:00:00Z"));
    deliveryRepositoryAdapter.save(ignoredStatus);

    deliveryRepositoryAdapter.save(new Delivery(otherTenant, otherEvent, otherSubscription, otherEndpoint));

    DeliveryFilter filter = new DeliveryFilter(
        DeliveryStatus.PENDING,
        tenant.getId(),
        webhookEvent.getId(),
        webhookEndpoint.getId(),
        EndpointType.WEBHOOK,
        null,
        null);

    Page<Delivery> results =
        deliveryRepositoryAdapter.findAll(filter, PageRequest.of(0, 50));

    assertEquals(1, results.getTotalElements());
    assertEquals(matchingDelivery.getId(), results.getContent().get(0).getId());
  }

  @Test
  void findAllSupportsTimeRangeFilters() throws InterruptedException {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-chrono", "Tenant Chrono", TenantStatus.ACTIVE));

    Endpoint endpoint = endpointRepository.save(new Endpoint(tenant, EndpointType.WEBHOOK, config("webhook")));
    Subscription subscription = subscriptionRepository.save(new Subscription(tenant, "event.time", endpoint));
    Event firstEvent = eventRepository.save(new Event(tenant, "event.time", "idem-first", config("first"), "src", "trace"));
    Event secondEvent = eventRepository.save(new Event(tenant, "event.time", "idem-second", config("second"), "src", "trace"));

    Delivery firstDelivery = deliveryRepositoryAdapter.save(new Delivery(tenant, firstEvent, subscription, endpoint));
    Thread.sleep(800);
    Delivery secondDelivery = deliveryRepositoryAdapter.save(new Delivery(tenant, secondEvent, subscription, endpoint));

    Instant from = secondDelivery.getCreatedAt().minusMillis(1);
    Instant to = secondDelivery.getCreatedAt().plusMillis(1);

    DeliveryFilter filter = new DeliveryFilter(
        null,
        tenant.getId(),
        null,
        null,
        null,
        from,
        to);

    var filtered = deliveryRepositoryAdapter.findAll(filter, PageRequest.of(0, 50)).getContent();

    assertEquals(1, filtered.size());
    assertEquals(secondDelivery.getId(), filtered.get(0).getId());

    assertTrue(firstDelivery.getCreatedAt().isBefore(from));
  }

  @Test
  void findByTenantIdAndEventIdAndSubscriptionIdSupportsPositiveAndNegativeLookup() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-find", "Tenant Find", TenantStatus.ACTIVE));
    tenantRepository.save(new Tenant("tenant-find-other", "Tenant Find Other", TenantStatus.ACTIVE));

    Endpoint endpoint = endpointRepository.save(new Endpoint(tenant, EndpointType.WEBHOOK, config("webhook")));
    Subscription subscription = subscriptionRepository.save(new Subscription(tenant, "event.find", endpoint));
    Subscription otherSubscription = subscriptionRepository.save(new Subscription(tenant, "event.other", endpoint));

    Event matchEvent = eventRepository.save(new Event(tenant, "event.find", "idem-match", config("payload"), "src", "trace"));
    Event otherEvent = eventRepository.save(new Event(tenant, "event.other", "idem-other", config("payload"), "src", "trace"));

    Delivery match = deliveryRepositoryAdapter.save(new Delivery(tenant, matchEvent, subscription, endpoint));
    deliveryRepositoryAdapter.save(new Delivery(tenant, otherEvent, otherSubscription, endpoint));

    Optional<Delivery> found =
        deliveryRepositoryAdapter.findByTenantIdAndEventIdAndSubscriptionId(tenant.getId(), matchEvent.getId(), subscription.getId());

    assertTrue(found.isPresent());
    assertEquals(match.getId(), found.get().getId());

    Optional<Delivery> notFound =
        deliveryRepositoryAdapter.findByTenantIdAndEventIdAndSubscriptionId(tenant.getId(), matchEvent.getId(), otherSubscription.getId());
    assertTrue(notFound.isEmpty());
  }

  private static JsonNode config(String text) {
    return TextNode.valueOf(text);
  }

  @TestConfiguration
  static class Config {
    @Bean
    DeliveryRepositoryAdapter deliveryRepositoryAdapter(DeliveryJpaRepository deliveryJpaRepository) {
      return new DeliveryRepositoryAdapter(deliveryJpaRepository);
    }
  }
}
