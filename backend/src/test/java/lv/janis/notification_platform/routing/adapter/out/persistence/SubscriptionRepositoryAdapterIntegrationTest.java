package lv.janis.notification_platform.routing.adapter.out.persistence;

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
import lv.janis.notification_platform.delivery.adapter.out.persistence.EndpointJpaRepository;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.routing.application.port.out.SubscriptionFilter;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.routing.domain.SubscriptionStatus;
import lv.janis.notification_platform.tenant.adapter.out.persistence.TenantJpaRepository;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

@DataJpaTest
@Import({JpaAuditingConfig.class, SubscriptionRepositoryAdapterIntegrationTest.Config.class})
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:notification_platform_subscription_repo;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class SubscriptionRepositoryAdapterIntegrationTest {

  @Autowired
  private SubscriptionRepositoryAdapter subscriptionRepositoryAdapter;

  @Autowired
  private TenantJpaRepository tenantRepository;

  @Autowired
  private EndpointJpaRepository endpointRepository;

  @Test
  void findAllSupportsCombinedFilters() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-sub", "Tenant Sub", TenantStatus.ACTIVE));
    Tenant otherTenant = tenantRepository.save(new Tenant("tenant-sub-other", "Tenant Sub Other", TenantStatus.ACTIVE));

    Endpoint webhook = endpointRepository.save(new Endpoint(tenant, EndpointType.WEBHOOK, TextNode.valueOf("webhook")));
    Endpoint email = endpointRepository.save(new Endpoint(tenant, EndpointType.EMAIL, TextNode.valueOf("email")));
    Endpoint other = endpointRepository.save(new Endpoint(otherTenant, EndpointType.WEBHOOK, TextNode.valueOf("other")));

    Subscription kept = subscriptionRepositoryAdapter.save(new Subscription(tenant, "order.created", webhook));
    Subscription paused = subscriptionRepositoryAdapter.save(new Subscription(tenant, "order.created", email));
    paused.pause();
    subscriptionRepositoryAdapter.save(paused);
    subscriptionRepositoryAdapter.save(new Subscription(otherTenant, "order.created", other));

    SubscriptionFilter filter = new SubscriptionFilter(
        tenant.getId(),
        "  order.created ",
        webhook.getId(),
        SubscriptionStatus.ACTIVE,
        null,
        null);

    var result = subscriptionRepositoryAdapter.findAll(filter, PageRequest.of(0, 50));

    assertEquals(1, result.getTotalElements());
    assertEquals(kept.getId(), result.getContent().get(0).getId());
  }

  @Test
  void findAllSupportsTimeRangeFilter() throws InterruptedException {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-sub-time", "Tenant Sub Time", TenantStatus.ACTIVE));
    Endpoint endpoint = endpointRepository.save(new Endpoint(tenant, EndpointType.WEBHOOK, TextNode.valueOf("endpoint")));
    Subscription first = subscriptionRepositoryAdapter.save(new Subscription(tenant, "order.created", endpoint));
    Thread.sleep(700);
    Subscription second = subscriptionRepositoryAdapter.save(new Subscription(tenant, "order.updated", endpoint));

    Instant from = second.getCreatedAt().minusMillis(1);
    Instant to = second.getCreatedAt().plusMillis(1);

    var result = subscriptionRepositoryAdapter.findAll(
        new SubscriptionFilter(tenant.getId(), null, null, null, from, to),
        PageRequest.of(0, 50));

    assertEquals(1, result.getTotalElements());
    assertEquals(second.getId(), result.getContent().get(0).getId());
    assertTrue(first.getCreatedAt().isBefore(from));
  }

  @Test
  void findByTenantIdAndFindActiveByTenantIdAndEventTypeReturnExpectedSubscriptions() {
    Tenant tenant = tenantRepository.save(new Tenant("tenant-sub-list", "Tenant Sub List", TenantStatus.ACTIVE));
    Endpoint endpoint = endpointRepository.save(new Endpoint(tenant, EndpointType.WEBHOOK, TextNode.valueOf("endpoint")));
    Subscription active = subscriptionRepositoryAdapter.save(new Subscription(tenant, "order.created", endpoint));
    Subscription paused = subscriptionRepositoryAdapter.save(new Subscription(tenant, "order.created", endpointRepository.save(new Endpoint(tenant, EndpointType.EMAIL, TextNode.valueOf("endpoint2")))));
    paused.pause();
    subscriptionRepositoryAdapter.save(paused);

    var byTenant = subscriptionRepositoryAdapter.findByTenantId(tenant.getId());
    var activeOnly = subscriptionRepositoryAdapter.findActiveByTenantIdAndEventType(tenant.getId(), "order.created");

    assertEquals(2, byTenant.size());
    assertEquals(1, activeOnly.size());
    assertEquals(active.getId(), activeOnly.get(0).getId());
  }

  @TestConfiguration
  static class Config {
    @Bean
    SubscriptionRepositoryAdapter subscriptionRepositoryAdapter(SubscriptionJpaRepository subscriptionJpaRepository) {
      return new SubscriptionRepositoryAdapter(subscriptionJpaRepository);
    }
  }
}
