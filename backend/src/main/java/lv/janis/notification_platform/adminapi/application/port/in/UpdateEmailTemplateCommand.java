package lv.janis.notification_platform.adminapi.application.port.in;

import java.util.UUID;

public record UpdateEmailTemplateCommand(
    UUID templateId,
    String name,
    String subject,
    String body,
    boolean html,
    String description) {
}
