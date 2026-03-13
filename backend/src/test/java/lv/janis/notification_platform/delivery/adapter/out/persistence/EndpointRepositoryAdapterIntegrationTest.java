package lv.janis.notification_platform.delivery.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

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

import com.fasterxml.jackson.databind.node.TextNode;

import lv.janis.notification_platform.config.JpaAuditingConfig;
import lv.janis.notification_platform.delivery.application.port.out.EndpointFilter;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.tenant.adapter.out.persistence.TenantJpaRepository;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

@DataJpaTest
@Import({JpaAuditingConfig.class, EndpointRepositoryAdapterIntegrationTest.Config.class})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:notification_platform_endpoint_repo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class EndpointRepositoryAdapterIntegrationTest {

  @Autowired
  private EndpointRepositoryAdapter endpointRepositoryAdapter;

  @Autowired
  private TenantJpaRepository tenantRepository;

  @Test
  void findAllSupportsCombinedFilters() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-endpoint", "Tenant Endpoint", TenantStatus.ACTIVE));
    Tenant otherTenant = tenantRepository.save(new Tenant("tenant-endpoint-other", "Tenant Endpoint Other", TenantStatus.ACTIVE));

    Endpoint kept = endpointRepositoryAdapter.save(new Endpoint(tenant, EndpointType.WEBHOOK, TextNode.valueOf("kept")));
    Endpoint inactive = endpointRepositoryAdapter.save(new Endpoint(tenant, EndpointType.EMAIL, TextNode.valueOf("inactive")));
    inactive.deactivate();
    endpointRepositoryAdapter.save(inactive);
    endpointRepositoryAdapter.save(new Endpoint(otherTenant, EndpointType.WEBHOOK, TextNode.valueOf("other")));

    EndpointFilter filter = new EndpointFilter(tenant.getId(), EndpointStatus.ACTIVE, EndpointType.WEBHOOK, null, null);

    var result = endpointRepositoryAdapter.findAll(filter, PageRequest.of(0, 50));

    assertEquals(1, result.getTotalElements());
    assertEquals(kept.getId(), result.getContent().get(0).getId());
  }

  @Test
  void findAllSupportsTimeRangeFilter() throws InterruptedException {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-endpoint-time", "Tenant Endpoint Time", TenantStatus.ACTIVE));
    Endpoint first = endpointRepositoryAdapter.save(new Endpoint(tenant, EndpointType.WEBHOOK, TextNode.valueOf("first")));
    Thread.sleep(700);
    Endpoint second = endpointRepositoryAdapter.save(new Endpoint(tenant, EndpointType.WEBHOOK, TextNode.valueOf("second")));

    Instant from = second.getCreatedAt().minusMillis(1);
    Instant to = second.getCreatedAt().plusMillis(1);

    var result = endpointRepositoryAdapter.findAll(
        new EndpointFilter(tenant.getId(), null, null, from, to),
        PageRequest.of(0, 50));

    assertEquals(1, result.getTotalElements());
    assertEquals(second.getId(), result.getContent().get(0).getId());
    assertTrue(first.getCreatedAt().isBefore(from));
  }

  @Test
  void findByTenantIdAndStatusDelegateToJpaQueries() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-endpoint-list", "Tenant Endpoint List", TenantStatus.ACTIVE));
    Endpoint active = endpointRepositoryAdapter.save(new Endpoint(tenant, EndpointType.WEBHOOK, TextNode.valueOf("active")));
    Endpoint inactive = endpointRepositoryAdapter.save(new Endpoint(tenant, EndpointType.EMAIL, TextNode.valueOf("inactive")));
    inactive.deactivate();
    endpointRepositoryAdapter.save(inactive);

    var byTenant = endpointRepositoryAdapter.findByTenantId(tenant.getId());
    var activeOnly = endpointRepositoryAdapter.findByTenantIdAndStatus(tenant.getId(), EndpointStatus.ACTIVE);

    assertEquals(2, byTenant.size());
    assertEquals(1, activeOnly.size());
    assertEquals(active.getId(), activeOnly.get(0).getId());
  }

  @TestConfiguration
  static class Config {
    @Bean
    EndpointRepositoryAdapter endpointRepositoryAdapter(EndpointJpaRepository endpointJpaRepository) {
      return new EndpointRepositoryAdapter(endpointJpaRepository);
    }
  }
}
