package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

public record CreateTenantRequest(
    @NotBlank @Size(max = 64) String slug,
    @NotBlank @Size(max = 200) String name,
    TenantStatus status
) {
}
