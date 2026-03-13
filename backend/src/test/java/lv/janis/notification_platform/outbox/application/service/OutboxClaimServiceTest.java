package lv.janis.notification_platform.outbox.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.TextNode;

import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;
import lv.janis.notification_platform.shared.metrics.NotificationMetrics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static lv.janis.notification_platform.support.EntityTestData.outboxEvent;
import static lv.janis.notification_platform.support.EntityTestData.tenant;

class OutboxClaimServiceTest {
  private final OutboxEventRepositoryPort repository = mock(OutboxEventRepositoryPort.class);
  private final NotificationMetrics notificationMetrics = mock(NotificationMetrics.class);
  private final OutboxClaimService service = new OutboxClaimService(
      repository,
      new OutboxDispatchProperties(100, true, 1000L, 300000L, 5, 3, 100L),
      notificationMetrics);

  @Test
  void claimMarksEventsInProgressAndSavesUpdatedBatch() {
    Instant now = Instant.parse("2026-02-03T10:00:00Z");
    OutboxEvent first = outboxEvent(
        UUID.randomUUID(),
        tenant(UUID.randomUUID()),
        OutboxEventAggregateType.EVENT,
        UUID.randomUUID(),
        OutboxEventType.EVENT_ACCEPTED,
        new TextNode("payload"),
        now.minusSeconds(60));
    OutboxEvent second = outboxEvent(
        UUID.randomUUID(),
        tenant(UUID.randomUUID()),
        OutboxEventAggregateType.DELIVERY,
        UUID.randomUUID(),
        OutboxEventType.DELIVERY_CREATED_WEBHOOK,
        new TextNode("payload"),
        now.minusSeconds(30));
    when(repository.claimNextBatch(10, now, now.minusMillis(300000L))).thenReturn(List.of(first, second));
    when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    List<OutboxEvent> result = service.claim(10, now);

    assertSame(first, result.get(0));
    assertSame(second, result.get(1));
    assertEquals(OutboxStatus.IN_PROGRESS, first.getStatus());
    assertEquals(1, first.getAttemptCount());
    assertEquals(now, first.getLastAttemptAt());
    assertEquals(OutboxStatus.IN_PROGRESS, second.getStatus());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<OutboxEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(eventsCaptor.capture());
    assertEquals(2, eventsCaptor.getValue().size());
    verify(notificationMetrics).incrementOutboxClaimed(2);
  }
}
