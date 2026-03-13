package lv.janis.notification_platform.adminapi.application.validation.endpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import org.junit.jupiter.api.Test;

class EndpointConfigValidatorRegistryTest {

  @Test
  void getReturnsValidatorForSupportedType() {
    EndpointConfigValidator validator = mock(EndpointConfigValidator.class);
    when(validator.supports()).thenReturn(EndpointType.EMAIL);
    EndpointConfigValidatorRegistry registry = new EndpointConfigValidatorRegistry(List.of(validator));

    EndpointConfigValidator result = registry.get(EndpointType.EMAIL);

    assertSame(validator, result);
  }

  @Test
  void getRejectsNullType() {
    EndpointConfigValidatorRegistry registry = new EndpointConfigValidatorRegistry(List.of());

    BadRequestException ex = assertThrows(BadRequestException.class, () -> registry.get(null));

    assertEquals("type must not be null", ex.getMessage());
  }

  @Test
  void getRejectsUnsupportedType() {
    EndpointConfigValidator validator = mock(EndpointConfigValidator.class);
    when(validator.supports()).thenReturn(EndpointType.EMAIL);
    EndpointConfigValidatorRegistry registry = new EndpointConfigValidatorRegistry(List.of(validator));

    BadRequestException ex = assertThrows(BadRequestException.class, () -> registry.get(EndpointType.WEBHOOK));

    assertEquals("Endpoint type is not supported yet: WEBHOOK", ex.getMessage());
  }

  @Test
  void constructorRejectsDuplicateValidatorsForSameType() {
    EndpointConfigValidator first = mock(EndpointConfigValidator.class);
    EndpointConfigValidator second = mock(EndpointConfigValidator.class);
    when(first.supports()).thenReturn(EndpointType.EMAIL);
    when(second.supports()).thenReturn(EndpointType.EMAIL);

    IllegalStateException ex = assertThrows(
        IllegalStateException.class,
        () -> new EndpointConfigValidatorRegistry(List.of(first, second)));

    assertEquals("Duplicate endpoint config validator for type: EMAIL", ex.getMessage());
  }
}
