package lv.janis.notification_platform.adminapi.application.port.in;

import lv.janis.notification_platform.tenant.domain.Tenant;

public interface EditTenantByIdUseCase {
  Tenant editTenantById(String tenantId, EditTenantCommand command);
}
