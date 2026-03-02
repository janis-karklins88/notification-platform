package lv.janis.notification_platform.ingest.application.port.in;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public record IngestCommand(
    UUID tenantId,
    String eventType,
    JsonNode payload,
    String idempotencyKey,
    String source,
    String traceId) {
}
