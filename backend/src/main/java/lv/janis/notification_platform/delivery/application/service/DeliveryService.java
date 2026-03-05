package lv.janis.notification_platform.delivery.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.delivery.application.port.in.DeliveryUseCase;
import lv.janis.notification_platform.delivery.application.port.out.DeliveryRepositoryPort;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.ingest.application.port.out.EventRepositoryPort;
import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.ingest.domain.EventStatus;
import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.routing.application.port.out.SubscriptionRepositoryPort;
import lv.janis.notification_platform.routing.domain.Subscription;

@Service
public class DeliveryService implements DeliveryUseCase {

  private final EventRepositoryPort eventRepositoryPort;
  private final SubscriptionRepositoryPort subscriptionRepositoryPort;
  private final DeliveryRepositoryPort deliveryRepositoryPort;
  private final OutboxEventRepositoryPort outboxRepositoryPort;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public DeliveryService(EventRepositoryPort eventRepositoryPort,
      SubscriptionRepositoryPort subscriptionRepositoryPort, DeliveryRepositoryPort deliveryRepositoryPort,
      OutboxEventRepositoryPort outboxRepositoryPort, ObjectMapper objectMapper, Clock clock) {
    this.eventRepositoryPort = eventRepositoryPort;
    this.subscriptionRepositoryPort = subscriptionRepositoryPort;
    this.deliveryRepositoryPort = deliveryRepositoryPort;
    this.outboxRepositoryPort = outboxRepositoryPort;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void routeEvent(UUID eventId) {
    Event event = eventRepositoryPort.findById(eventId)
        .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found"));

    if (event.getStatus() == EventStatus.ROUTED) {
      return;
    }

    List<Subscription> subscriptions = subscriptionRepositoryPort.findActiveByTenantIdAndEventType(event.getTenantId(),
        event.getEventType());

    if (subscriptions.isEmpty()) {
      event.markRouted();
      eventRepositoryPort.save(event);
      return;
    }

    Instant now = Instant.now(clock);

    for (var sub : subscriptions) {
      boolean alreadyCreated = deliveryRepositoryPort
          .findByTenantIdAndEventIdAndSubscriptionId(event.getTenantId(), event.getId(), sub.getId())
          .isPresent();
      if (alreadyCreated) {
        continue;
      }

      var savedDelivery = deliveryRepositoryPort.save(createDelivery(sub, event, now));
      var outboxEvent = createOutboxEvent(savedDelivery, now);
      outboxRepositoryPort.save(outboxEvent);
    }

    event.markRouted();
    eventRepositoryPort.save(event);

  }

  private Delivery createDelivery(Subscription sub, Event event, Instant timestamp) {
    var tenant = event.getTenant();
    var endpoint = sub.getEndpoint();
    return new Delivery(tenant, event, sub, endpoint, timestamp);
  }

  private OutboxEvent createOutboxEvent(Delivery delivery, Instant timestamp) {
    JsonNode payload = objectMapper.createObjectNode()
        .put("deliveryId", delivery.getId().toString())
        .put("eventId", delivery.getEventId().toString());
    var outboxEventType = switch (delivery.getEndpoint().getType()) {
      case EMAIL -> OutboxEventType.DELIVERY_CREATED_EMAIL;
      case WEBHOOK -> OutboxEventType.DELIVERY_CREATED_WEBHOOK;
      case SMS, PUSH_NOTIFICATION -> throw new IllegalArgumentException(
          "No outbox routing configured for endpoint type: " + delivery.getEndpoint().getType());
    };
    return new OutboxEvent(
        delivery.getTenant(),
        OutboxEventAggregateType.DELIVERY,
        delivery.getId(),
        outboxEventType,
        payload,
        timestamp);

  }

}
