package lv.janis.notification_platform.adminapi.application.port.in;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.auth.domain.ApiKey;
import lv.janis.notification_platform.auth.domain.ApiKeyStatus;

public record CreateApiKeyResult(
        UUID id,
        UUID tenantId,
        String keyPrefix,
        String plaintextKey,
        ApiKeyStatus status,
        Instant createdAt) {
    public static CreateApiKeyResult from(ApiKey apiKey, String plaintextKey) {
        return new CreateApiKeyResult(
                apiKey.getId(),
                apiKey.getTenant().getId(),
                apiKey.getKeyPrefix(),
                plaintextKey,
                apiKey.getStatus(),
                apiKey.getCreatedAt());
    }
}
