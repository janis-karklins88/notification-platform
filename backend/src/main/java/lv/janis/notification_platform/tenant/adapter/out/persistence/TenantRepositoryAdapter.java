package lv.janis.notification_platform.tenant.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import lv.janis.notification_platform.tenant.application.port.out.TenantFilter;
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
  public Page<Tenant> findAll(TenantFilter filter, Pageable pageable) {
    Specification<Tenant> spec = (root, query, cb) -> cb.conjunction();

    if (filter.status() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), filter.status()));
    }
    if (StringUtils.hasText(filter.nameContains())) {
      String pattern = "%" + filter.nameContains().toLowerCase() + "%";
      spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), pattern));
    }
    if (filter.createdFrom() != null) {
      spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), filter.createdFrom()));
    }
    if (filter.createdTo() != null) {
      spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), filter.createdTo()));
    }

    return tenantJpaRepository.findAll(spec, pageable);
  }

}
