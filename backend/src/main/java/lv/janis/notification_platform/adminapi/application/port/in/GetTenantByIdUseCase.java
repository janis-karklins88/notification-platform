package lv.janis.notification_platform.adminapi.application.port.in;

import java.util.UUID;

import lv.janis.notification_platform.tenant.domain.Tenant;

public interface GetTenantByIdUseCase {
  Tenant getTenantById(UUID tenantId);
}
