package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

public record TenantResponse(
    UUID id,
    String slug,
    String name,
    TenantStatus status,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {
  public static TenantResponse from(Tenant tenant) {
    return new TenantResponse(
        tenant.getId(),
        tenant.getSlug(),
        tenant.getName(),
        tenant.getStatus(),
        tenant.getVersion(),
        tenant.getCreatedAt(),
        tenant.getUpdatedAt()
    );
  }
}
