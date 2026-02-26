package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotNull;
import lv.janis.notification_platform.delivery.domain.EndpointType;

public record CreateEndpointRequest(
    @NotNull EndpointType type,
    @NotNull JsonNode config) {

}
