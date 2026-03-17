package lv.janis.notification_platform.delivery.application.service;

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
import lv.janis.notification_platform.delivery.adapter.out.persistence.DeliveryJpaRepository;
import lv.janis.notification_platform.delivery.adapter.out.persistence.DeliveryRepositoryAdapter;
import lv.janis.notification_platform.delivery.adapter.out.persistence.EndpointJpaRepository;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.DeliveryStatus;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.ingest.adapter.out.persistence.EventJpaRepository;
import lv.janis.notification_platform.ingest.adapter.out.persistence.EventRepositoryAdapter;
import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.ingest.domain.EventStatus;
import lv.janis.notification_platform.outbox.adapter.out.persistence.OutboxEventJpaRepository;
import lv.janis.notification_platform.outbox.adapter.out.persistence.OutboxEventRepositoryAdapter;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;
import lv.janis.notification_platform.routing.adapter.out.persistence.SubscriptionJpaRepository;
import lv.janis.notification_platform.routing.adapter.out.persistence.SubscriptionRepositoryAdapter;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.shared.metrics.NotificationMetrics;
import lv.janis.notification_platform.tenant.adapter.out.persistence.TenantJpaRepository;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

@DataJpaTest
@Import({JpaAuditingConfig.class, DeliveryServiceIntegrationTest.Config.class})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:notification_platform_delivery_service;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class DeliveryServiceIntegrationTest {
  private static final Instant NOW = Instant.parse("2026-02-08T11:00:00Z");

  @Autowired
  private DeliveryService deliveryService;

  @Autowired
  private TenantJpaRepository tenantRepository;

  @Autowired
  private EventJpaRepository eventRepository;

  @Autowired
  private EndpointJpaRepository endpointRepository;

  @Autowired
  private SubscriptionJpaRepository subscriptionRepository;

  @Autowired
  private DeliveryJpaRepository deliveryRepository;

  @Autowired
  private OutboxEventJpaRepository outboxEventRepository;

  @MockitoBean
  private NotificationMetrics notificationMetrics;

  @Test
  void routeEventCreatesDeliveriesAndDeliveryOutboxEventsForActiveSubscriptions() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-route", "Tenant Route", TenantStatus.ACTIVE));
    Event event = eventRepository.save(new Event(
        tenant,
        "order.created",
        "idem-route",
        payload("orderId", "123"),
        "api",
        "trace-route"));

    Endpoint emailEndpoint = endpointRepository.save(new Endpoint(
        tenant,
        EndpointType.EMAIL,
        payload("to", "ops@example.com")));
    Endpoint webhookEndpoint = endpointRepository.save(new Endpoint(
        tenant,
        EndpointType.WEBHOOK,
        payload("url", "https://example.test/hook")));
    Endpoint pausedEndpoint = endpointRepository.save(new Endpoint(
        tenant,
        EndpointType.WEBHOOK,
        payload("url", "https://example.test/paused")));

    Subscription emailSubscription = subscriptionRepository.save(new Subscription(tenant, "order.created", emailEndpoint));
    Subscription webhookSubscription = subscriptionRepository.save(new Subscription(tenant, "order.created", webhookEndpoint));
    Subscription pausedSubscription = new Subscription(tenant, "order.created", pausedEndpoint);
    pausedSubscription.pause();
    subscriptionRepository.save(pausedSubscription);

    deliveryService.routeEvent(event.getId());

    Event routedEvent = eventRepository.findById(event.getId()).orElseThrow();
    assertEquals(EventStatus.ROUTED, routedEvent.getStatus());
    assertEquals(2, deliveryRepository.count());
    assertEquals(2, outboxEventRepository.count());

    Optional<Delivery> storedEmailDelivery = deliveryRepository.findByTenant_IdAndEvent_IdAndSubscription_Id(
        tenant.getId(),
        event.getId(),
        emailSubscription.getId());
    assertTrue(storedEmailDelivery.isPresent());
    assertEquals(DeliveryStatus.PENDING, storedEmailDelivery.get().getStatus());
    assertEquals(emailEndpoint.getId(), storedEmailDelivery.get().getEndpointId());

    Optional<Delivery> storedWebhookDelivery = deliveryRepository.findByTenant_IdAndEvent_IdAndSubscription_Id(
        tenant.getId(),
        event.getId(),
        webhookSubscription.getId());
    assertTrue(storedWebhookDelivery.isPresent());
    assertEquals(DeliveryStatus.PENDING, storedWebhookDelivery.get().getStatus());
    assertEquals(webhookEndpoint.getId(), storedWebhookDelivery.get().getEndpointId());

    assertFalse(deliveryRepository.findByTenant_IdAndEvent_IdAndSubscription_Id(
        tenant.getId(),
        event.getId(),
        pausedSubscription.getId()).isPresent());

    Optional<OutboxEvent> storedEmailOutbox = outboxEventRepository.findByTenant_IdAndAggregateTypeAndAggregateIdAndEventType(
        tenant.getId(),
        OutboxEventAggregateType.DELIVERY,
        storedEmailDelivery.get().getId(),
        OutboxEventType.DELIVERY_CREATED_EMAIL);
    assertTrue(storedEmailOutbox.isPresent());
    assertEquals(OutboxStatus.PENDING, storedEmailOutbox.get().getStatus());
    assertEquals(storedEmailDelivery.get().getId().toString(), storedEmailOutbox.get().getPayload().get("deliveryId").asText());
    assertEquals(event.getId().toString(), storedEmailOutbox.get().getPayload().get("eventId").asText());

    Optional<OutboxEvent> storedWebhookOutbox = outboxEventRepository.findByTenant_IdAndAggregateTypeAndAggregateIdAndEventType(
        tenant.getId(),
        OutboxEventAggregateType.DELIVERY,
        storedWebhookDelivery.get().getId(),
        OutboxEventType.DELIVERY_CREATED_WEBHOOK);
    assertTrue(storedWebhookOutbox.isPresent());
    assertEquals(OutboxStatus.PENDING, storedWebhookOutbox.get().getStatus());
    assertEquals(storedWebhookDelivery.get().getId().toString(), storedWebhookOutbox.get().getPayload().get("deliveryId").asText());
    assertEquals(event.getId().toString(), storedWebhookOutbox.get().getPayload().get("eventId").asText());

    verify(notificationMetrics).incrementDeliveriesCreated(2);
  }

  @Test
  void routeEventDoesNotCreateDuplicateDeliveriesWhenCalledTwice() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-route-idem", "Tenant Route Idem", TenantStatus.ACTIVE));
    Event event = eventRepository.save(new Event(
        tenant,
        "order.created",
        "idem-route-2",
        payload("orderId", "456"),
        "api",
        "trace-route-2"));

    Endpoint webhookEndpoint = endpointRepository.save(new Endpoint(
        tenant,
        EndpointType.WEBHOOK,
        payload("url", "https://example.test/once")));
    Subscription subscription = subscriptionRepository.save(new Subscription(tenant, "order.created", webhookEndpoint));

    deliveryService.routeEvent(event.getId());
    deliveryService.routeEvent(event.getId());

    Event routedEvent = eventRepository.findById(event.getId()).orElseThrow();
    assertEquals(EventStatus.ROUTED, routedEvent.getStatus());
    assertEquals(1, deliveryRepository.count());
    assertEquals(1, outboxEventRepository.count());
    assertTrue(deliveryRepository.findByTenant_IdAndEvent_IdAndSubscription_Id(
        tenant.getId(),
        event.getId(),
        subscription.getId()).isPresent());

    verify(notificationMetrics, times(1)).incrementDeliveriesCreated(1);
  }

  private ObjectNode payload(String key, String value) {
    return new ObjectMapper().createObjectNode().put(key, value);
  }

  @TestConfiguration
  static class Config {
    @Bean
    DeliveryService deliveryService(
        EventRepositoryAdapter eventRepositoryAdapter,
        SubscriptionRepositoryAdapter subscriptionRepositoryAdapter,
        DeliveryRepositoryAdapter deliveryRepositoryAdapter,
        OutboxEventRepositoryAdapter outboxEventRepositoryAdapter,
        ObjectMapper objectMapper,
        Clock clock,
        NotificationMetrics notificationMetrics) {
      return new DeliveryService(
          eventRepositoryAdapter,
          subscriptionRepositoryAdapter,
          deliveryRepositoryAdapter,
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
    SubscriptionRepositoryAdapter subscriptionRepositoryAdapter(SubscriptionJpaRepository subscriptionJpaRepository) {
      return new SubscriptionRepositoryAdapter(subscriptionJpaRepository);
    }

    @Bean
    DeliveryRepositoryAdapter deliveryRepositoryAdapter(DeliveryJpaRepository deliveryJpaRepository) {
      return new DeliveryRepositoryAdapter(deliveryJpaRepository);
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
