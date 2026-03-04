package lv.janis.notification_platform.delivery.adapter.in.messaging;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.config.OutboxMessagingConstants;
import lv.janis.notification_platform.delivery.application.port.in.DeliveryUseCase;

@Component
public class EventAcceptedListener {
  private static final Logger log = LoggerFactory.getLogger(EventAcceptedListener.class);

  private final DeliveryUseCase deliveryUseCase;
  private final ObjectMapper objectMapper;

  public EventAcceptedListener(DeliveryUseCase deliveryUseCase, ObjectMapper objectMapper) {
    this.deliveryUseCase = deliveryUseCase;
    this.objectMapper = objectMapper;
  }

  @RabbitListener(queues = OutboxMessagingConstants.QUEUE_ROUTING_EVENT_ACCEPTED)
  public void onEventAccepted(Message message) {
    String messageId = message.getMessageProperties().getMessageId();
    UUID eventId = extractEventId(message.getBody(), messageId);
    try {
      deliveryUseCase.routeEvent(eventId);
    } catch (NotFoundException | IllegalArgumentException ex) {
      throw reject(messageId, "non-retryable event.accepted payload or event state", ex);
    }
  }

  private UUID extractEventId(byte[] body, String messageId) {
    try {
      JsonNode payload = objectMapper.readTree(body);
      JsonNode eventIdNode = payload.get("eventId");
      if (eventIdNode == null || !eventIdNode.isTextual()) {
        throw new IllegalArgumentException("eventId is missing or invalid");
      }
      return UUID.fromString(eventIdNode.asText());
    } catch (IOException | IllegalArgumentException ex) {
      throw reject(messageId, "invalid event.accepted payload", ex);
    }
  }

  private AmqpRejectAndDontRequeueException reject(String messageId, String reason, Exception ex) {
    log.warn("Rejecting message id={} reason={}", messageId, reason, ex);
    return new AmqpRejectAndDontRequeueException(reason, ex);
  }
}

