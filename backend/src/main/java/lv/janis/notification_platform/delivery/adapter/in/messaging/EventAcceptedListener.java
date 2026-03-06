package lv.janis.notification_platform.delivery.adapter.in.messaging;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import lv.janis.notification_platform.config.OutboxMessagingConstants;
import lv.janis.notification_platform.delivery.application.port.in.DeliveryUseCase;

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
    UUID eventId = DeliveryListenerMessageExtractor.extractEventId(message, messageId);
    try {
      deliveryUseCase.routeEvent(eventId);
    } catch (Exception ex) {
      if (DeliveryListenerFailurePolicy.isNonRetryable(ex)) {
        throw reject(messageId, "non-retryable event.accepted payload or event state", ex);
      }
      throw ex;
    }
  }

  private AmqpRejectAndDontRequeueException reject(String messageId, String reason, Exception ex) {
    log.warn("Rejecting message id={} reason={}", messageId, reason, ex);
    return new AmqpRejectAndDontRequeueException(reason, ex);
  }
}
