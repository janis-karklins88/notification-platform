package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.delivery.domain.EmailTemplate;

public record EmailTemplateResponse(
    UUID id,
    UUID tenantId,
    String name,
    String subject,
    String body,
    boolean html,
    String description,
    boolean active,
    Instant createdAt) {

  public static EmailTemplateResponse from(EmailTemplate template) {
    return new EmailTemplateResponse(
        template.getId(),
        template.getTenantId(),
        template.getName(),
        template.getSubject(),
        template.getBody(),
        template.isHtml(),
        template.getDescription(),
        template.isActive(),
        template.getCreatedAt());
  }
}
