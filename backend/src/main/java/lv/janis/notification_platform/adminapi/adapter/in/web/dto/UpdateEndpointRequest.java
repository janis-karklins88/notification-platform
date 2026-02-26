package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotNull;

public record UpdateEndpointRequest(@NotNull JsonNode config) {

}
