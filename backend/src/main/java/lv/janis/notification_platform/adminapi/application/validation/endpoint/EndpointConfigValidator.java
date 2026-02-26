package lv.janis.notification_platform.adminapi.application.validation.endpoint;

import com.fasterxml.jackson.databind.JsonNode;

import lv.janis.notification_platform.delivery.domain.EndpointType;

public interface EndpointConfigValidator {
  EndpointType supports();

  void validate(JsonNode config);
}
