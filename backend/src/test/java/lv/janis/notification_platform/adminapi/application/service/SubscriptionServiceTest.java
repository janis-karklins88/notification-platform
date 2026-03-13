package lv.janis.notification_platform.adminapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.CreateSubscriptionCommand;
import lv.janis.notification_platform.adminapi.application.port.in.ListSubscriptionsQuery;
import lv.janis.notification_platform.delivery.application.port.out.EndpointRepositoryPort;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.routing.application.port.out.SubscriptionFilter;
import lv.janis.notification_platform.routing.application.port.out.SubscriptionRepositoryPort;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.routing.domain.SubscriptionStatus;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static lv.janis.notification_platform.support.EntityTestData.endpoint;
import static lv.janis.notification_platform.support.EntityTestData.subscription;
import static lv.janis.notification_platform.support.EntityTestData.tenant;

class SubscriptionServiceTest {
  private final SubscriptionRepositoryPort subscriptionRepository = mock(SubscriptionRepositoryPort.class);
  private final EndpointRepositoryPort endpointRepository = mock(EndpointRepositoryPort.class);
  private final TenantRepositoryPort tenantRepository = mock(TenantRepositoryPort.class);
  private final SubscriptionService service = new SubscriptionService(subscriptionRepository, endpointRepository, tenantRepository);

  @Test
  void createSubscriptionSavesWhenEndpointBelongsToTenant() {
    UUID tenantId = UUID.randomUUID();
    UUID endpointId = UUID.randomUUID();
    Tenant tenant = tenant(tenantId);
    Endpoint endpoint = endpoint(endpointId, tenant, EndpointType.WEBHOOK);
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(endpointRepository.findById(endpointId)).thenReturn(Optional.of(endpoint));
    when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Subscription result = service.createSubscription(new CreateSubscriptionCommand(tenantId, "order.created", endpointId));

    assertEquals("order.created", result.getEventType());
    assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
    assertSame(endpoint, result.getEndpoint());
  }

  @Test
  void createSubscriptionRejectsEndpointFromAnotherTenant() {
    UUID tenantId = UUID.randomUUID();
    Tenant tenant = tenant(tenantId);
    Endpoint endpoint = endpoint(UUID.randomUUID(), tenant(UUID.randomUUID()), EndpointType.WEBHOOK);
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(endpointRepository.findById(endpoint.getId())).thenReturn(Optional.of(endpoint));

    BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.createSubscription(new CreateSubscriptionCommand(tenantId, "order.created", endpoint.getId())));

    assertEquals("Endpoint does not belong to the specified tenant", ex.getMessage());
  }

  @Test
  void listSubscriptionsClampsPageAndSizeAndDelegatesFilter() {
    UUID tenantId = UUID.randomUUID();
    UUID endpointId = UUID.randomUUID();
    Instant from = Instant.parse("2026-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-01-02T00:00:00Z");
    ListSubscriptionsQuery query = new ListSubscriptionsQuery(
        tenantId,
        "order.created",
        endpointId,
        SubscriptionStatus.ACTIVE,
        from,
        to,
        -1,
        500);
    Page<Subscription> expected = new PageImpl<>(List.of());
    when(subscriptionRepository.findAll(any(), any())).thenReturn(expected);

    Page<Subscription> result = service.listSubscriptions(query);

    assertSame(expected, result);

    ArgumentCaptor<SubscriptionFilter> filterCaptor = ArgumentCaptor.forClass(SubscriptionFilter.class);
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(subscriptionRepository).findAll(filterCaptor.capture(), pageableCaptor.capture());
    assertEquals(tenantId, filterCaptor.getValue().tenantId());
    assertEquals("order.created", filterCaptor.getValue().eventType());
    assertEquals(endpointId, filterCaptor.getValue().endpointId());
    assertEquals(SubscriptionStatus.ACTIVE, filterCaptor.getValue().status());
    assertEquals(from, filterCaptor.getValue().createdFrom());
    assertEquals(to, filterCaptor.getValue().createdTo());
    assertEquals(0, pageableCaptor.getValue().getPageNumber());
    assertEquals(100, pageableCaptor.getValue().getPageSize());
    assertEquals(Sort.Direction.DESC, pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection());
  }

  @Test
  void listSubscriptionsRejectsInvalidDateRange() {
    ListSubscriptionsQuery query = new ListSubscriptionsQuery(
        null,
        null,
        null,
        null,
        Instant.parse("2026-01-02T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"),
        0,
        10);

    BadRequestException ex = assertThrows(BadRequestException.class, () -> service.listSubscriptions(query));

    assertEquals("createdFrom must be before or equal to createdTo", ex.getMessage());
  }

  @Test
  void deleteActivateAndDeactivateSubscriptionPersistStateChanges() {
    UUID subscriptionId = UUID.randomUUID();
    Tenant tenant = tenant(UUID.randomUUID());
    Endpoint endpoint = endpoint(UUID.randomUUID(), tenant, EndpointType.EMAIL);
    Subscription subscription = subscription(subscriptionId, tenant, "order.created", endpoint);
    when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(subscription));
    when(subscriptionRepository.save(subscription)).thenReturn(subscription);

    service.deactivateSubscription(subscriptionId);
    assertEquals(SubscriptionStatus.PAUSED, subscription.getStatus());

    service.activateSubscription(subscriptionId);
    assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());

    service.deleteSubscription(subscriptionId);
    assertEquals(SubscriptionStatus.DELETED, subscription.getStatus());
  }

  @Test
  void createSubscriptionThrowsNotFoundWhenTenantMissing() {
    UUID tenantId = UUID.randomUUID();
    UUID endpointId = UUID.randomUUID();
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(
        NotFoundException.class,
        () -> service.createSubscription(new CreateSubscriptionCommand(tenantId, "order.created", endpointId)));

    assertEquals("Tenant with id " + tenantId + " not found", ex.getMessage());
  }
}
