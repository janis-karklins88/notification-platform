package lv.janis.notification_platform.adminapi.application.port.in;

import java.time.Instant;

import lv.janis.notification_platform.tenant.domain.TenantStatus;

public record ListTenantsQuery(
    int page,
    int size,
    TenantStatus status,
    String nameContains,
    Instant createdFrom,
    Instant createdTo) {

}
