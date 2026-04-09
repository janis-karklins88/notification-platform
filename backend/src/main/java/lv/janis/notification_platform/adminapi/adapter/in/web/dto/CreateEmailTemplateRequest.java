package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEmailTemplateRequest(
    @NotBlank String name,
    @NotBlank String subject,
    @NotBlank String body,
    @NotNull Boolean html,
    String description) {
}
