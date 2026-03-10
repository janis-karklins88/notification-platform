package lv.janis.notification_platform.delivery.application.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lv.janis.notification_platform.delivery.application.exception.DeliveryNonRetryableException;
import lv.janis.notification_platform.delivery.application.model.PreparedEmailMessage;
import lv.janis.notification_platform.delivery.domain.Delivery;

@Component
public class EmailMessageFactory {
  private static final String DEFAULT_SUBJECT_TEMPLATE = "Notification: {{eventType}}";
  private static final String DEFAULT_BODY_TEMPLATE = """
      Delivery {{deliveryId}} for event "{{eventType}}" was created.

      Event ID: {{eventId}}
      Tenant ID: {{tenantId}}
      Subscription ID: {{subscriptionId}}
      Occurred at: {{occurredAt}}
      Created at: {{createdAt}}

      Payload:
      {{payload}}
      """;

  private static final String SUBJECT_TEMPLATE_KEY = "subjectTemplate";
  private static final String BODY_TEMPLATE_KEY = "bodyTemplate";
  private static final String BODY_TYPE_KEY = "bodyType";
  private static final String TEMPLATE_NAME_KEY = "templateName";
  private static final String RECIPIENTS_KEY = "recipients";
  private static final String FROM_KEY = "from";
  private static final String REPLY_TO_KEY = "replyTo";
  private static final String BODY_TYPE_TEXT = "text";
  private static final String BODY_TYPE_HTML = "html";

  private final Clock clock;
  private final ObjectMapper objectMapper;
  private final EmailTemplateRenderer templateRenderer;
  private final String defaultFrom;

  public EmailMessageFactory(Clock clock, ObjectMapper objectMapper, EmailTemplateRenderer templateRenderer,
      @Value("${notification.email.from:}") String defaultFrom) {
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    this.templateRenderer = Objects.requireNonNull(templateRenderer, "templateRenderer must not be null");
    this.defaultFrom = defaultFrom == null ? "" : defaultFrom.strip();
  }

  public PreparedEmailMessage build(Delivery delivery) {
    JsonNode config = delivery.getEndpoint().getConfig();
    List<String> recipients = resolveRecipients(config);
    if (recipients.isEmpty()) {
      throw new DeliveryNonRetryableException("EMAIL config has no recipients");
    }

    Map<String, Object> context = buildTemplateContext(delivery);
    String templateName = readText(config, TEMPLATE_NAME_KEY).orElse("");
    boolean html = isHtml(config, !templateName.isBlank());
    String subjectTemplate = readText(config, SUBJECT_TEMPLATE_KEY).orElse(DEFAULT_SUBJECT_TEMPLATE);
    String body;
    String subject;
    String bodyTemplate = readText(config, BODY_TEMPLATE_KEY).orElse(DEFAULT_BODY_TEMPLATE);

    try {
      if (templateName.isBlank()) {
        body = templateRenderer.renderInlineTemplate(bodyTemplate, context);
      } else {
        body = templateRenderer.renderTemplateByName(templateName, context);
      }
      subject = templateRenderer.renderInlineTemplate(subjectTemplate, context);
    } catch (RuntimeException ex) {
      throw new DeliveryNonRetryableException("Failed to render email message template", ex);
    }

    return new PreparedEmailMessage(
        recipients,
        readText(config, FROM_KEY).orElse(defaultFrom),
        readText(config, REPLY_TO_KEY).orElse(""),
        subject,
        body,
        html);
  }

  private boolean isHtml(JsonNode config, boolean useTemplateDefault) {
    String bodyType = readText(config, BODY_TYPE_KEY)
        .orElse(useTemplateDefault ? BODY_TYPE_HTML : BODY_TYPE_TEXT)
        .toLowerCase()
        .strip();
    return BODY_TYPE_HTML.equals(bodyType);
  }

  private static List<String> resolveRecipients(JsonNode config) {
    JsonNode recipientsNode = config != null ? config.get(RECIPIENTS_KEY) : null;
    if (recipientsNode == null || !recipientsNode.isArray()) {
      return List.of();
    }

    List<String> recipients = new ArrayList<>();
    for (JsonNode recipientNode : recipientsNode) {
      if (recipientNode != null && recipientNode.isTextual()) {
        String recipient = recipientNode.asText().strip();
        if (!recipient.isBlank()) {
          recipients.add(recipient);
        }
      }
    }
    return recipients;
  }

  private static Optional<String> readText(JsonNode config, String key) {
    if (config == null || !config.hasNonNull(key)) {
      return Optional.empty();
    }
    JsonNode node = config.get(key);
    if (!node.isTextual()) {
      return Optional.empty();
    }
    String value = node.asText().strip();
    return value.isBlank() ? Optional.empty() : Optional.of(value);
  }

  private Map<String, Object> buildTemplateContext(Delivery delivery) {
    JsonNode payloadNode = delivery.getEvent().getPayload();
    Map<String, Object> payloadMap = toPayloadMap(payloadNode);

    Map<String, Object> context = new HashMap<>();
    context.put("deliveryId", String.valueOf(delivery.getId()));
    context.put("eventId", String.valueOf(delivery.getEventId()));
    context.put("eventType", delivery.getEvent().getEventType());
    context.put("tenantId", String.valueOf(delivery.getTenantId()));
    context.put("subscriptionId", String.valueOf(delivery.getSubscriptionId()));
    context.put("occurredAt", toText(delivery.getEvent().getReceivedAt()));
    context.put("createdAt", toText(delivery.getCreatedAt()));
    context.put("timestamp", toText(clock.instant()));
    context.put("payload", payloadMap);
    context.put("payloadJson", payloadNode == null ? "" : payloadNode.toString());
    return context;
  }

  private static String toText(java.time.Instant instant) {
    return instant == null ? "" : instant.toString();
  }

  private Map<String, Object> toPayloadMap(JsonNode payloadNode) {
    if (payloadNode == null || payloadNode.isNull()) {
      return Map.of();
    }
    if (!payloadNode.isObject()) {
      return Map.of("value", payloadNode.toString());
    }

    try {
      return objectMapper.convertValue(payloadNode, new TypeReference<Map<String, Object>>() {});
    } catch (IllegalArgumentException ex) {
      return Map.of("raw", payloadNode.toString());
    }
  }
}
