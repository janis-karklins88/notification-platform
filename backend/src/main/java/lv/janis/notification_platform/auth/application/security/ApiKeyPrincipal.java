package lv.janis.notification_platform.auth.application.security;

import java.util.UUID;

public record ApiKeyPrincipal(UUID apiKeyId, UUID tenantId) {
}
