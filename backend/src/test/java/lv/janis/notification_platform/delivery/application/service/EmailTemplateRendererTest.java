package lv.janis.notification_platform.delivery.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

class EmailTemplateRendererTest {
  private final TemplateEngine templateEngine = mock(TemplateEngine.class);
  private final EmailTemplateRenderer renderer = new EmailTemplateRenderer(templateEngine);

  @Test
  void renderTemplateByNameDelegatesToTemplateEngine() {
    Map<String, Object> model = Map.of("name", "Janis");
    when(templateEngine.process(eq("welcome"), any(Context.class))).thenReturn("Hello Janis");

    String rendered = renderer.renderTemplateByName("welcome", model);

    assertEquals("Hello Janis", rendered);
    verify(templateEngine).process(eq("welcome"), any(Context.class));
  }

  @Test
  void renderTemplateByNameReturnsEmptyForBlankTemplateName() {
    assertEquals("", renderer.renderTemplateByName("   ", Map.of()));
  }

  @Test
  void renderInlineTemplateResolvesSimpleNestedAndIndexedExpressions() {
    Map<String, Object> context = Map.of(
        "name", "Janis",
        "payload", Map.of(
            "order", Map.of("id", 42),
            "items", List.of(Map.of("name", "first"), Map.of("name", "second"))));

    String rendered = renderer.renderInlineTemplate(
        "Hello {{ name }}, order={{payload.order.id}}, item={{payload.items[1].name}}, missing={{missing}}",
        context);

    assertEquals("Hello Janis, order=42, item=second, missing=", rendered);
  }

  @Test
  void renderInlineTemplateReturnsWholePayloadStringForPayloadToken() {
    Map<String, Object> context = Map.of("payload", Map.of("orderId", 123));

    String rendered = renderer.renderInlineTemplate("Payload={{payload}}", context);

    assertEquals("Payload={orderId=123}", rendered);
  }
}
