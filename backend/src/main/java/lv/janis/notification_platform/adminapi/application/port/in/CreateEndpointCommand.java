package lv.janis.notification_platform.adminapi.application.port.in;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import lv.janis.notification_platform.delivery.domain.EndpointType;

public record CreateEndpointCommand(
    UUID tenantId,
    EndpointType type,
    JsonNode config) {
}
