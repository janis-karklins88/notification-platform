package lv.janis.notification_platform.tenant.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import lv.janis.notification_platform.tenant.domain.Tenant;

public interface TenantJpaRepository extends JpaRepository<Tenant, UUID>, JpaSpecificationExecutor<Tenant> {
  Optional<Tenant> findBySlug(String slug);

  boolean existsBySlug(String slug);
}
