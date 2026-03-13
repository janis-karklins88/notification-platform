package lv.janis.notification_platform.delivery.adapter.in.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;

class DeliveryListenerMessageExtractorTest {

  @Test
  void extractDeliveryIdReturnsAggregateIdWhenHeadersAreValid() {
    UUID deliveryId = UUID.randomUUID();
    Message message = message("msg-1", OutboxEventAggregateType.DELIVERY.name(), deliveryId.toString());

    UUID result = DeliveryListenerMessageExtractor.extractDeliveryId(message, "msg-1");

    assertEquals(deliveryId, result);
  }

  @Test
  void extractEventIdReturnsAggregateIdWhenHeadersAreValid() {
    UUID eventId = UUID.randomUUID();
    Message message = message("msg-2", OutboxEventAggregateType.EVENT.name(), eventId.toString());

    UUID result = DeliveryListenerMessageExtractor.extractEventId(message, "msg-2");

    assertEquals(eventId, result);
  }

  @Test
  void extractDeliveryIdRejectsUnexpectedAggregateType() {
    Message message = message("msg-3", OutboxEventAggregateType.EVENT.name(), UUID.randomUUID().toString());

    AmqpRejectAndDontRequeueException ex = assertThrows(
        AmqpRejectAndDontRequeueException.class,
        () -> DeliveryListenerMessageExtractor.extractDeliveryId(message, "msg-3"));

    assertEquals("unexpected aggregateType header for event.accepted", ex.getMessage());
  }

  @Test
  void extractEventIdRejectsInvalidAggregateId() {
    Message message = message("msg-4", OutboxEventAggregateType.EVENT.name(), "not-a-uuid");

    AmqpRejectAndDontRequeueException ex = assertThrows(
        AmqpRejectAndDontRequeueException.class,
        () -> DeliveryListenerMessageExtractor.extractEventId(message, "msg-4"));

    assertEquals("invalid aggregateId header", ex.getMessage());
  }

  private static Message message(String messageId, String aggregateType, String aggregateId) {
    MessageProperties properties = new MessageProperties();
    properties.setMessageId(messageId);
    properties.setHeader(DeliveryListenerMessageExtractor.AGGREGATE_TYPE_HEADER, aggregateType);
    properties.setHeader(DeliveryListenerMessageExtractor.AGGREGATE_ID_HEADER, aggregateId);
    return new Message(new byte[0], properties);
  }
}
