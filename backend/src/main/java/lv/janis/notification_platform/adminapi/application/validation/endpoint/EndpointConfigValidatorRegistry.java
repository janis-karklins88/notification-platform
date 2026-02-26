package lv.janis.notification_platform.adminapi.application.validation.endpoint;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.delivery.domain.EndpointType;

@Component
public class EndpointConfigValidatorRegistry {
  private final Map<EndpointType, EndpointConfigValidator> validatorsByType;

  public EndpointConfigValidatorRegistry(List<EndpointConfigValidator> validators) {
    this.validatorsByType = new EnumMap<>(EndpointType.class);
    for (EndpointConfigValidator validator : validators) {
      EndpointType type = validator.supports();
      EndpointConfigValidator previous = validatorsByType.put(type, validator);
      if (previous != null) {
        throw new IllegalStateException("Duplicate endpoint config validator for type: " + type);
      }
    }
  }

  public EndpointConfigValidator get(EndpointType type) {
    if (type == null) {
      throw new BadRequestException("type must not be null");
    }

    EndpointConfigValidator validator = validatorsByType.get(type);
    if (validator == null) {
      throw new BadRequestException("Endpoint type is not supported yet: " + type);
    }
    return validator;
  }
}
