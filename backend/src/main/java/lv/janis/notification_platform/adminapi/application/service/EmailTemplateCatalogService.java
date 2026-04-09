package lv.janis.notification_platform.adminapi.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lv.janis.notification_platform.adminapi.application.port.in.EmailTemplateUseCase;

@Service
public class EmailTemplateCatalogService implements EmailTemplateUseCase {
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

  @Override
  public List<EmailTemplateDefinition> listTemplates() {
    return EMAIL_TEMPLATES;
  }
}
