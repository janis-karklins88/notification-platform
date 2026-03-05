package lv.janis.notification_platform.config;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboxRabbitConfig {

  @Bean
  TopicExchange outboxEventsExchange() {
    return new TopicExchange(OutboxMessagingConstants.EXCHANGE_OUTBOX_EVENTS, true, false);
  }

  @Bean
  DirectExchange outboxEventsDlqExchange() {
    return new DirectExchange(OutboxMessagingConstants.EXCHANGE_OUTBOX_EVENTS_DLQ, true, false);
  }

  @Bean
  Queue routingEventAcceptedQueue() {
    return new Queue(OutboxMessagingConstants.QUEUE_ROUTING_EVENT_ACCEPTED, true, false, false,
        Map.of(
            "x-dead-letter-exchange", OutboxMessagingConstants.EXCHANGE_OUTBOX_EVENTS_DLQ,
            "x-dead-letter-routing-key", OutboxMessagingConstants.RK_OUTBOX_DLQ));
  }

  @Bean
  Queue deliveryCreatedWebhookQueue() {
    return new Queue(OutboxMessagingConstants.QUEUE_DELIVERY_CREATED_WEBHOOK, true, false, false,
        Map.of(
            "x-dead-letter-exchange", OutboxMessagingConstants.EXCHANGE_OUTBOX_EVENTS_DLQ,
            "x-dead-letter-routing-key", OutboxMessagingConstants.RK_OUTBOX_DLQ));
  }

  @Bean
  Queue deliveryCreatedEmailQueue() {
    return new Queue(OutboxMessagingConstants.QUEUE_DELIVERY_CREATED_EMAIL, true, false, false,
        Map.of(
            "x-dead-letter-exchange", OutboxMessagingConstants.EXCHANGE_OUTBOX_EVENTS_DLQ,
            "x-dead-letter-routing-key", OutboxMessagingConstants.RK_OUTBOX_DLQ));
  }

  @Bean
  Queue outboxDlqQueue() {
    return new Queue(OutboxMessagingConstants.QUEUE_OUTBOX_DLQ, true);
  }

  @Bean
  Binding routingEventAcceptedBinding(Queue routingEventAcceptedQueue, TopicExchange outboxEventsExchange) {
    return BindingBuilder.bind(routingEventAcceptedQueue)
        .to(outboxEventsExchange)
        .with(OutboxMessagingConstants.RK_EVENT_ACCEPTED);
  }

  @Bean
  Binding deliveryCreatedWebhookBinding(Queue deliveryCreatedWebhookQueue, TopicExchange outboxEventsExchange) {
    return BindingBuilder.bind(deliveryCreatedWebhookQueue)
        .to(outboxEventsExchange)
        .with(OutboxMessagingConstants.RK_DELIVERY_CREATED_WEBHOOK);
  }

  @Bean
  Binding deliveryCreatedEmailBinding(Queue deliveryCreatedEmailQueue, TopicExchange outboxEventsExchange) {
    return BindingBuilder.bind(deliveryCreatedEmailQueue)
        .to(outboxEventsExchange)
        .with(OutboxMessagingConstants.RK_DELIVERY_CREATED_EMAIL);
  }

  @Bean
  Binding outboxDlqBinding(Queue outboxDlqQueue, DirectExchange outboxEventsDlqExchange) {
    return BindingBuilder.bind(outboxDlqQueue)
        .to(outboxEventsDlqExchange)
        .with(OutboxMessagingConstants.RK_OUTBOX_DLQ);
  }

  @Bean
  MessageConverter rabbitMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
