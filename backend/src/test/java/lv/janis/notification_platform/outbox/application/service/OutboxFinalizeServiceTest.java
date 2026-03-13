package lv.janis.notification_platform.outbox.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.TextNode;

import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;
import org.junit.jupiter.api.Test;

import static lv.janis.notification_platform.support.EntityTestData.outboxEvent;
import static lv.janis.notification_platform.support.EntityTestData.tenant;

class OutboxFinalizeServiceTest {
  private final OutboxEventRepositoryPort repository = mock(OutboxEventRepositoryPort.class);
  private final OutboxFinalizeService service = new OutboxFinalizeService(repository);

  @Test
  void markPublishedUpdatesEventAndSaves() {
    UUID eventId = UUID.randomUUID();
    Instant publishedAt = Instant.parse("2026-02-03T11:00:00Z");
    OutboxEvent event = event(eventId);
    when(repository.findById(eventId)).thenReturn(Optional.of(event));

    service.markPublished(eventId, publishedAt);

    assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
    assertEquals(publishedAt, event.getPublishedAt());
    verify(repository).save(event);
  }

  @Test
  void rescheduleUpdatesEventAndSaves() {
    UUID eventId = UUID.randomUUID();
    Instant nextAttemptAt = Instant.parse("2026-02-03T11:05:00Z");
    OutboxEvent event = event(eventId);
    when(repository.findById(eventId)).thenReturn(Optional.of(event));

    service.reschedule(eventId, nextAttemptAt, " retry ");

    assertEquals(OutboxStatus.PENDING, event.getStatus());
    assertEquals(nextAttemptAt, event.getAvailableAt());
    assertEquals("retry", event.getLastError());
    verify(repository).save(event);
  }

  @Test
  void markFailedUpdatesEventAndSaves() {
    UUID eventId = UUID.randomUUID();
    Instant failedAt = Instant.parse("2026-02-03T11:10:00Z");
    OutboxEvent event = event(eventId);
    when(repository.findById(eventId)).thenReturn(Optional.of(event));

    service.markFailed(eventId, failedAt, " failed ");

    assertEquals(OutboxStatus.FAILED, event.getStatus());
    assertEquals(failedAt, event.getAvailableAt());
    assertEquals("failed", event.getLastError());
    verify(repository).save(event);
  }

  @Test
  void markPublishedThrowsWhenEventMissing() {
    UUID eventId = UUID.randomUUID();
    when(repository.findById(eventId)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(NotFoundException.class, () -> service.markPublished(eventId, Instant.now()));

    assertEquals("Outbox event not found: " + eventId, ex.getMessage());
  }

  private static OutboxEvent event(UUID eventId) {
    return outboxEvent(
        eventId,
        tenant(UUID.randomUUID()),
        OutboxEventAggregateType.EVENT,
        UUID.randomUUID(),
        OutboxEventType.EVENT_ACCEPTED,
        new TextNode("payload"),
        Instant.parse("2026-02-03T10:00:00Z"));
  }
}
