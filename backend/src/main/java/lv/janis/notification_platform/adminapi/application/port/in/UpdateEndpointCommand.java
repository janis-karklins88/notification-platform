package lv.janis.notification_platform.adminapi.application.port.in;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public record UpdateEndpointCommand(
    UUID endpointId,
    JsonNode config) {
}
