package lv.janis.notification_platform.delivery.application.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateRenderer {
  private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*\\}\\}");

  private final TemplateEngine templateEngine;

  public EmailTemplateRenderer(TemplateEngine templateEngine) {
    this.templateEngine = Objects.requireNonNull(templateEngine);
  }

  public String renderTemplateByName(String templateName, Map<String, Object> model) {
    if (templateName == null || templateName.isBlank()) {
      return "";
    }
    Context context = new Context(Locale.ROOT, model);
    return templateEngine.process(templateName, context);
  }

  public String renderInlineTemplate(String template, Map<String, Object> context) {
    if (template == null || template.isBlank()) {
      return "";
    }

    Matcher matcher = TEMPLATE_TOKEN.matcher(template);
    StringBuffer rendered = new StringBuffer();
    while (matcher.find()) {
      String expression = matcher.group(1);
      Object value = resolveExpression(context, expression);
      matcher.appendReplacement(rendered, Matcher.quoteReplacement(asString(value)));
    }
    matcher.appendTail(rendered);
    return rendered.toString();
  }

  private static Object resolveExpression(Map<String, Object> context, String expression) {
    Object current = context.get(expression);
    if (current != null) {
      return current;
    }

    if (expression == null || expression.isBlank()) {
      return "";
    }
    if ("payload".equals(expression)) {
      Object payload = context.get("payload");
      return payload == null ? "" : payload;
    }

    if (expression.startsWith("payload.")) {
      Object payload = context.get("payload");
      return resolvePath(payload, expression.substring("payload.".length()));
    }

    return resolvePath(context, expression);
  }

  private static Object resolvePath(Object root, String path) {
    if (root == null || path == null || path.isBlank()) {
      return "";
    }

    Object current = root;
    for (String part : path.split("\\.")) {
      if (current == null) {
        return "";
      }

      String arrayField = part;
      Integer arrayIndex = null;

      int bracketStart = part.indexOf('[');
      int bracketEnd = part.indexOf(']');
      if (bracketStart > 0 && bracketEnd > bracketStart) {
        String field = part.substring(0, bracketStart);
        String indexText = part.substring(bracketStart + 1, bracketEnd);
        arrayField = field;
        try {
          arrayIndex = Integer.parseInt(indexText);
        } catch (NumberFormatException ex) {
          return "";
        }
      }

      if (current instanceof Map<?, ?> map) {
        current = map.get(arrayField);
      } else if (current instanceof List<?> list) {
        if (arrayIndex == null || arrayIndex < 0 || arrayIndex >= list.size()) {
          return "";
        }
        current = list.get(arrayIndex);
      } else {
        return "";
      }

      if (arrayIndex != null) {
        if (!(current instanceof List<?> list)) {
          return "";
        }
        if (arrayIndex < 0 || arrayIndex >= list.size()) {
          return "";
        }
        current = list.get(arrayIndex);
      }
    }

    return current;
  }

  private static String asString(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof Map<?, ?> map) {
      return map.toString();
    }
    if (value instanceof List<?> list) {
      return list.toString();
    }
    return String.valueOf(value);
  }
}
