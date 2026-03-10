package lv.janis.notification_platform.adminapi.application.validation.endpoint;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.delivery.domain.EndpointType;

@Component
public class EmailEndpointConfigValidator implements EndpointConfigValidator {
  private static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

  @Override
  public EndpointType supports() {
    return EndpointType.EMAIL;
  }

  @Override
  public void validate(JsonNode config) {
    if (config == null || !config.isObject()) {
      throw new BadRequestException("config must be a JSON object");
    }

    JsonNode recipientsNode = config.get("recipients");
    if (recipientsNode == null || !recipientsNode.isArray() || recipientsNode.isEmpty()) {
      throw new BadRequestException("EMAIL config requires non-empty 'recipients' array");
    }

    for (JsonNode recipient : recipientsNode) {
      if (!recipient.isTextual() || !isValidEmail(recipient.asText())) {
        throw new BadRequestException("EMAIL 'recipients' contains invalid email");
      }
    }

    JsonNode fromNode = config.get("from");
    if (fromNode != null && (!fromNode.isTextual() || !isValidEmail(fromNode.asText()))) {
      throw new BadRequestException("EMAIL 'from' must be a valid email");
    }

    JsonNode replyToNode = config.get("replyTo");
    if (replyToNode != null && (!replyToNode.isTextual() || !isValidEmail(replyToNode.asText()))) {
      throw new BadRequestException("EMAIL 'replyTo' must be a valid email");
    }

    JsonNode subjectTemplateNode = config.get("subjectTemplate");
    if (subjectTemplateNode != null && !subjectTemplateNode.isTextual()) {
      throw new BadRequestException("EMAIL 'subjectTemplate' must be a string");
    }

    JsonNode bodyTemplateNode = config.get("bodyTemplate");
    if (bodyTemplateNode != null && !bodyTemplateNode.isTextual()) {
      throw new BadRequestException("EMAIL 'bodyTemplate' must be a string");
    }

    JsonNode bodyTypeNode = config.get("bodyType");
    if (bodyTypeNode != null) {
      if (!bodyTypeNode.isTextual()) {
        throw new BadRequestException("EMAIL 'bodyType' must be a string");
      }
      String bodyType = bodyTypeNode.asText().trim().toLowerCase();
      if (!("text".equals(bodyType) || "html".equals(bodyType))) {
        throw new BadRequestException("EMAIL 'bodyType' must be either 'text' or 'html'");
      }
    }

    JsonNode templateNameNode = config.get("templateName");
    if (templateNameNode != null && !templateNameNode.isTextual()) {
      throw new BadRequestException("EMAIL 'templateName' must be a string");
    }
  }

  private boolean isValidEmail(String email) {
    return email != null && SIMPLE_EMAIL_PATTERN.matcher(email).matches();
  }
}
