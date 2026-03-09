package lv.janis.notification_platform.delivery.adapter.out.sender;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import lv.janis.notification_platform.delivery.application.exception.DeliveryNonRetryableException;
import lv.janis.notification_platform.delivery.application.port.out.WebhookSenderPort;
import lv.janis.notification_platform.delivery.domain.Delivery;

@Component
public class WebhookSenderAdapter implements WebhookSenderPort {

  private static final int DEFAULT_TIMEOUT_MS = 5000;
  private static final Set<Integer> NON_RETRYABLE_HTTP_STATUSES = Set.of(400, 401, 403, 404, 405, 409, 410, 413, 414,
      415, 422,
      426);
  private static final Logger log = LoggerFactory.getLogger(WebhookSenderAdapter.class);
  private final RestTemplateBuilder restTemplateBuilder;
  private final Clock clock;
  private final ConcurrentMap<Integer, RestTemplate> restTemplateByTimeout;

  public WebhookSenderAdapter(RestTemplateBuilder restTemplateBuilder, Clock clock) {
    this.restTemplateBuilder = restTemplateBuilder;
    this.clock = clock;
    this.restTemplateByTimeout = new ConcurrentHashMap<>();
  }

  @Override
  public void send(Delivery delivery) {
    JsonNode config = delivery.getEndpoint().getConfig();
    String url = requiredText(config, "url");
    int timeoutMs = config.path("timeoutMs").asInt(DEFAULT_TIMEOUT_MS);
    String deliveryId = delivery.getId().toString();
    String eventId = delivery.getEventId().toString();
    String tenantId = delivery.getTenantId().toString();
    String subscriptionId = delivery.getSubscriptionId().toString();

    HttpHeaders headers = new HttpHeaders();

    if (config.has("headers") && config.get("headers").isObject()) {
      for (var entry : config.get("headers").properties()) {
        JsonNode headerValue = entry.getValue();

        if (!headerValue.isTextual()) {
          throw new DeliveryNonRetryableException(
              "Webhook header must be text: " + entry.getKey());
        }
        headers.set(entry.getKey(), headerValue.asText());
      }
    }

    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Delivery-Id", deliveryId);
    headers.set("X-Delivery-Event-Id", eventId);
    headers.set("X-Tenant-Id", tenantId);
    headers.set("X-Subscription-Id", subscriptionId);

    Map<String, Object> body = new HashMap<>();
    body.put("deliveryId", deliveryId);
    body.put("eventId", eventId);
    body.put("tenantId", tenantId);
    body.put("subscriptionId", subscriptionId);
    body.put("eventType", delivery.getEvent().getEventType());
    body.put("payload", delivery.getEvent().getPayload());
    body.put("createdAt", delivery.getCreatedAt().toString());
    body.put("occurredAt",
        delivery.getEvent().getReceivedAt() != null ? delivery.getEvent().getReceivedAt().toString() : null);
    body.put("attemptedAt", clock.instant().toString());

    try {
      ResponseEntity<Void> response = getRestTemplate(timeoutMs).exchange(
          url,
          HttpMethod.POST,
          new HttpEntity<>(body, headers),
          Void.class);

      log.debug("Webhook delivered for deliveryId={} to {} status={}", deliveryId, url, response.getStatusCode());
    } catch (HttpStatusCodeException ex) {
      int status = ex.getStatusCode().value();
      if (isNonRetryableStatus(status)) {
        throw new DeliveryNonRetryableException(
            "Webhook returned non-retryable HTTP " + status + " for delivery " + deliveryId, ex);
      }

      log.warn("Webhook delivery retryable for deliveryId={} endpoint={} status={}", deliveryId, url,
          ex.getStatusCode(), ex);
      throw ex;
    } catch (ResourceAccessException ex) {
      log.warn("Webhook transport failed for deliveryId={} endpoint={} (retryable)", deliveryId, url, ex);
      throw ex;
    } catch (IllegalArgumentException ex) {
      throw new DeliveryNonRetryableException("Webhook send failed for delivery " + deliveryId + " at " + url, ex);
    }
  }

  private static String requiredText(JsonNode config, String key) {
    if (!config.hasNonNull(key) || !config.get(key).isTextual() || config.get(key).asText().isBlank()) {
      throw new DeliveryNonRetryableException("Missing required webhook config: " + key);
    }
    return config.get(key).asText();
  }

  private RestTemplate getRestTemplate(int timeoutMs) {
    return restTemplateByTimeout.computeIfAbsent(timeoutMs, ms -> restTemplateBuilder
        .connectTimeout(Duration.ofMillis(ms))
        .readTimeout(Duration.ofMillis(ms))
        .build());
  }

  private boolean isNonRetryableStatus(int status) {
    return NON_RETRYABLE_HTTP_STATUSES.contains(status);
  }
}
