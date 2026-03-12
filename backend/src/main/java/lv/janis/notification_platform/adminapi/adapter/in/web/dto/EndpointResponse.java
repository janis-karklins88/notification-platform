package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;

public record EndpointResponse(
    UUID id,
    UUID tenantId,
    EndpointType type,
    EndpointStatus status,
    JsonNode config,
    Instant createdAt,
    Instant updatedAt) {
  public static EndpointResponse from(Endpoint endpoint) {
    return new EndpointResponse(
        endpoint.getId(),
        endpoint.getTenantId(),
        endpoint.getType(),
        endpoint.getStatus(),
        endpoint.getConfig(),
        endpoint.getCreatedAt(),
        endpoint.getUpdatedAt());
  }

}
