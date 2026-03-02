package lv.janis.notification_platform.ingest.adapter.in.web.dto;

import java.util.UUID;

import lv.janis.notification_platform.ingest.application.port.in.IngestResult;
import lv.janis.notification_platform.ingest.domain.EventStatus;

public record IngestResponse(
    UUID eventId,
    EventStatus status,
    boolean duplicate) {
  public static IngestResponse from(IngestResult result) {
    return new IngestResponse(result.eventId(), result.status(), result.duplicate());
  }
}
