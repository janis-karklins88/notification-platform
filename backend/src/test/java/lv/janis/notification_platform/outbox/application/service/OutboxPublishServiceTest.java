package lv.janis.notification_platform.outbox.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import lv.janis.notification_platform.config.OutboxMessagingConstants;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import org.junit.jupiter.api.Test;

import static lv.janis.notification_platform.support.EntityTestData.outboxEvent;
import static lv.janis.notification_platform.support.EntityTestData.tenant;

class OutboxPublishServiceTest {
  private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
  private final OutboxPublishService service = new OutboxPublishService(rabbitTemplate);

  @Test
  void publishSetsMessageHeadersAndWaitsForAck() {
    UUID tenantId = UUID.randomUUID();
    UUID aggregateId = UUID.randomUUID();
    OutboxEvent event = outboxEvent(
        UUID.randomUUID(),
        tenant(tenantId),
        OutboxEventAggregateType.EVENT,
        aggregateId,
        OutboxEventType.EVENT_ACCEPTED,
        JsonNodeFactory.instance.objectNode().put("eventId", aggregateId.toString()),
        Instant.parse("2026-02-04T10:00:00Z"));
    MessageProperties[] capturedProperties = new MessageProperties[1];

    doAnswer(invocation -> {
      MessagePostProcessor postProcessor = invocation.getArgument(3);
      CorrelationData correlationData = invocation.getArgument(4);
      Message message = postProcessor
          .postProcessMessage(new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties()));
      capturedProperties[0] = message.getMessageProperties();
      correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
      return null;
    }).when(rabbitTemplate).convertAndSend(
        eq(OutboxMessagingConstants.EXCHANGE_OUTBOX_EVENTS),
        eq(event.getEventType().routingKey()),
        eq(event.getPayload()),
        any(MessagePostProcessor.class),
        any(CorrelationData.class));

    service.publish(event);

    assertEquals(event.getId().toString(), capturedProperties[0].getMessageId());
    assertEquals("application/json", capturedProperties[0].getContentType());
    assertEquals(MessageDeliveryMode.PERSISTENT, capturedProperties[0].getDeliveryMode());
    assertEquals(event.getTenantId().toString(), capturedProperties[0].getHeaders().get("tenantId"));
    assertEquals(event.getAggregateType().name(), capturedProperties[0].getHeaders().get("aggregateType"));
    assertEquals(event.getAggregateId().toString(), capturedProperties[0].getHeaders().get("aggregateId"));
    assertEquals(event.getEventType().name(), capturedProperties[0].getHeaders().get("eventType"));
  }

  @Test
  void publishThrowsWhenBrokerDoesNotAck() {
    OutboxEvent event = outboxEvent(
        UUID.randomUUID(),
        tenant(UUID.randomUUID()),
        OutboxEventAggregateType.EVENT,
        UUID.randomUUID(),
        OutboxEventType.EVENT_ACCEPTED,
        JsonNodeFactory.instance.objectNode(),
        Instant.parse("2026-02-04T10:00:00Z"));

    doAnswer(invocation -> {
      CorrelationData correlationData = invocation.getArgument(4);
      correlationData.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
      return null;
    }).when(rabbitTemplate).convertAndSend(
        eq(OutboxMessagingConstants.EXCHANGE_OUTBOX_EVENTS),
        eq(event.getEventType().routingKey()),
        eq(event.getPayload()),
        any(MessagePostProcessor.class),
        any(CorrelationData.class));

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.publish(event));

    assertEquals("Broker did not ack outbox event " + event.getId() + ": nack", ex.getMessage());
  }
}
