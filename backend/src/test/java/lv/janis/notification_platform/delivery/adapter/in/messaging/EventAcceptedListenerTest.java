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
import lv.janis.notification_platform.delivery.application.port.in.DeliveryUseCase;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;

class EventAcceptedListenerTest {
  private final DeliveryUseCase deliveryUseCase = mock(DeliveryUseCase.class);
  private final EventAcceptedListener listener = new EventAcceptedListener(deliveryUseCase);

  @Test
  void onEventAcceptedRoutesEventWhenMessageIsValid() {
    UUID eventId = UUID.randomUUID();

    listener.onEventAccepted(message("msg-1", OutboxEventAggregateType.EVENT.name(), eventId.toString()));

    verify(deliveryUseCase).routeEvent(eventId);
  }

  @Test
  void onEventAcceptedRejectsForNonRetryableException() {
    UUID eventId = UUID.randomUUID();
    doThrow(new BadRequestException("bad")).when(deliveryUseCase).routeEvent(eventId);

    AmqpRejectAndDontRequeueException ex = assertThrows(
        AmqpRejectAndDontRequeueException.class,
        () -> listener.onEventAccepted(message("msg-2", OutboxEventAggregateType.EVENT.name(), eventId.toString())));

    assertEquals("non-retryable event.accepted payload or event state", ex.getMessage());
  }

  @Test
  void onEventAcceptedRethrowsRetryableException() {
    UUID eventId = UUID.randomUUID();
    RuntimeException failure = new RuntimeException("temporary");
    doThrow(failure).when(deliveryUseCase).routeEvent(eventId);

    RuntimeException ex = assertThrows(
        RuntimeException.class,
        () -> listener.onEventAccepted(message("msg-3", OutboxEventAggregateType.EVENT.name(), eventId.toString())));

    assertEquals(failure, ex);
  }

  private static Message message(String messageId, String aggregateType, String aggregateId) {
    MessageProperties properties = new MessageProperties();
    properties.setMessageId(messageId);
    properties.setHeader(DeliveryListenerMessageExtractor.AGGREGATE_TYPE_HEADER, aggregateType);
    properties.setHeader(DeliveryListenerMessageExtractor.AGGREGATE_ID_HEADER, aggregateId);
    return new Message(new byte[0], properties);
  }
}
