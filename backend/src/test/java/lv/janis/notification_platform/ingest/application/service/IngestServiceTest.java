package lv.janis.notification_platform.ingest.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.ingest.application.port.in.IngestCommand;
import lv.janis.notification_platform.ingest.application.port.in.IngestResult;
import lv.janis.notification_platform.ingest.application.port.out.EventRepositoryPort;
import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.shared.metrics.NotificationMetrics;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static lv.janis.notification_platform.support.EntityTestData.event;
import static lv.janis.notification_platform.support.EntityTestData.tenant;

class IngestServiceTest {
  private static final Instant NOW = Instant.parse("2026-02-01T12:00:00Z");

  private final EventRepositoryPort eventRepository = mock(EventRepositoryPort.class);
  private final TenantRepositoryPort tenantRepository = mock(TenantRepositoryPort.class);
  private final OutboxEventRepositoryPort outboxRepository = mock(OutboxEventRepositoryPort.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final NotificationMetrics notificationMetrics = mock(NotificationMetrics.class);
  private final IngestService service = new IngestService(
      eventRepository,
      tenantRepository,
      outboxRepository,
      objectMapper,
      clock,
      notificationMetrics);

  @Test
  void ingestReturnsExistingEventWhenIdempotencyKeyMatches() {
    UUID tenantId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    Event existing = event(eventId, tenant(tenantId), "order.created");
    when(eventRepository.findByTenantIdAndIdempotencyKey(tenantId, "idem-1")).thenReturn(Optional.of(existing));

    IngestResult result = service.ingest(new IngestCommand(
        tenantId,
        "order.created",
        JsonNodeFactory.instance.objectNode(),
        " idem-1 ",
        "api",
        "trace"));

    assertEquals(eventId, result.eventId());
    assertEquals(existing.getStatus(), result.status());
    assertEquals(true, result.duplicate());
    verify(tenantRepository, never()).findById(any());
    verify(outboxRepository, never()).save(any());
    verify(notificationMetrics, never()).incrementEventAccepted();
  }

  @Test
  void ingestSavesEventAndOutboxEventAndIncrementsMetric() {
    UUID tenantId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    Tenant tenant = tenant(tenantId);
    ObjectNode payload = JsonNodeFactory.instance.objectNode().put("orderId", "123");
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(eventRepository.save(any())).thenAnswer(invocation -> {
      Event event = invocation.getArgument(0);
      ReflectionTestUtils.setField(event, "id", eventId);
      ReflectionTestUtils.setField(event, "tenantId", tenantId);
      return event;
    });
    when(outboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    IngestResult result = service.ingest(new IngestCommand(
        tenantId,
        "order.created",
        payload,
        " idem-1 ",
        "api",
        "trace-1"));

    assertEquals(eventId, result.eventId());
    assertEquals(false, result.duplicate());

    ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(eventCaptor.capture());
    assertEquals("idem-1", eventCaptor.getValue().getIdempotencyKey());
    assertSame(payload, eventCaptor.getValue().getPayload());

    ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxRepository).save(outboxCaptor.capture());
    assertEquals(OutboxEventAggregateType.EVENT, outboxCaptor.getValue().getAggregateType());
    assertEquals(OutboxEventType.EVENT_ACCEPTED, outboxCaptor.getValue().getEventType());
    assertEquals(eventId, outboxCaptor.getValue().getAggregateId());
    assertEquals(eventId.toString(), outboxCaptor.getValue().getPayload().get("eventId").asText());
    assertEquals(tenantId.toString(), outboxCaptor.getValue().getPayload().get("tenantId").asText());
    assertEquals("order.created", outboxCaptor.getValue().getPayload().get("eventType").asText());
    verify(notificationMetrics).incrementEventAccepted();
  }

  @Test
  void getEventByIdReturnsEventWhenPresent() {
    UUID tenantId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    Event expected = mock(Event.class);
    when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(expected));

    Event result = service.getEventById(tenantId, eventId);

    assertSame(expected, result);
  }

  @Test
  void getEventByIdThrowsNotFoundWhenMissing() {
    UUID tenantId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getEventById(tenantId, eventId));

    assertEquals("Event not found: " + eventId, ex.getMessage());
  }
}
