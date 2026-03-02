package lv.janis.notification_platform.ingest.adapter.in.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IngestRequest(
    @NotBlank @Size(max = 150) String eventType,
    @NotNull JsonNode payload,
    @Size(max = 128) String idempotencyKey,
    @Size(max = 100) String source,
    @Size(max = 100) String traceId) {
}
