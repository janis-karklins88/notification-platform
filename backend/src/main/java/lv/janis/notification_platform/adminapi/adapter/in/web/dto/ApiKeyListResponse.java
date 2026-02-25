package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.auth.domain.ApiKey;
import lv.janis.notification_platform.auth.domain.ApiKeyStatus;

public record ApiKeyListResponse(
        UUID id,
        String keyPrefix,
        ApiKeyStatus status,
        Instant createdAt,
        Instant revokedAt,
        Instant lastUsedAt) {
    public static ApiKeyListResponse from(ApiKey apiKey) {
        return new ApiKeyListResponse(
                apiKey.getId(),
                apiKey.getKeyPrefix(),
                apiKey.getStatus(),
                apiKey.getCreatedAt(),
                apiKey.getRevokedAt(),
                apiKey.getLastUsedAt());
    }
}
