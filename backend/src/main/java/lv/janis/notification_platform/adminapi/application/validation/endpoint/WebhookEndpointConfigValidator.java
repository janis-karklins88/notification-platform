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
    if (headersNode != null) {
      if (!headersNode.isObject()) {
        throw new BadRequestException("WEBHOOK 'headers' must be a JSON object");
      }

      for (var entry : headersNode.properties()) {
        if (!entry.getValue().isTextual()) {
          throw new BadRequestException("WEBHOOK header values must be strings: " + entry.getKey());
        }
      }
    }

    validateTimeout(config, "connectTimeoutMs", 2000, 3000);
    validateTimeout(config, "responseTimeoutMs", 5000, 10000);
    validateTimeout(config, "connectionRequestTimeoutMs", 1000, 3000);
  }

  private void validateTimeout(JsonNode config, String key, int min, int max) {
    JsonNode node = config.get(key);
    if (node == null) {
      return;
    }

    if (!node.canConvertToInt()) {
      throw new BadRequestException("WEBHOOK '" + key + "' must be an integer");
    }

    int timeout = node.asInt();
    if (timeout < min || timeout > max) {
      throw new BadRequestException("WEBHOOK '" + key + "' must be between " + min + " and " + max);
    }
  }
}
