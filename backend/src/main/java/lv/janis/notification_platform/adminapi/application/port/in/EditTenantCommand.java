package lv.janis.notification_platform.adminapi.application.port.in;

import java.util.UUID;

import lv.janis.notification_platform.tenant.domain.TenantStatus;

public record EditTenantCommand(UUID id, String name, TenantStatus status) {
}
