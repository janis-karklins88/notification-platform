package lv.janis.notification_platform.delivery.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lv.janis.notification_platform.delivery.application.exception.DeliveryNonRetryableException;
import lv.janis.notification_platform.delivery.application.model.PreparedEmailMessage;
import lv.janis.notification_platform.delivery.application.port.out.EmailTemplateRepositoryPort;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.EmailTemplate;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.tenant.domain.Tenant;

import static lv.janis.notification_platform.support.EntityTestData.delivery;
import static lv.janis.notification_platform.support.EntityTestData.emailTemplate;
import static lv.janis.notification_platform.support.EntityTestData.endpoint;
import static lv.janis.notification_platform.support.EntityTestData.event;
import static lv.janis.notification_platform.support.EntityTestData.subscription;
import static lv.janis.notification_platform.support.EntityTestData.tenant;

class EmailMessageFactoryTest {
  private static final Instant NOW = Instant.parse("2026-02-05T12:00:00Z");

  private final EmailTemplateRenderer renderer = mock(EmailTemplateRenderer.class);
  private final EmailTemplateRepositoryPort emailTemplateRepository = mock(EmailTemplateRepositoryPort.class);
  private final EmailMessageFactory factory = new EmailMessageFactory(
      Clock.fixed(NOW, ZoneOffset.UTC),
      new ObjectMapper(),
      emailTemplateRepository,
      renderer,
      "default@example.com");

  @Test
  void buildUsesInlineTemplatesAndDefaultFromAddress() {
    ObjectNode config = emailConfig();
    config.putArray("recipients")
        .add(" first@example.com ")
        .add("   ")
        .add("second@example.com");
    config.put("subjectTemplate", "Subject {{eventType}}");
    config.put("bodyTemplate", "Body {{payload.orderId}}");
    config.put("bodyType", "text");
    Delivery delivery = deliveryWithConfig(config);
    when(renderer.renderInlineTemplate(eq("Body {{payload.orderId}}"), anyMap())).thenReturn("Body 123");
    when(renderer.renderInlineTemplate(eq("Subject {{eventType}}"), anyMap())).thenReturn("Subject order.created");

    PreparedEmailMessage message = factory.build(delivery);

    assertEquals(java.util.List.of("first@example.com", "second@example.com"), message.recipients());
    assertEquals("default@example.com", message.from());
    assertEquals("", message.replyTo());
    assertEquals("Subject order.created", message.subject());
    assertEquals("Body 123", message.body());
    assertFalse(message.html());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
    verify(renderer).renderInlineTemplate(eq("Body {{payload.orderId}}"), contextCaptor.capture());
    Map<String, Object> context = contextCaptor.getValue();
    assertEquals(delivery.getId().toString(), context.get("deliveryId"));
    assertEquals(delivery.getEventId().toString(), context.get("eventId"));
    assertEquals("order.created", context.get("eventType"));
    assertEquals(delivery.getTenantId().toString(), context.get("tenantId"));
    assertEquals(NOW.toString(), context.get("timestamp"));
    assertEquals("123", String.valueOf(((Map<?, ?>) context.get("payload")).get("orderId")));
  }

  @Test
  void buildUsesDatabaseTemplateWhenTemplateNamePresent() {
    ObjectNode config = emailConfig();
    config.putArray("recipients").add("to@example.com");
    config.put("templateName", "welcome");
    config.put("from", "sender@example.com");
    config.put("replyTo", "reply@example.com");
    Delivery delivery = deliveryWithConfig(config);
    EmailTemplate template = emailTemplate(
        UUID.randomUUID(),
        delivery.getTenant(),
        "welcome",
        "Hello {{eventType}}",
        "<p>Hello {{payload.orderId}}</p>",
        true,
        "welcome template",
        true);
    when(emailTemplateRepository.findByTenantIdAndName(delivery.getTenantId(), "welcome"))
        .thenReturn(Optional.of(template));
    when(renderer.renderInlineTemplate(eq("Hello {{eventType}}"), anyMap())).thenReturn("Hello order.created");
    when(renderer.renderInlineTemplate(eq("<p>Hello {{payload.orderId}}</p>"), anyMap()))
        .thenReturn("<p>Hello 123</p>");

    PreparedEmailMessage message = factory.build(delivery);

    assertEquals("sender@example.com", message.from());
    assertEquals("reply@example.com", message.replyTo());
    assertEquals("Hello order.created", message.subject());
    assertEquals("<p>Hello 123</p>", message.body());
    assertTrue(message.html());
    verify(renderer, never()).renderTemplateByName(eq("welcome"), anyMap());
  }

  @Test
  void buildRejectsMissingDatabaseTemplateWhenTemplateNamePresent() {
    ObjectNode config = emailConfig();
    config.putArray("recipients").add("to@example.com");
    config.put("templateName", "welcome");
    Delivery delivery = deliveryWithConfig(config);
    when(emailTemplateRepository.findByTenantIdAndName(delivery.getTenantId(), "welcome"))
        .thenReturn(Optional.empty());

    DeliveryNonRetryableException ex = assertThrows(DeliveryNonRetryableException.class, () -> factory.build(delivery));

    assertEquals(
        "EMAIL template 'welcome' not found for tenant " + delivery.getTenantId(),
        ex.getMessage());
  }

  @Test
  void buildRejectsMissingRecipients() {
    Delivery delivery = deliveryWithConfig(emailConfig());

    DeliveryNonRetryableException ex = assertThrows(DeliveryNonRetryableException.class, () -> factory.build(delivery));

    assertEquals("EMAIL config has no recipients", ex.getMessage());
  }

  @Test
  void buildWrapsTemplateRendererFailure() {
    ObjectNode config = emailConfig();
    config.putArray("recipients").add("to@example.com");
    config.put("bodyTemplate", "Body");
    config.put("subjectTemplate", "Subject");
    Delivery delivery = deliveryWithConfig(config);
    when(renderer.renderInlineTemplate(eq("Body"), anyMap())).thenThrow(new RuntimeException("boom"));

    DeliveryNonRetryableException ex = assertThrows(DeliveryNonRetryableException.class, () -> factory.build(delivery));

    assertEquals("Failed to render email message template", ex.getMessage());
  }

  private static ObjectNode emailConfig() {
    return JsonNodeFactory.instance.objectNode();
  }

  private static Delivery deliveryWithConfig(ObjectNode config) {
    Tenant tenant = tenant(UUID.randomUUID());
    Endpoint endpoint = endpoint(UUID.randomUUID(), tenant, EndpointType.EMAIL, config, lv.janis.notification_platform.delivery.domain.EndpointStatus.ACTIVE);
    Event event = event(
        UUID.randomUUID(),
        tenant,
        "order.created",
        JsonNodeFactory.instance.objectNode().put("orderId", 123),
        lv.janis.notification_platform.ingest.domain.EventStatus.RECEIVED);
    Subscription subscription = subscription(UUID.randomUUID(), tenant, "order.created", endpoint);
    Delivery delivery = delivery(UUID.randomUUID(), tenant, event, subscription, endpoint);
    ReflectionTestUtils.setField(event, "receivedAt", Instant.parse("2026-02-05T11:00:00Z"));
    ReflectionTestUtils.setField(delivery, "createdAt", Instant.parse("2026-02-05T11:30:00Z"));
    return delivery;
  }
}
