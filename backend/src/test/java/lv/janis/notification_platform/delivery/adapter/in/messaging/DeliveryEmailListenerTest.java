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
import lv.janis.notification_platform.delivery.application.port.in.EmailDeliveryUseCase;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;

class DeliveryEmailListenerTest {
  private final EmailDeliveryUseCase useCase = mock(EmailDeliveryUseCase.class);
  private final DeliveryEmailListener listener = new DeliveryEmailListener(useCase);

  @Test
  void onDeliveryCreatedEmailDelegatesWhenMessageIsValid() {
    UUID deliveryId = UUID.randomUUID();

    listener.onDeliveryCreatedEmail(message("msg-1", deliveryId));

    verify(useCase).deliverEmail(deliveryId);
  }

  @Test
  void onDeliveryCreatedEmailRejectsForNonRetryableException() {
    UUID deliveryId = UUID.randomUUID();
    doThrow(new BadRequestException("bad")).when(useCase).deliverEmail(deliveryId);

    AmqpRejectAndDontRequeueException ex = assertThrows(
        AmqpRejectAndDontRequeueException.class,
        () -> listener.onDeliveryCreatedEmail(message("msg-2", deliveryId)));

    assertEquals("non-retryable event.accepted payload or delivery state", ex.getMessage());
  }

  @Test
  void onDeliveryCreatedEmailRethrowsRetryableException() {
    UUID deliveryId = UUID.randomUUID();
    RuntimeException failure = new RuntimeException("temporary");
    doThrow(failure).when(useCase).deliverEmail(deliveryId);

    RuntimeException ex = assertThrows(
        RuntimeException.class,
        () -> listener.onDeliveryCreatedEmail(message("msg-3", deliveryId)));

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
