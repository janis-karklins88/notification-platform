package lv.janis.notification_platform.ingest.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.ingest.application.port.in.IngestCommand;
import lv.janis.notification_platform.ingest.application.port.in.IngestResult;
import lv.janis.notification_platform.ingest.application.port.in.IngestUseCase;
import lv.janis.notification_platform.ingest.application.port.out.EventRepositoryPort;
import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.shared.metrics.NotificationMetrics;

@Service
public class IngestService implements IngestUseCase {
  private final EventRepositoryPort eventRepositoryPort;
  private final TenantRepositoryPort tenantRepositoryPort;
  private final OutboxEventRepositoryPort outboxEventRepositoryPort;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final NotificationMetrics notificationMetrics;

  public IngestService(
      EventRepositoryPort eventRepositoryPort,
      TenantRepositoryPort tenantRepositoryPort,
      OutboxEventRepositoryPort outboxEventRepositoryPort,
      ObjectMapper objectMapper,
      Clock clock,
      NotificationMetrics notificationMetrics) {
    this.eventRepositoryPort = eventRepositoryPort;
    this.tenantRepositoryPort = tenantRepositoryPort;
    this.outboxEventRepositoryPort = outboxEventRepositoryPort;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.notificationMetrics = notificationMetrics;
  }

  @Override
  @Transactional
  public IngestResult ingest(IngestCommand command) {
    String normalizedIdempotencyKey = normalizeOptional(command.idempotencyKey());

    if (normalizedIdempotencyKey != null) {
      var existing = eventRepositoryPort.findByTenantIdAndIdempotencyKey(command.tenantId(), normalizedIdempotencyKey);
      if (existing.isPresent()) {
        var event = existing.get();
        return new IngestResult(event.getId(), event.getStatus(), true);
      }
    }

    var tenant = tenantRepositoryPort.findById(command.tenantId())
        .orElseThrow(() -> new NotFoundException("Tenant not found: " + command.tenantId()));

    Event event = new Event(
        tenant,
        command.eventType(),
        normalizedIdempotencyKey,
        command.payload(),
        command.source(),
        command.traceId());

    Event saved = eventRepositoryPort.save(event);
    OutboxEvent outboxEvent = createEventAcceptedOutboxEvent(saved, Instant.now(clock));
    outboxEventRepositoryPort.save(outboxEvent);
    notificationMetrics.incrementEventAccepted();
    return new IngestResult(saved.getId(), saved.getStatus(), false);
  }

  @Override
  public Event getEventById(UUID tenantId, UUID eventId) {
    return eventRepositoryPort.findByIdAndTenantId(eventId, tenantId)
        .orElseThrow(() -> new NotFoundException("Event not found: " + eventId));
  }

  private String normalizeOptional(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private OutboxEvent createEventAcceptedOutboxEvent(Event event, Instant timestamp) {
    JsonNode payload = objectMapper.createObjectNode()
        .put("eventId", event.getId().toString())
        .put("tenantId", event.getTenantId().toString())
        .put("eventType", event.getEventType());

    return new OutboxEvent(
        event.getTenant(),
        OutboxEventAggregateType.EVENT,
        event.getId(),
        OutboxEventType.EVENT_ACCEPTED,
        payload,
        timestamp);
  }
}
