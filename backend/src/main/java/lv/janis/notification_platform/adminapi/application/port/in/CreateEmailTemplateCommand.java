package lv.janis.notification_platform.adminapi.application.port.in;

import java.util.UUID;

public record CreateEmailTemplateCommand(
    UUID tenantId,
    String name,
    String subject,
    String body,
    boolean html,
    String description) {
}
