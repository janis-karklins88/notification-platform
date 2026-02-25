package lv.janis.notification_platform.adminapi.application.service;

import org.springframework.stereotype.Service;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.EditTenantByIdUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.EditTenantCommand;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;

@Service
public class EditTenantByIdService implements EditTenantByIdUseCase {
  private final TenantRepositoryPort tenantRepositoryPort;

  public EditTenantByIdService(TenantRepositoryPort tenantRepositoryPort) {
    this.tenantRepositoryPort = tenantRepositoryPort;
  }

  @Override
  public Tenant editTenantById(EditTenantCommand command) {
    var tenant = tenantRepositoryPort.findById(command.id())
        .orElseThrow(() -> NotFoundException.of("Tenant", command.id()));

    if (command.name() == null && command.status() == null) {
      throw new BadRequestException("Name or Status must be provided!");
    }

    if (command.name() != null) {
      tenant.rename(command.name());
    }
    if (command.status() != null) {
      tenant.changeStatus(command.status());
    }

    tenantRepositoryPort.save(tenant);
    return tenant;
  }
}
