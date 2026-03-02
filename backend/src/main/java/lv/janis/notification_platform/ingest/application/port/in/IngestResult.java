package lv.janis.notification_platform.ingest.application.port.in;

import java.util.UUID;

import lv.janis.notification_platform.ingest.domain.EventStatus;

public record IngestResult(
    UUID eventId,
    EventStatus status,
    boolean duplicate) {
}
