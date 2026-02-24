package lv.janis.notification_platform.adminapi.application.port.in;

import lv.janis.notification_platform.tenant.domain.Tenant;

public interface CreateTenantUseCase {
  Tenant createTenant(CreateTenantCommand command);
}
