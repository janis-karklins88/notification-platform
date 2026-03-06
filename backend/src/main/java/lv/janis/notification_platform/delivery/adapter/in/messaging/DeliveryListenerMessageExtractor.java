package lv.janis.notification_platform.delivery.adapter.in.messaging;

import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;

import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;

public final class DeliveryListenerMessageExtractor {

  public static final String AGGREGATE_TYPE_HEADER = "aggregateType";
  public static final String AGGREGATE_ID_HEADER = "aggregateId";

  private DeliveryListenerMessageExtractor() {
  }

  public static UUID extractDeliveryId(Message message, String messageId) {
    Map<String, Object> headers = message.getMessageProperties().getHeaders();
    var aggregateType = headers.get(AGGREGATE_TYPE_HEADER);
    var aggregateId = headers.get(AGGREGATE_ID_HEADER);

    if (!(aggregateType instanceof String)) {
      throw reject(messageId, "missing or invalid aggregateType header",
          new IllegalArgumentException("aggregateType: " + aggregateType));
    }

    if (!OutboxEventAggregateType.DELIVERY.name().equals(aggregateType)) {
      throw reject(messageId, "unexpected aggregateType header for event.accepted",
          new IllegalArgumentException("aggregateType: " + aggregateType));
    }

    try {
      if (!(aggregateId instanceof String)) {
        throw new IllegalArgumentException("aggregateId header is missing or not a string");
      }
      return UUID.fromString((String) aggregateId);
    } catch (IllegalArgumentException ex) {
      throw reject(messageId, "invalid aggregateId header", ex);
    }
  }

  public static UUID extractEventId(Message message, String messageId) {
    Map<String, Object> headers = message.getMessageProperties().getHeaders();
    var aggregateType = headers.get(AGGREGATE_TYPE_HEADER);
    var aggregateId = headers.get(AGGREGATE_ID_HEADER);

    if (!(aggregateType instanceof String)) {
      throw reject(messageId, "missing or invalid aggregateType header", new IllegalArgumentException("aggregateType: " + aggregateType));
    }

    if (!OutboxEventAggregateType.EVENT.name().equals(aggregateType)) {
      throw reject(messageId, "unexpected aggregateType header", new IllegalArgumentException("aggregateType: " + aggregateType));
    }

    try {
      if (!(aggregateId instanceof String)) {
        throw new IllegalArgumentException("aggregateId header is missing or not a string");
      }
      return UUID.fromString((String) aggregateId);
    } catch (IllegalArgumentException ex) {
      throw reject(messageId, "invalid aggregateId header", ex);
    }
  }

  private static AmqpRejectAndDontRequeueException reject(String messageId, String reason, Exception ex) {
    return new AmqpRejectAndDontRequeueException(reason, ex);
  }
}
