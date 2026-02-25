package lv.janis.notification_platform.adminapi.application.port.in;

import lv.janis.notification_platform.tenant.domain.TenantStatus;

public record EditTenantCommand(String name, TenantStatus status) {
}
