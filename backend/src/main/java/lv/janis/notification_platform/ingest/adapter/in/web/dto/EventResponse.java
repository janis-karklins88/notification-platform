package lv.janis.notification_platform.ingest.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.ingest.domain.EventStatus;

public record EventResponse(UUID eventId, String eventType, EventStatus status, Instant receivedAt, Instant updatedAt,
    String source, String traceId, String idempotencyKey) {

  public static EventResponse from(Event event) {
    return new EventResponse(
        event.getId(),
        event.getEventType(),
        event.getStatus(),
        event.getReceivedAt(),
        event.getUpdatedAt(),
        event.getSource(),
        event.getTraceId(),
        event.getIdempotencyKey());
  }
}
