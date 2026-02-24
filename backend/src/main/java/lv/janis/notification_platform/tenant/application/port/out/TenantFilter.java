package lv.janis.notification_platform.tenant.application.port.out;

import java.time.Instant;

import lv.janis.notification_platform.tenant.domain.TenantStatus;

public record TenantFilter(
    TenantStatus status,
    String nameContains,
    Instant createdFrom,
    Instant createdTo) {
}
