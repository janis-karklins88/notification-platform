package lv.janis.notification_platform.adminapi.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateCatalogService {
  private static final List<EmailTemplateDefinition> EMAIL_TEMPLATES = List.of(
      new EmailTemplateDefinition(
          "email/order-created",
          "Order Created",
          "HTML email for a newly created order with customer, totals, and line items.",
          "html"),
      new EmailTemplateDefinition(
          "email/generic-notification",
          "Generic Notification",
          "Simple text email for arbitrary event notifications.",
          "text"));

  public List<EmailTemplateDefinition> listTemplates() {
    return EMAIL_TEMPLATES;
  }
}
