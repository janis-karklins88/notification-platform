package lv.janis.notification_platform.auth.adapter.out.persistence;

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

import lv.janis.notification_platform.auth.application.port.out.ListApiKeyQuery;
import lv.janis.notification_platform.auth.domain.ApiKey;
import lv.janis.notification_platform.auth.domain.ApiKeyStatus;
import lv.janis.notification_platform.config.JpaAuditingConfig;
import lv.janis.notification_platform.tenant.adapter.out.persistence.TenantJpaRepository;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

@DataJpaTest
@Import({JpaAuditingConfig.class, ApiKeyRepositoryAdapterIntegrationTest.Config.class})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:notification_platform_api_key_repo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ApiKeyRepositoryAdapterIntegrationTest {

  @Autowired
  private ApiKeyRepositoryAdapter apiKeyRepositoryAdapter;

  @Autowired
  private TenantJpaRepository tenantRepository;

  @Test
  void findByKeyHashReturnsStoredApiKey() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-api-key", "Tenant Api Key", TenantStatus.ACTIVE));
    ApiKey saved = apiKeyRepositoryAdapter.save(new ApiKey(tenant, "prefix01", "hash-1"));

    Optional<ApiKey> found = apiKeyRepositoryAdapter.findByKeyHash("hash-1");

    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
  }

  @Test
  void findAllSupportsCombinedFilters() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-api-filter", "Tenant Api Filter", TenantStatus.ACTIVE));
    Tenant otherTenant = tenantRepository.save(new Tenant("tenant-api-other", "Tenant Api Other", TenantStatus.ACTIVE));

    ApiKey kept = apiKeyRepositoryAdapter.save(new ApiKey(tenant, "prefix-kept", "hash-kept"));
    ApiKey revoked = apiKeyRepositoryAdapter.save(new ApiKey(tenant, "prefix-revoked", "hash-revoked"));
    revoked.revoke(Instant.parse("2026-01-01T00:00:00Z"));
    apiKeyRepositoryAdapter.save(revoked);
    apiKeyRepositoryAdapter.save(new ApiKey(otherTenant, "prefix-other", "hash-other"));

    var query = new ListApiKeyQuery(
        0,
        50,
        tenant.getId(),
        ApiKeyStatus.ACTIVE,
        "prefix-k",
        null,
        null);

    var result = apiKeyRepositoryAdapter.findAll(query, PageRequest.of(0, 50));

    assertEquals(1, result.getTotalElements());
    assertEquals(kept.getId(), result.getContent().get(0).getId());
  }

  @Test
  void findAllSupportsTimeRangeFilter() throws InterruptedException {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-api-time", "Tenant Api Time", TenantStatus.ACTIVE));
    ApiKey first = apiKeyRepositoryAdapter.save(new ApiKey(tenant, "prefix-first", "hash-first"));
    Thread.sleep(700);
    ApiKey second = apiKeyRepositoryAdapter.save(new ApiKey(tenant, "prefix-second", "hash-second"));

    Instant from = second.getCreatedAt().minusMillis(1);
    Instant to = second.getCreatedAt().plusMillis(1);

    var query = new ListApiKeyQuery(
        0,
        50,
        tenant.getId(),
        ApiKeyStatus.ACTIVE,
        null,
        from,
        to);

    var result = apiKeyRepositoryAdapter.findAll(query, PageRequest.of(0, 50));

    assertEquals(1, result.getTotalElements());
    assertEquals(second.getId(), result.getContent().get(0).getId());
    assertTrue(first.getCreatedAt().isBefore(from));
  }

  @TestConfiguration
  static class Config {
    @Bean
    ApiKeyRepositoryAdapter apiKeyRepositoryAdapter(ApiKeyJpaRepository apiKeyJpaRepository) {
      return new ApiKeyRepositoryAdapter(apiKeyJpaRepository);
    }
  }
}
