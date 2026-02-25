package lv.janis.notification_platform.auth.application.port.out;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.auth.domain.ApiKeyStatus;

public record ListApiKeyQuery(
        int page,
        int size,
        UUID tenantId,
        ApiKeyStatus status,
        String prefix,
        Instant createdFrom,
        Instant createdTo) {

}
