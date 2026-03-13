package lv.janis.notification_platform.adminapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.ListOutboxEventsQuery;
import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;
import lv.janis.notification_platform.outbox.application.port.out.OutboxFilter;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class OutboxMonitoringServiceTest {
  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID AGGREGATE_ID = UUID.randomUUID();

  private final OutboxEventRepositoryPort repository = mock(OutboxEventRepositoryPort.class);
  private final OutboxMonitoringService service = new OutboxMonitoringService(repository);

  @Test
  void listOutboxEventsClampsPageAndSizeAndDelegatesFilter() {
    Instant from = Instant.parse("2026-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-01-02T00:00:00Z");
    ListOutboxEventsQuery query = new ListOutboxEventsQuery(
        -1,
        500,
        OutboxStatus.PENDING,
        TENANT_ID,
        OutboxEventType.EVENT_ACCEPTED,
        OutboxEventAggregateType.EVENT,
        AGGREGATE_ID,
        from,
        to);

    Page<OutboxEvent> expected = new PageImpl<>(List.of());
    when(repository.findAll(any(), any())).thenReturn(expected);

    Page<OutboxEvent> result = service.listOutboxEvents(query);

    assertSame(expected, result);

    ArgumentCaptor<OutboxFilter> filterCaptor = ArgumentCaptor.forClass(OutboxFilter.class);
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).findAll(filterCaptor.capture(), pageableCaptor.capture());

    OutboxFilter filter = filterCaptor.getValue();
    Pageable pageable = pageableCaptor.getValue();
    assertEquals(0, pageable.getPageNumber());
    assertEquals(100, pageable.getPageSize());
    assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
    assertEquals(OutboxStatus.PENDING, filter.status());
    assertEquals(TENANT_ID, filter.tenantId());
    assertEquals(OutboxEventType.EVENT_ACCEPTED, filter.eventType());
    assertEquals(OutboxEventAggregateType.EVENT, filter.aggregateType());
    assertEquals(AGGREGATE_ID, filter.aggregateId());
    assertEquals(from, filter.from());
    assertEquals(to, filter.to());
  }

  @Test
  void listOutboxEventsRejectsInvalidTimeRange() {
    ListOutboxEventsQuery query = new ListOutboxEventsQuery(
        0,
        10,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2026-01-02T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"));

    BadRequestException ex = assertThrows(BadRequestException.class, () -> service.listOutboxEvents(query));
    assertEquals("from must be before or equal to to", ex.getMessage());
    verify(repository, never()).findAll(any(), any());
  }

  @Test
  void getOutboxEventByIdReturnsEventWhenPresent() {
    UUID eventId = UUID.randomUUID();
    OutboxEvent event = mock(OutboxEvent.class);
    when(repository.findById(eventId)).thenReturn(Optional.of(event));

    OutboxEvent result = service.getOutboxEventById(eventId);

    assertSame(event, result);
    verify(repository).findById(eventId);
  }

  @Test
  void getOutboxEventByIdThrowsNotFoundWhenMissing() {
    UUID eventId = UUID.randomUUID();
    when(repository.findById(eventId)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getOutboxEventById(eventId));
    assertEquals("Outbox event with id " + eventId + " not found", ex.getMessage());
  }
}
