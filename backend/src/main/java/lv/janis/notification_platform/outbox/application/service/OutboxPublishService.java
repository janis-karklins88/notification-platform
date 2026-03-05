package lv.janis.notification_platform.outbox.application.service;

import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.stereotype.Service;

import lv.janis.notification_platform.config.OutboxMessagingConstants;
import lv.janis.notification_platform.outbox.application.port.in.OutboxPublishUseCase;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;

@Service
public class OutboxPublishService implements OutboxPublishUseCase {
  private static final long CONFIRM_TIMEOUT_MS = 5_000L;

  private final RabbitTemplate rabbitTemplate;

  public OutboxPublishService(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;

  }

  @Override
  public void publish(OutboxEvent event) {
    CorrelationData correlationData = new CorrelationData(event.getId().toString());

    rabbitTemplate.convertAndSend(
        OutboxMessagingConstants.EXCHANGE_OUTBOX_EVENTS,
        event.getEventType().routingKey(),
        event.getPayload(),
        message -> {
          var props = message.getMessageProperties();
          props.setMessageId(event.getId().toString());
          props.setContentType("application/json");
          props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
          props.setHeader("tenantId", event.getTenantId().toString());
          props.setHeader("aggregateType", event.getAggregateType().name());
          props.setHeader("aggregateId", event.getAggregateId().toString());
          props.setHeader("eventType", event.getEventType().name());
          return message;
        },
        correlationData);

    waitForBrokerConfirm(event, correlationData);
    ensureNotReturned(event, correlationData.getReturned());
  }

  private void waitForBrokerConfirm(OutboxEvent event, CorrelationData correlationData) {
    try {
      CorrelationData.Confirm confirm = correlationData.getFuture().get(CONFIRM_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      if (confirm == null || !confirm.isAck()) {
        String reason = confirm == null ? "no confirm received" : confirm.getReason();
        throw new IllegalStateException("Broker did not ack outbox event " + event.getId() + ": " + reason);
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting broker confirm for outbox event " + event.getId(), ex);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed waiting broker confirm for outbox event " + event.getId(), ex);
    }
  }

  private void ensureNotReturned(OutboxEvent event, ReturnedMessage returnedMessage) {
    if (returnedMessage == null) {
      return;
    }
    throw new IllegalStateException(
        "Outbox event " + event.getId() + " was returned by broker, replyCode=" + returnedMessage.getReplyCode()
            + ", replyText=" + returnedMessage.getReplyText() + ", routingKey=" + returnedMessage.getRoutingKey());
  }

}
