package lv.janis.notification_platform.delivery.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.delivery.application.port.out.DeliveryRepositoryPort;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.ingest.application.port.out.EventRepositoryPort;
import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.ingest.domain.EventStatus;
import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.routing.application.port.out.SubscriptionRepositoryPort;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.shared.metrics.NotificationMetrics;
import lv.janis.notification_platform.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static lv.janis.notification_platform.support.EntityTestData.delivery;
import static lv.janis.notification_platform.support.EntityTestData.endpoint;
import static lv.janis.notification_platform.support.EntityTestData.event;
import static lv.janis.notification_platform.support.EntityTestData.subscription;
import static lv.janis.notification_platform.support.EntityTestData.tenant;

class DeliveryServiceTest {
  private static final Instant NOW = Instant.parse("2026-02-01T15:00:00Z");

  private final EventRepositoryPort eventRepository = mock(EventRepositoryPort.class);
  private final SubscriptionRepositoryPort subscriptionRepository = mock(SubscriptionRepositoryPort.class);
  private final DeliveryRepositoryPort deliveryRepository = mock(DeliveryRepositoryPort.class);
  private final OutboxEventRepositoryPort outboxRepository = mock(OutboxEventRepositoryPort.class);
  private final NotificationMetrics notificationMetrics = mock(NotificationMetrics.class);
  private final DeliveryService service = new DeliveryService(
      eventRepository,
      subscriptionRepository,
      deliveryRepository,
      outboxRepository,
      new ObjectMapper(),
      Clock.fixed(NOW, ZoneOffset.UTC),
      notificationMetrics);

  @Test
  void routeEventThrowsNotFoundWhenEventMissing() {
    UUID eventId = UUID.randomUUID();
    when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(NotFoundException.class, () -> service.routeEvent(eventId));

    assertEquals("Event with id " + eventId + " not found", ex.getMessage());
  }

  @Test
  void routeEventReturnsImmediatelyWhenEventAlreadyRouted() {
    Event event = event(UUID.randomUUID(), tenant(UUID.randomUUID()), "order.created", JsonNodeFactory.instance.objectNode(), EventStatus.ROUTED);
    when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

    service.routeEvent(event.getId());

    verify(subscriptionRepository, never()).findActiveByTenantIdAndEventType(any(), any());
    verify(eventRepository, never()).save(any());
  }

  @Test
  void routeEventMarksEventRoutedWhenNoSubscriptionsExist() {
    Event event = event(UUID.randomUUID(), tenant(UUID.randomUUID()), "order.created");
    when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
    when(subscriptionRepository.findActiveByTenantIdAndEventType(event.getTenantId(), event.getEventType())).thenReturn(List.of());

    service.routeEvent(event.getId());

    assertEquals(EventStatus.ROUTED, event.getStatus());
    verify(eventRepository).save(event);
  }

  @Test
  void routeEventCreatesDeliveriesAndOutboxEventsForNewSubscriptions() {
    Tenant tenant = tenant(UUID.randomUUID());
    Event event = event(UUID.randomUUID(), tenant, "order.created");
    Endpoint emailEndpoint = endpoint(UUID.randomUUID(), tenant, EndpointType.EMAIL);
    Endpoint webhookEndpoint = endpoint(UUID.randomUUID(), tenant, EndpointType.WEBHOOK);
    Subscription emailSubscription = subscription(UUID.randomUUID(), tenant, "order.created", emailEndpoint);
    Subscription webhookSubscription = subscription(UUID.randomUUID(), tenant, "order.created", webhookEndpoint);

    when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
    when(subscriptionRepository.findActiveByTenantIdAndEventType(event.getTenantId(), event.getEventType()))
        .thenReturn(List.of(emailSubscription, webhookSubscription));
    when(deliveryRepository.findByTenantIdAndEventIdAndSubscriptionId(event.getTenantId(), event.getId(), emailSubscription.getId()))
        .thenReturn(Optional.empty());
    when(deliveryRepository.findByTenantIdAndEventIdAndSubscriptionId(event.getTenantId(), event.getId(), webhookSubscription.getId()))
        .thenReturn(Optional.empty());
    AtomicInteger sequence = new AtomicInteger();
    when(deliveryRepository.save(any())).thenAnswer(invocation -> {
      Delivery delivery = invocation.getArgument(0);
      ReflectionTestUtils.setField(delivery, "id", UUID.fromString(String.format("00000000-0000-0000-0000-%012d", sequence.incrementAndGet())));
      ReflectionTestUtils.setField(delivery, "tenantId", tenant.getId());
      ReflectionTestUtils.setField(delivery, "eventId", event.getId());
      ReflectionTestUtils.setField(delivery, "subscriptionId", delivery.getSubscription().getId());
      ReflectionTestUtils.setField(delivery, "endpointId", delivery.getEndpoint().getId());
      return delivery;
    });
    when(outboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.routeEvent(event.getId());

    assertEquals(EventStatus.ROUTED, event.getStatus());
    verify(notificationMetrics).incrementDeliveriesCreated(2);
    verify(eventRepository).save(event);

    ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxRepository, org.mockito.Mockito.times(2)).save(outboxCaptor.capture());
    assertEquals(OutboxEventType.DELIVERY_CREATED_EMAIL, outboxCaptor.getAllValues().get(0).getEventType());
    assertEquals(OutboxEventType.DELIVERY_CREATED_WEBHOOK, outboxCaptor.getAllValues().get(1).getEventType());
    assertEquals(event.getId().toString(), outboxCaptor.getAllValues().get(0).getPayload().get("eventId").asText());
    assertEquals(event.getId().toString(), outboxCaptor.getAllValues().get(1).getPayload().get("eventId").asText());
  }

  @Test
  void routeEventSkipsSubscriptionWhenDeliveryAlreadyExists() {
    Tenant tenant = tenant(UUID.randomUUID());
    Event event = event(UUID.randomUUID(), tenant, "order.created");
    Endpoint endpoint = endpoint(UUID.randomUUID(), tenant, EndpointType.WEBHOOK);
    Subscription subscription = subscription(UUID.randomUUID(), tenant, "order.created", endpoint);
    Delivery existing = delivery(UUID.randomUUID(), tenant, event, subscription, endpoint);

    when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
    when(subscriptionRepository.findActiveByTenantIdAndEventType(event.getTenantId(), event.getEventType())).thenReturn(List.of(subscription));
    when(deliveryRepository.findByTenantIdAndEventIdAndSubscriptionId(event.getTenantId(), event.getId(), subscription.getId()))
        .thenReturn(Optional.of(existing));

    service.routeEvent(event.getId());

    assertEquals(EventStatus.ROUTED, event.getStatus());
    verify(deliveryRepository, never()).save(any());
    verify(outboxRepository, never()).save(any());
    verify(notificationMetrics).incrementDeliveriesCreated(0);
  }
}
