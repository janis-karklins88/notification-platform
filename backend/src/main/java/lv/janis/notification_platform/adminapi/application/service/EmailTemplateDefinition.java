package lv.janis.notification_platform.adminapi.application.service;

public record EmailTemplateDefinition(
    String name,
    String displayName,
    String description,
    String bodyType) {
}
