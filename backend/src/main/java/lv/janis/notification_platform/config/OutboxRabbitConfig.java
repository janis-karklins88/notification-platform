package lv.janis.notification_platform.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
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
  Queue routingEventAcceptedQueue() {
    return new Queue(OutboxMessagingConstants.QUEUE_ROUTING_EVENT_ACCEPTED, true);
  }

  @Bean
  Queue deliveryCreatedWebhookQueue() {
    return new Queue(OutboxMessagingConstants.QUEUE_DELIVERY_CREATED_WEBHOOK, true);
  }

  @Bean
  Queue deliveryCreatedEmailQueue() {
    return new Queue(OutboxMessagingConstants.QUEUE_DELIVERY_CREATED_EMAIL, true);
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
  MessageConverter rabbitMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}