package lv.janis.notification_platform.tenant.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;

@Repository
public class TenantRepositoryAdapter implements TenantRepositoryPort {
  private final TenantJpaRepository tenantJpaRepository;

  public TenantRepositoryAdapter(TenantJpaRepository tenantJpaRepository) {
    this.tenantJpaRepository = tenantJpaRepository;
  }

  @Override
  public Tenant save(Tenant tenant) {
    return tenantJpaRepository.save(tenant);
  }

  @Override
  public Optional<Tenant> findById(UUID id) {
    return tenantJpaRepository.findById(id);
  }

  @Override
  public Optional<Tenant> findBySlug(String slug) {
    return tenantJpaRepository.findBySlug(slug);
  }

  @Override
  public boolean existsBySlug(String slug) {
    return tenantJpaRepository.existsBySlug(slug);
  }

  @Override
  public Page<Tenant> findAll(Pageable pageable) {
    return tenantJpaRepository.findAll(pageable);
  }
}
