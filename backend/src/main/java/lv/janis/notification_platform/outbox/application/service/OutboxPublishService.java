package lv.janis.notification_platform.outbox.application.service;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import lv.janis.notification_platform.config.OutboxMessagingConstants;
import lv.janis.notification_platform.outbox.application.port.in.OutboxPublishUseCase;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;

@Service
public class OutboxPublishService implements OutboxPublishUseCase {

  private final RabbitTemplate rabbitTemplate;

  public OutboxPublishService(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  public void publish(OutboxEvent event) {

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
        });
  }
}
