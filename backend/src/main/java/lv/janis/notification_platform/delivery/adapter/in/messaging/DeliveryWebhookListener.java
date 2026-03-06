package lv.janis.notification_platform.delivery.adapter.in.messaging;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.amqp.core.Message;

import lv.janis.notification_platform.config.OutboxMessagingConstants;
import lv.janis.notification_platform.delivery.application.port.in.WebhookDeliveryUseCase;

@Component
public class DeliveryWebhookListener {
  private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryUseCase.class);

  public final WebhookDeliveryUseCase webhookDeliveryUseCase;

  public DeliveryWebhookListener(WebhookDeliveryUseCase webhookDeliveryUseCase) {
    this.webhookDeliveryUseCase = webhookDeliveryUseCase;
  }

  @RabbitListener(queues = OutboxMessagingConstants.QUEUE_DELIVERY_CREATED_WEBHOOK)
  public void onDeliveryCreatedWebhook(Message message) {
    String messageId = message.getMessageProperties().getMessageId();
    UUID deliveryId = DeliveryListenerMessageExtractor.extractDeliveryId(message, messageId);
    try {
      webhookDeliveryUseCase.deliverWebhook(deliveryId);
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
