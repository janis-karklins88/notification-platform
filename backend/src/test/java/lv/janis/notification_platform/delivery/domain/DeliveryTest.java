package lv.janis.notification_platform.delivery.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;

class DeliveryTest {

  @Test
  void constructorSetsPendingStateWhenTenantsMatch() {
    UUID tenantId = UUID.randomUUID();
    Tenant tenant = tenantWithId(tenantId);
    Event event = eventWithTenant(tenant);
    Subscription subscription = subscriptionWithTenant(tenant);
    Endpoint endpoint = endpointWithTenant(tenant);

    Delivery delivery = new Delivery(tenant, event, subscription, endpoint);

    assertEquals(DeliveryStatus.PENDING, delivery.getStatus());
    assertNull(delivery.getLastAttemptAt());
  }

  @Test
  void constructorRejectsMismatchedTenants() {
    UUID tenantId = UUID.randomUUID();
    UUID differentTenantId = UUID.randomUUID();

    Tenant tenant = tenantWithId(tenantId);
    Event event = eventWithTenant(tenant);
    Subscription subscription = subscriptionWithTenant(tenant);
    Endpoint endpoint = endpointWithTenant(tenantWithId(differentTenantId));

    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class, () -> new Delivery(tenant, event, subscription, endpoint));
    assertEquals("event, subscription and endpoint must belong to the same tenant", ex.getMessage());
  }

  @Test
  void markDeliveredClearsFailureState() {
    Delivery delivery = deliveryWithTenant(tenantWithId(UUID.randomUUID()));
    delivery.markFailed(Instant.parse("2026-01-01T00:00:00Z"), "temporary");

    Instant deliveredAt = Instant.parse("2026-01-01T00:00:10Z");
    delivery.markDelivered(deliveredAt);

    assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
    assertEquals(deliveredAt, delivery.getDeliveredAt());
    assertNull(delivery.getFailedAt());
    assertNull(delivery.getLastError());
  }

  @Test
  void markFailedNormalizesErrorMessage() {
    Delivery delivery = deliveryWithTenant(tenantWithId(UUID.randomUUID()));

    delivery.markFailed(Instant.parse("2026-01-01T00:00:00Z"), "   temporary failure   ");
    assertEquals("temporary failure", delivery.getLastError());

    delivery.markFailed(Instant.parse("2026-01-01T00:00:10Z"), "   ");
    assertNull(delivery.getLastError());
  }

  @Test
  void markInProgressRequiresAttemptTime() {
    Delivery delivery = deliveryWithTenant(tenantWithId(UUID.randomUUID()));

    assertThrows(NullPointerException.class, () -> delivery.markInProgress(null));
  }

  @Test
  void markDeliveredRequiresDeliveredAt() {
    Delivery delivery = deliveryWithTenant(tenantWithId(UUID.randomUUID()));

    assertThrows(NullPointerException.class, () -> delivery.markDelivered(null));
  }

  @Test
  void markFailedRequiresFailureTime() {
    Delivery delivery = deliveryWithTenant(tenantWithId(UUID.randomUUID()));

    assertThrows(NullPointerException.class, () -> delivery.markFailed(null, "temporary"));
  }

  @Test
  void transitionToDeliveredClearsFailedFields() {
    Delivery delivery = deliveryWithTenant(tenantWithId(UUID.randomUUID()));

    Instant failedAt = Instant.parse("2026-01-01T00:00:00Z");
    delivery.markFailed(failedAt, "temporary");
    delivery.markDelivered(Instant.parse("2026-01-01T00:00:10Z"));

    assertEquals(DeliveryStatus.DELIVERED, delivery.getStatus());
    assertEquals(Instant.parse("2026-01-01T00:00:10Z"), delivery.getDeliveredAt());
    assertNull(delivery.getFailedAt());
    assertNull(delivery.getLastError());
  }

  @Test
  void transitionToFailedSetsFailureStateWithoutClearingLastAttempt() {
    Delivery delivery = deliveryWithTenant(tenantWithId(UUID.randomUUID()));
    Instant attemptedAt = Instant.parse("2026-01-01T00:00:00Z");
    delivery.markInProgress(attemptedAt);

    Instant failedAt = Instant.parse("2026-01-01T00:00:05Z");
    delivery.markFailed(failedAt, "  down for maintenance ");

    assertEquals(DeliveryStatus.FAILED, delivery.getStatus());
    assertEquals(attemptedAt, delivery.getLastAttemptAt());
    assertEquals(failedAt, delivery.getFailedAt());
    assertEquals("down for maintenance", delivery.getLastError());
  }

  @Test
  void constructorRejectsNullInputs() {
    Tenant tenant = tenantWithId(UUID.randomUUID());
    Event event = eventWithTenant(tenant);
    Subscription subscription = subscriptionWithTenant(tenant);
    Endpoint endpoint = endpointWithTenant(tenant);

    assertThrows(NullPointerException.class, () -> new Delivery(null, event, subscription, endpoint));
    assertThrows(NullPointerException.class, () -> new Delivery(tenant, null, subscription, endpoint));
    assertThrows(NullPointerException.class, () -> new Delivery(tenant, event, null, endpoint));
    assertThrows(NullPointerException.class, () -> new Delivery(tenant, event, subscription, null));
  }

  @Test
  void constructorRejectsMissingTenantIdentifiers() {
    UUID tenantId = UUID.randomUUID();
    Tenant tenant = tenantWithId(tenantId);
    Event event = eventWithTenant(tenant);
    Subscription subscription = subscriptionWithTenant(tenant);
    Endpoint endpoint = endpointWithTenant(tenant);

    Tenant tenantWithoutId = tenantWithoutId();
    Event eventWithoutTenantId = eventWithTenant(tenantWithoutId);
    Subscription subscriptionWithoutTenantId = subscriptionWithTenant(tenantWithoutId);
    Endpoint endpointWithoutTenantId = endpointWithTenant(tenantWithoutId);

    assertThrows(NullPointerException.class, () -> new Delivery(tenantWithoutId, event, subscription, endpoint));
    assertThrows(
        NullPointerException.class,
        () -> new Delivery(tenant, eventWithoutTenantId, subscription, endpoint));
    assertThrows(
        NullPointerException.class,
        () -> new Delivery(tenant, event, subscriptionWithoutTenantId, endpoint));
    assertThrows(
        NullPointerException.class,
        () -> new Delivery(tenant, event, subscription, endpointWithoutTenantId));
  }

  private static Delivery deliveryWithTenant(Tenant tenant) {
    return new Delivery(
        tenant,
        eventWithTenant(tenant),
        subscriptionWithTenant(tenant),
        endpointWithTenant(tenant));
  }

  private static Tenant tenantWithId(UUID id) {
    Tenant tenant = mock(Tenant.class);
    when(tenant.getId()).thenReturn(id);
    return tenant;
  }

  private static Tenant tenantWithoutId() {
    return mock(Tenant.class);
  }

  private static Event eventWithTenant(Tenant tenant) {
    Event event = mock(Event.class);
    when(event.getTenant()).thenReturn(tenant);
    return event;
  }

  private static Subscription subscriptionWithTenant(Tenant tenant) {
    Subscription subscription = mock(Subscription.class);
    when(subscription.getTenant()).thenReturn(tenant);
    return subscription;
  }

  private static Endpoint endpointWithTenant(Tenant tenant) {
    Endpoint endpoint = mock(Endpoint.class);
    when(endpoint.getTenant()).thenReturn(tenant);
    return endpoint;
  }
}
