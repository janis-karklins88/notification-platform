package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.adminapi.application.port.in.CreateApiKeyResult;
import lv.janis.notification_platform.auth.domain.ApiKeyStatus;

public record CreateApiKeyResponse(
        UUID id,
        UUID tenantId,
        String keyPrefix,
        String plaintextKey,
        ApiKeyStatus status,
        Instant createdAt) {
    public static CreateApiKeyResponse from(CreateApiKeyResult result) {
        return new CreateApiKeyResponse(
                result.id(),
                result.tenantId(),
                result.keyPrefix(),
                result.plaintextKey(),
                result.status(),
                result.createdAt());
    }
}
