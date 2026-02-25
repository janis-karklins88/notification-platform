package lv.janis.notification_platform.adminapi.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.GetTenantByIdUseCase;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;

@Service
public class GetTenantByIdService implements GetTenantByIdUseCase {
  private final TenantRepositoryPort tenantRepositoryPort;

  public GetTenantByIdService(TenantRepositoryPort tenantRepositoryPort) {
    this.tenantRepositoryPort = tenantRepositoryPort;
  }

  @Override
  public Tenant getTenantById(UUID tenantId) {
    var tenant = tenantRepositoryPort.findById(tenantId)
        .orElseThrow(() -> NotFoundException.of("Tenant", tenantId));
    return tenant;
  }

}
