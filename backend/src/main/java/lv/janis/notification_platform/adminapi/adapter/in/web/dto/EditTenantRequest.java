package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import jakarta.validation.constraints.Size;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

public record EditTenantRequest(
    @Size(max = 200) String name,
    TenantStatus status) {
}
