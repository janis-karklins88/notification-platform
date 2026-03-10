package lv.janis.notification_platform.delivery.application.model;

import java.util.List;

public record PreparedEmailMessage(
    List<String> recipients,
    String from,
    String replyTo,
    String subject,
    String body,
    boolean html) {
}
