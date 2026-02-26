package lv.janis.notification_platform.adminapi.application.validation.endpoint;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.delivery.domain.EndpointType;

@Component
public class WebhookEndpointConfigValidator implements EndpointConfigValidator {

  @Override
  public EndpointType supports() {
    return EndpointType.WEBHOOK;
  }

  @Override
  public void validate(JsonNode config) {
    if (config == null || !config.isObject()) {
      throw new BadRequestException("config must be a JSON object");
    }

    JsonNode urlNode = config.get("url");
    if (urlNode == null || !urlNode.isTextual() || urlNode.asText().isBlank()) {
      throw new BadRequestException("WEBHOOK config requires non-empty 'url'");
    }

    String url = urlNode.asText();
    try {
      URI uri = new URI(url);
      String scheme = uri.getScheme();
      if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
        throw new BadRequestException("WEBHOOK 'url' must use http or https scheme");
      }
      if (uri.getHost() == null || uri.getHost().isBlank()) {
        throw new BadRequestException("WEBHOOK 'url' must include host");
      }
    } catch (URISyntaxException e) {
      throw new BadRequestException("WEBHOOK 'url' is not a valid URI");
    }

    JsonNode headersNode = config.get("headers");
    if (headersNode != null && !headersNode.isObject()) {
      throw new BadRequestException("WEBHOOK 'headers' must be a JSON object");
    }

    JsonNode timeoutNode = config.get("timeoutMs");
    if (timeoutNode != null) {
      if (!timeoutNode.canConvertToInt()) {
        throw new BadRequestException("WEBHOOK 'timeoutMs' must be an integer");
      }
      int timeout = timeoutNode.asInt();
      if (timeout < 100 || timeout > 30000) {
        throw new BadRequestException("WEBHOOK 'timeoutMs' must be between 100 and 30000");
      }
    }
  }
}
