package lv.janis.notification_platform.adminapi.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lv.janis.notification_platform.adminapi.application.port.in.ListTenantUseCase;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;

@Service
public class ListTenantsService implements ListTenantUseCase {
  private static final int MAX_PAGE_SIZE = 100;

  private final TenantRepositoryPort tenantRepositoryPort;

  public ListTenantsService(TenantRepositoryPort tenantRepositoryPort) {
    this.tenantRepositoryPort = tenantRepositoryPort;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Tenant> listTenants(int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    return tenantRepositoryPort.findAll(pageable);
  }
}
