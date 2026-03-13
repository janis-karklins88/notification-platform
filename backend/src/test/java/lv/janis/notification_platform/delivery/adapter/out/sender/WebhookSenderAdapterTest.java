package lv.janis.notification_platform.delivery.adapter.out.sender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lv.janis.notification_platform.delivery.application.exception.DeliveryNonRetryableException;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.tenant.domain.Tenant;
import org.springframework.test.util.ReflectionTestUtils;

import static lv.janis.notification_platform.support.EntityTestData.delivery;
import static lv.janis.notification_platform.support.EntityTestData.endpoint;
import static lv.janis.notification_platform.support.EntityTestData.event;
import static lv.janis.notification_platform.support.EntityTestData.subscription;
import static lv.janis.notification_platform.support.EntityTestData.tenant;

class WebhookSenderAdapterTest {
  private static final Instant NOW = Instant.parse("2026-02-07T10:00:00Z");

  private final RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
  private final RestTemplate restTemplate = mock(RestTemplate.class);
  private final WebhookSenderAdapter adapter = new WebhookSenderAdapter(builder, Clock.fixed(NOW, ZoneOffset.UTC));

  WebhookSenderAdapterTest() {
    when(builder.requestFactory(org.mockito.ArgumentMatchers.<Supplier<ClientHttpRequestFactory>>any())).thenReturn(builder);
    when(builder.build()).thenReturn(restTemplate);
  }

  @Test
  void sendBuildsExpectedRequestAndDelegatesToRestTemplate() {
    Delivery delivery = deliveryWithWebhookConfig(validWebhookConfig());
    when(restTemplate.exchange(eq("https://example.com/webhook"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
        .thenReturn(ResponseEntity.ok().build());

    adapter.send(delivery);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate).exchange(eq("https://example.com/webhook"), eq(HttpMethod.POST), entityCaptor.capture(), eq(Void.class));

    HttpEntity<Map<String, Object>> entity = entityCaptor.getValue();
    assertEquals("application/json", entity.getHeaders().getContentType().toString());
    assertEquals("token-1", entity.getHeaders().getFirst("Authorization"));
    assertEquals(delivery.getId().toString(), entity.getHeaders().getFirst("X-Delivery-Id"));
    assertEquals(delivery.getEventId().toString(), entity.getHeaders().getFirst("X-Delivery-Event-Id"));
    assertEquals(delivery.getTenantId().toString(), entity.getHeaders().getFirst("X-Tenant-Id"));
    assertEquals(delivery.getSubscriptionId().toString(), entity.getHeaders().getFirst("X-Subscription-Id"));
    assertEquals(delivery.getId().toString(), entity.getBody().get("deliveryId"));
    assertEquals("order.created", entity.getBody().get("eventType"));
    assertEquals(NOW.toString(), entity.getBody().get("attemptedAt"));
  }

  @Test
  void sendWrapsNonRetryableHttpStatus() {
    Delivery delivery = deliveryWithWebhookConfig(validWebhookConfig());
    HttpClientErrorException failure = HttpClientErrorException.create(
        HttpStatus.BAD_REQUEST,
        "Bad Request",
        org.springframework.http.HttpHeaders.EMPTY,
        new byte[0],
        StandardCharsets.UTF_8);
    when(restTemplate.exchange(eq("https://example.com/webhook"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
        .thenThrow(failure);

    DeliveryNonRetryableException ex = assertThrows(DeliveryNonRetryableException.class, () -> adapter.send(delivery));

    assertEquals("Webhook returned non-retryable HTTP 400 for delivery " + delivery.getId(), ex.getMessage());
  }

  @Test
  void sendRethrowsRetryableTransportException() {
    Delivery delivery = deliveryWithWebhookConfig(validWebhookConfig());
    ResourceAccessException failure = new ResourceAccessException("timeout");
    when(restTemplate.exchange(eq("https://example.com/webhook"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
        .thenThrow(failure);

    ResourceAccessException ex = assertThrows(ResourceAccessException.class, () -> adapter.send(delivery));

    assertEquals(failure, ex);
  }

  @Test
  void sendRejectsInvalidHeaderValue() {
    ObjectNode config = validWebhookConfig();
    config.set("headers", JsonNodeFactory.instance.objectNode().put("X-Retry", 1));
    Delivery delivery = deliveryWithWebhookConfig(config);

    DeliveryNonRetryableException ex = assertThrows(DeliveryNonRetryableException.class, () -> adapter.send(delivery));

    assertEquals("Webhook header must be text: X-Retry", ex.getMessage());
  }

  private static ObjectNode validWebhookConfig() {
    ObjectNode config = JsonNodeFactory.instance.objectNode();
    config.put("url", "https://example.com/webhook");
    config.put("connectTimeoutMs", 2500);
    config.put("responseTimeoutMs", 5000);
    config.put("connectionRequestTimeoutMs", 2000);
    config.set("headers", JsonNodeFactory.instance.objectNode().put("Authorization", "token-1"));
    return config;
  }

  private static Delivery deliveryWithWebhookConfig(ObjectNode config) {
    Tenant tenant = tenant(UUID.randomUUID());
    Endpoint endpoint = endpoint(UUID.randomUUID(), tenant, EndpointType.WEBHOOK, config, lv.janis.notification_platform.delivery.domain.EndpointStatus.ACTIVE);
    Event event = event(
        UUID.randomUUID(),
        tenant,
        "order.created",
        JsonNodeFactory.instance.objectNode().put("orderId", 123),
        lv.janis.notification_platform.ingest.domain.EventStatus.RECEIVED);
    Subscription subscription = subscription(UUID.randomUUID(), tenant, "order.created", endpoint);
    Delivery delivery = delivery(UUID.randomUUID(), tenant, event, subscription, endpoint);
    ReflectionTestUtils.setField(event, "receivedAt", Instant.parse("2026-02-07T09:00:00Z"));
    ReflectionTestUtils.setField(delivery, "createdAt", Instant.parse("2026-02-07T09:30:00Z"));
    return delivery;
  }
}
