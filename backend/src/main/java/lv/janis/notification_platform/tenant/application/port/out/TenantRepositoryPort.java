package lv.janis.notification_platform.tenant.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lv.janis.notification_platform.tenant.domain.Tenant;

public interface TenantRepositoryPort {
  Tenant save(Tenant tenant);

  Optional<Tenant> findById(UUID id);

  Optional<Tenant> findBySlug(String slug);

  boolean existsBySlug(String slug);

  Page<Tenant> findAll(TenantFilter filter, Pageable pageable);

}
