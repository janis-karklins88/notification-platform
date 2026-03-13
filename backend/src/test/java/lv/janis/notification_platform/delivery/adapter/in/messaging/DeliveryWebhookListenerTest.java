package lv.janis.notification_platform.delivery.adapter.in.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.delivery.application.port.in.WebhookDeliveryUseCase;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;

class DeliveryWebhookListenerTest {
  private final WebhookDeliveryUseCase useCase = mock(WebhookDeliveryUseCase.class);
  private final DeliveryWebhookListener listener = new DeliveryWebhookListener(useCase);

  @Test
  void onDeliveryCreatedWebhookDelegatesWhenMessageIsValid() {
    UUID deliveryId = UUID.randomUUID();

    listener.onDeliveryCreatedWebhook(message("msg-1", deliveryId));

    verify(useCase).deliverWebhook(deliveryId);
  }

  @Test
  void onDeliveryCreatedWebhookRejectsForNonRetryableException() {
    UUID deliveryId = UUID.randomUUID();
    doThrow(new BadRequestException("bad")).when(useCase).deliverWebhook(deliveryId);

    AmqpRejectAndDontRequeueException ex = assertThrows(
        AmqpRejectAndDontRequeueException.class,
        () -> listener.onDeliveryCreatedWebhook(message("msg-2", deliveryId)));

    assertEquals("non-retryable event.accepted payload or delivery state", ex.getMessage());
  }

  @Test
  void onDeliveryCreatedWebhookRethrowsRetryableException() {
    UUID deliveryId = UUID.randomUUID();
    RuntimeException failure = new RuntimeException("temporary");
    doThrow(failure).when(useCase).deliverWebhook(deliveryId);

    RuntimeException ex = assertThrows(
        RuntimeException.class,
        () -> listener.onDeliveryCreatedWebhook(message("msg-3", deliveryId)));

    assertEquals(failure, ex);
  }

  private static Message message(String messageId, UUID deliveryId) {
    MessageProperties properties = new MessageProperties();
    properties.setMessageId(messageId);
    properties.setHeader(DeliveryListenerMessageExtractor.AGGREGATE_TYPE_HEADER, OutboxEventAggregateType.DELIVERY.name());
    properties.setHeader(DeliveryListenerMessageExtractor.AGGREGATE_ID_HEADER, deliveryId.toString());
    return new Message(new byte[0], properties);
  }
}
