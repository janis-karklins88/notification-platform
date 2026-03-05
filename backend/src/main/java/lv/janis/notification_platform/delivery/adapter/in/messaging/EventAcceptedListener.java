package lv.janis.notification_platform.delivery.adapter.in.messaging;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.config.OutboxMessagingConstants;
import lv.janis.notification_platform.delivery.application.port.in.DeliveryUseCase;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;

@Component
public class EventAcceptedListener {
  private static final Logger log = LoggerFactory.getLogger(EventAcceptedListener.class);

  private final DeliveryUseCase deliveryUseCase;

  public EventAcceptedListener(DeliveryUseCase deliveryUseCase) {
    this.deliveryUseCase = deliveryUseCase;
  }

  @RabbitListener(queues = OutboxMessagingConstants.QUEUE_ROUTING_EVENT_ACCEPTED)
  public void onEventAccepted(Message message) {
    String messageId = message.getMessageProperties().getMessageId();
    UUID eventId = extractEventId(message, messageId);
    try {
      deliveryUseCase.routeEvent(eventId);
    } catch (NotFoundException | IllegalArgumentException ex) {
      throw reject(messageId, "non-retryable event.accepted payload or event state", ex);
    }
  }

  private UUID extractEventId(Message message, String messageId) {
    var headers = message.getMessageProperties().getHeaders();
    var aggregateType = headers.get("aggregateType");
    var aggregateId = headers.get("aggregateId");

    if (!(aggregateType instanceof String)) {
      throw reject(messageId, "missing or invalid aggregateType header", new IllegalArgumentException("aggregateType: " + aggregateType));
    }

    if (!OutboxEventAggregateType.EVENT.name().equals(aggregateType)) {
      throw reject(messageId, "unexpected aggregateType header for event.accepted", new IllegalArgumentException("aggregateType: " + aggregateType));
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

  private AmqpRejectAndDontRequeueException reject(String messageId, String reason, Exception ex) {
    log.warn("Rejecting message id={} reason={}", messageId, reason, ex);
    return new AmqpRejectAndDontRequeueException(reason, ex);
  }
}
