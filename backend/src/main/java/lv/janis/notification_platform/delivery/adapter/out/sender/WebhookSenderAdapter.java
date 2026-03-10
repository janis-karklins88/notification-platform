package lv.janis.notification_platform.delivery.adapter.out.sender;

import java.time.Clock;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import lv.janis.notification_platform.delivery.application.exception.DeliveryNonRetryableException;
import lv.janis.notification_platform.delivery.domain.Delivery;

public class WebhookSenderAdapter implements DisposableBean {

  private static final Set<Integer> NON_RETRYABLE_HTTP_STATUSES = Set.of(400, 401, 403, 404, 405, 409, 410, 413, 414,
      415, 422,
      426);
  private static final int MAX_TOTAL_CONNECTIONS = 100;
  private static final int MAX_CONNECTIONS_PER_ROUTE = 20;
  private static final Logger log = LoggerFactory.getLogger(WebhookSenderAdapter.class);

  private final RestTemplateBuilder restTemplateBuilder;
  private final Clock clock;

  private final Map<WebhookTimeouts, RestTemplateHolder> restTemplateByTimeout = new ConcurrentHashMap<>();

  public WebhookSenderAdapter(RestTemplateBuilder restTemplateBuilder, Clock clock) {
    this.restTemplateBuilder = restTemplateBuilder;
    this.clock = clock;

  }

  public void send(Delivery delivery) {

    JsonNode config = delivery.getEndpoint().getConfig();

    String deliveryId = delivery.getId().toString();
    String url = requiredText(config, "url");
    WebhookTimeouts timeouts = resolveTimeouts(config, deliveryId);

    String eventId = delivery.getEventId().toString();
    String tenantId = delivery.getTenantId().toString();
    String subscriptionId = delivery.getSubscriptionId().toString();

    HttpHeaders headers = new HttpHeaders();

    JsonNode headersNode = config.get("headers");

    if (headersNode != null && headersNode.isObject()) {
      for (var entry : headersNode.properties()) {

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
        delivery.getEvent().getReceivedAt() != null
            ? delivery.getEvent().getReceivedAt().toString()
            : null);
    body.put("attemptedAt", clock.instant().toString());

    try {

      ResponseEntity<Void> response = getRestTemplate(timeouts).exchange(
          url,
          HttpMethod.POST,
          new HttpEntity<>(body, headers),
          Void.class);

      log.debug(
          "Webhook delivered for deliveryId={} to {} status={}",
          deliveryId,
          url,
          response.getStatusCode());

    } catch (HttpStatusCodeException ex) {

      int status = ex.getStatusCode().value();

      if (isNonRetryableStatus(status)) {
        throw new DeliveryNonRetryableException(
            "Webhook returned non-retryable HTTP " + status + " for delivery " + deliveryId,
            ex);
      }

      log.warn(
          "Webhook delivery retryable for deliveryId={} endpoint={} status={}",
          deliveryId,
          url,
          ex.getStatusCode(),
          ex);

      throw ex;

    } catch (ResourceAccessException ex) {

      log.warn(
          "Webhook transport failed for deliveryId={} endpoint={} (retryable)",
          deliveryId,
          url,
          ex);

      throw ex;

    } catch (IllegalArgumentException ex) {

      throw new DeliveryNonRetryableException(
          "Webhook send failed for delivery " + deliveryId + " at " + url,
          ex);
    }
  }

  @Override
  public void destroy() {
    restTemplateByTimeout.values().forEach(holder -> {
      try {
        holder.httpClient().close();
      } catch (IOException ex) {
        log.warn("Failed to close webhook HttpClient", ex);
      }
    });
  }

  private static String requiredText(JsonNode config, String key) {

    if (!config.hasNonNull(key)
        || !config.get(key).isTextual()
        || config.get(key).asText().isBlank()) {

      throw new DeliveryNonRetryableException(
          "Missing required webhook config: " + key);
    }

    return config.get(key).asText();
  }

  private RestTemplate getRestTemplate(WebhookTimeouts timeouts) {
    return restTemplateByTimeout.computeIfAbsent(
        timeouts,
        this::buildRestTemplate)
        .restTemplate();
  }

  private WebhookTimeouts resolveTimeouts(JsonNode config, String deliveryId) {
    int connectTimeoutMs = requiredTimeoutMs(config, "connectTimeoutMs", deliveryId);
    int responseTimeoutMs = requiredTimeoutMs(config, "responseTimeoutMs", deliveryId);
    int connectionRequestTimeoutMs = requiredTimeoutMs(config, "connectionRequestTimeoutMs", deliveryId);

    return new WebhookTimeouts(connectTimeoutMs, responseTimeoutMs, connectionRequestTimeoutMs);
  }

  private int requiredTimeoutMs(JsonNode config, String key, String deliveryId) {
    JsonNode node = config.get(key);
    if (node == null || !node.canConvertToInt()) {
      throw new DeliveryNonRetryableException(
          "Missing or invalid required webhook config timeout: " + key + " for delivery " + deliveryId);
    }

    return node.asInt();
  }

  private boolean isNonRetryableStatus(int status) {
    return NON_RETRYABLE_HTTP_STATUSES.contains(status);
  }

  private RestTemplateHolder buildRestTemplate(WebhookTimeouts timeouts) {

    RequestConfig requestConfig = RequestConfig.custom()
        .setResponseTimeout(Timeout.ofMilliseconds(timeouts.responseTimeoutMs))
        .setConnectionRequestTimeout(Timeout.ofMilliseconds(timeouts.connectionRequestTimeoutMs))
        .build();

    ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(Timeout.ofMilliseconds(timeouts.connectTimeoutMs))
        .build();

    PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
        .setDefaultConnectionConfig(connectionConfig)
        .setMaxConnTotal(MAX_TOTAL_CONNECTIONS)
        .setMaxConnPerRoute(MAX_CONNECTIONS_PER_ROUTE)
        .build();

    CloseableHttpClient httpClient = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .evictExpiredConnections()
        .evictIdleConnections(Timeout.ofSeconds(30))
        .build();

    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

    RestTemplate restTemplate = restTemplateBuilder
        .requestFactory(() -> requestFactory)
        .build();

    return new RestTemplateHolder(restTemplate, httpClient);
  }

  private static final class WebhookTimeouts {
    private final int connectTimeoutMs;
    private final int responseTimeoutMs;
    private final int connectionRequestTimeoutMs;

    private WebhookTimeouts(
        int connectTimeoutMs,
        int responseTimeoutMs,
        int connectionRequestTimeoutMs) {
      this.connectTimeoutMs = connectTimeoutMs;
      this.responseTimeoutMs = responseTimeoutMs;
      this.connectionRequestTimeoutMs = connectionRequestTimeoutMs;
    }

    @Override
    public int hashCode() {
      int result = connectTimeoutMs;
      result = 31 * result + responseTimeoutMs;
      return 31 * result + connectionRequestTimeoutMs;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof WebhookTimeouts)) {
        return false;
      }
      WebhookTimeouts that = (WebhookTimeouts) o;
      return connectTimeoutMs == that.connectTimeoutMs
          && responseTimeoutMs == that.responseTimeoutMs
          && connectionRequestTimeoutMs == that.connectionRequestTimeoutMs;
    }
  }

  private static final record RestTemplateHolder(RestTemplate restTemplate, CloseableHttpClient httpClient) {

  }
}
