package lv.janis.notification_platform.tenant.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import lv.janis.notification_platform.config.JpaAuditingConfig;
import lv.janis.notification_platform.tenant.application.port.out.TenantFilter;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

@DataJpaTest
@Import({JpaAuditingConfig.class, TenantRepositoryAdapterIntegrationTest.Config.class})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:notification_platform_tenant_repo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class TenantRepositoryAdapterIntegrationTest {

  @Autowired
  private TenantRepositoryAdapter tenantRepositoryAdapter;

  @Test
  void findBySlugAndExistsBySlugWorkForStoredTenant() {
    Tenant saved = tenantRepositoryAdapter.save(new Tenant("tenant-slug", "Tenant Slug", TenantStatus.ACTIVE));

    Optional<Tenant> found = tenantRepositoryAdapter.findBySlug("tenant-slug");

    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
    assertTrue(tenantRepositoryAdapter.existsBySlug("tenant-slug"));
  }

  @Test
  void findAllSupportsCombinedFilters() {
    Tenant kept = tenantRepositoryAdapter.save(new Tenant("tenant-kept", "Acme Corp", TenantStatus.ACTIVE));
    tenantRepositoryAdapter.save(new Tenant("tenant-paused", "Acme Archive", TenantStatus.SUSPENDED));
    tenantRepositoryAdapter.save(new Tenant("tenant-other", "Beta Group", TenantStatus.ACTIVE));

    TenantFilter filter = new TenantFilter(TenantStatus.ACTIVE, "acme", null, null);

    var result = tenantRepositoryAdapter.findAll(filter, PageRequest.of(0, 50));

    assertEquals(1, result.getTotalElements());
    assertEquals(kept.getId(), result.getContent().get(0).getId());
  }

  @Test
  void findAllSupportsTimeRangeFilter() throws InterruptedException {
    Tenant first = tenantRepositoryAdapter.save(new Tenant("tenant-first", "Tenant First", TenantStatus.ACTIVE));
    Thread.sleep(700);
    Tenant second = tenantRepositoryAdapter.save(new Tenant("tenant-second", "Tenant Second", TenantStatus.ACTIVE));

    Instant from = second.getCreatedAt().minusMillis(1);
    Instant to = second.getCreatedAt().plusMillis(1);

    var result = tenantRepositoryAdapter.findAll(
        new TenantFilter(null, null, from, to),
        PageRequest.of(0, 50));

    assertEquals(1, result.getTotalElements());
    assertEquals(second.getId(), result.getContent().get(0).getId());
    assertTrue(first.getCreatedAt().isBefore(from));
  }

  @TestConfiguration
  static class Config {
    @Bean
    TenantRepositoryAdapter tenantRepositoryAdapter(TenantJpaRepository tenantJpaRepository) {
      return new TenantRepositoryAdapter(tenantJpaRepository);
    }
  }
}
