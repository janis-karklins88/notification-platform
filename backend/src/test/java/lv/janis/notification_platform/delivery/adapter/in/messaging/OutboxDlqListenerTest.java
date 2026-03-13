package lv.janis.notification_platform.delivery.adapter.in.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import lv.janis.notification_platform.delivery.application.port.out.DeliveryRepositoryPort;
import lv.janis.notification_platform.delivery.application.service.DeliveryProcessingService;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;

import static lv.janis.notification_platform.support.EntityTestData.delivery;
import static lv.janis.notification_platform.support.EntityTestData.endpoint;
import static lv.janis.notification_platform.support.EntityTestData.event;
import static lv.janis.notification_platform.support.EntityTestData.subscription;
import static lv.janis.notification_platform.support.EntityTestData.tenant;

class OutboxDlqListenerTest {
  private static final Instant NOW = Instant.parse("2026-02-06T10:00:00Z");

  private final DeliveryRepositoryPort repository = mock(DeliveryRepositoryPort.class);
  private final DeliveryProcessingService processingService = new DeliveryProcessingService();
  private final OutboxDlqListener listener = new OutboxDlqListener(repository, processingService, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void onOutboxDlqIgnoresNonDeliveryAggregateType() {
    listener.onOutboxDlq(message("msg-1", OutboxEventAggregateType.EVENT.name(), UUID.randomUUID().toString(), Map.of()));

    verify(repository, never()).findById(any());
  }

  @Test
  void onOutboxDlqIgnoresInvalidDeliveryId() {
    listener.onOutboxDlq(message("msg-2", OutboxEventAggregateType.DELIVERY.name(), "invalid", Map.of()));

    verify(repository, never()).findById(any());
  }

  @Test
  void onOutboxDlqMarksNonTerminalDeliveryFailedAndSaves() {
    Delivery delivery = deliveryTree();
    when(repository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
    Map<String, Object> headers = Map.of(
        "x-exception-message", "delivery failed",
        "x-death", List.of(Map.of("count", 5L)));

    listener.onOutboxDlq(message("msg-3", OutboxEventAggregateType.DELIVERY.name(), delivery.getId().toString(), headers));

    assertEquals(lv.janis.notification_platform.delivery.domain.DeliveryStatus.FAILED, delivery.getStatus());
    assertEquals(NOW, delivery.getFailedAt());
    assertEquals("DLQ for outbox.dlq, retryCount=5, cause=delivery failed", delivery.getLastError());
    verify(repository).save(delivery);
  }

  @Test
  void onOutboxDlqSkipsAlreadyTerminalDelivery() {
    Delivery delivery = deliveryTree();
    delivery.markDelivered(NOW.minusSeconds(5));
    when(repository.findById(delivery.getId())).thenReturn(Optional.of(delivery));

    listener.onOutboxDlq(message("msg-4", OutboxEventAggregateType.DELIVERY.name(), delivery.getId().toString(), Map.of()));

    verify(repository, never()).save(any());
  }

  @Test
  void onOutboxDlqDoesNothingForMissingDelivery() {
    UUID deliveryId = UUID.randomUUID();
    when(repository.findById(deliveryId)).thenReturn(Optional.empty());

    listener.onOutboxDlq(message("msg-5", OutboxEventAggregateType.DELIVERY.name(), deliveryId.toString(), Map.of()));

    verify(repository, never()).save(any());
  }

  private static Message message(String messageId, String aggregateType, String aggregateId, Map<String, Object> headers) {
    MessageProperties properties = new MessageProperties();
    properties.setMessageId(messageId);
    properties.setConsumerQueue("outbox.dlq");
    properties.setHeader(DeliveryListenerMessageExtractor.AGGREGATE_TYPE_HEADER, aggregateType);
    properties.setHeader(DeliveryListenerMessageExtractor.AGGREGATE_ID_HEADER, aggregateId);
    headers.forEach(properties::setHeader);
    return new Message(new byte[0], properties);
  }

  private static Delivery deliveryTree() {
    var tenant = tenant(UUID.randomUUID());
    var endpoint = endpoint(UUID.randomUUID(), tenant, EndpointType.WEBHOOK);
    var event = event(UUID.randomUUID(), tenant, "order.created");
    var subscription = subscription(UUID.randomUUID(), tenant, "order.created", endpoint);
    return delivery(UUID.randomUUID(), tenant, event, subscription, endpoint);
  }
}
