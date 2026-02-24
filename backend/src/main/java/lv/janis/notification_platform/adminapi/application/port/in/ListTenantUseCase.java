package lv.janis.notification_platform.adminapi.application.port.in;

import org.springframework.data.domain.Page;

import lv.janis.notification_platform.tenant.domain.Tenant;

public interface ListTenantUseCase {
  Page<Tenant> listTenants(ListTenantsQuery query);
}
