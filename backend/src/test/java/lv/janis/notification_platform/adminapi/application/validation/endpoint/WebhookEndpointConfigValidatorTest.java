package lv.janis.notification_platform.adminapi.application.validation.endpoint;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import org.junit.jupiter.api.Test;

class WebhookEndpointConfigValidatorTest {
  private final WebhookEndpointConfigValidator validator = new WebhookEndpointConfigValidator();

  @Test
  void supportsReturnsWebhookType() {
    assertEquals(EndpointType.WEBHOOK, validator.supports());
  }

  @Test
  void validateAcceptsValidConfig() {
    ObjectNode config = JsonNodeFactory.instance.objectNode()
        .put("url", "https://example.com/hook")
        .set("headers", JsonNodeFactory.instance.objectNode().put("X-Key", "value"));
    config.put("connectTimeoutMs", 2500);
    config.put("responseTimeoutMs", 8000);
    config.put("connectionRequestTimeoutMs", 2000);

    assertDoesNotThrow(() -> validator.validate(config));
  }

  @Test
  void validateRejectsInvalidUrlScheme() {
    ObjectNode config = JsonNodeFactory.instance.objectNode().put("url", "ftp://example.com/hook");

    BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validate(config));

    assertEquals("WEBHOOK 'url' must use http or https scheme", ex.getMessage());
  }

  @Test
  void validateRejectsNonStringHeaderValue() {
    ObjectNode config = JsonNodeFactory.instance.objectNode().put("url", "https://example.com/hook");
    config.set("headers", JsonNodeFactory.instance.objectNode().put("X-Retry", 1));

    BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validate(config));

    assertEquals("WEBHOOK header values must be strings: X-Retry", ex.getMessage());
  }

  @Test
  void validateRejectsTimeoutOutsideAllowedRange() {
    ObjectNode config = JsonNodeFactory.instance.objectNode()
        .put("url", "https://example.com/hook")
        .put("responseTimeoutMs", 12000);

    BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validate(config));

    assertEquals("WEBHOOK 'responseTimeoutMs' must be between 5000 and 10000", ex.getMessage());
  }
}
