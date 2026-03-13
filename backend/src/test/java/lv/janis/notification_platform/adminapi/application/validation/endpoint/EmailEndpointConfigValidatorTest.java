package lv.janis.notification_platform.adminapi.application.validation.endpoint;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import org.junit.jupiter.api.Test;

class EmailEndpointConfigValidatorTest {
  private final EmailEndpointConfigValidator validator = new EmailEndpointConfigValidator();

  @Test
  void supportsReturnsEmailType() {
    assertEquals(EndpointType.EMAIL, validator.supports());
  }

  @Test
  void validateAcceptsValidConfig() {
    ObjectNode config = JsonNodeFactory.instance.objectNode();
    config.putArray("recipients")
        .add("to@example.com")
        .add("second@example.com");
    config.put("from", "from@example.com");
    config.put("replyTo", "reply@example.com");
    config.put("subjectTemplate", "Hello");
    config.put("bodyTemplate", "Body");
    config.put("bodyType", "html");
    config.put("templateName", "welcome");

    assertDoesNotThrow(() -> validator.validate(config));
  }

  @Test
  void validateRejectsMissingRecipients() {
    BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validate(JsonNodeFactory.instance.objectNode()));

    assertEquals("EMAIL config requires non-empty 'recipients' array", ex.getMessage());
  }

  @Test
  void validateRejectsInvalidRecipientEmail() {
    ObjectNode config = JsonNodeFactory.instance.objectNode();
    config.putArray("recipients").add("not-an-email");

    BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validate(config));

    assertEquals("EMAIL 'recipients' contains invalid email", ex.getMessage());
  }

  @Test
  void validateRejectsInvalidBodyType() {
    ObjectNode config = JsonNodeFactory.instance.objectNode();
    config.putArray("recipients").add("to@example.com");
    config.put("bodyType", "markdown");

    BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validate(config));

    assertEquals("EMAIL 'bodyType' must be either 'text' or 'html'", ex.getMessage());
  }
}
