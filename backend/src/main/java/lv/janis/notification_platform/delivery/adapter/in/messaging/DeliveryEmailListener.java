package lv.janis.notification_platform.delivery.adapter.in.messaging;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import lv.janis.notification_platform.config.OutboxMessagingConstants;
import lv.janis.notification_platform.delivery.application.port.in.EmailDeliveryUseCase;

@Component
public class DeliveryEmailListener {
  private static final Logger log = LoggerFactory.getLogger(DeliveryEmailListener.class);

  public final EmailDeliveryUseCase emailDeliveryUseCase;

  public DeliveryEmailListener(EmailDeliveryUseCase emailDeliveryUseCase) {
    this.emailDeliveryUseCase = emailDeliveryUseCase;
  }

  @RabbitListener(queues = OutboxMessagingConstants.QUEUE_DELIVERY_CREATED_EMAIL)
  public void onDeliveryCreatedEmail(Message message) {
    String messageId = message.getMessageProperties().getMessageId();
    UUID deliveryId = DeliveryListenerMessageExtractor.extractDeliveryId(message, messageId);
    try {
      emailDeliveryUseCase.deliverEmail(deliveryId);
    } catch (Exception ex) {
      if (DeliveryListenerFailurePolicy.isNonRetryable(ex)) {
        throw reject(messageId, "non-retryable event.accepted payload or delivery state", ex);
      }
      throw ex;
    }
  }

  private AmqpRejectAndDontRequeueException reject(String messageId, String reason, Exception ex) {
    log.warn("Rejecting message id={} reason={}", messageId, reason, ex);
    return new AmqpRejectAndDontRequeueException(reason, ex);
  }
}
