package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import lv.janis.notification_platform.adminapi.application.service.EmailTemplateDefinition;

public record EmailTemplateResponse(
    String name,
    String displayName,
    String description,
    String bodyType) {

  public static EmailTemplateResponse from(EmailTemplateDefinition definition) {
    return new EmailTemplateResponse(
        definition.name(),
        definition.displayName(),
        definition.description(),
        definition.bodyType());
  }
}
