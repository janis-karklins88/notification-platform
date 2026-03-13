package lv.janis.notification_platform.delivery.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import lv.janis.notification_platform.delivery.application.exception.DeliveryInProgressRetryableException;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.DeliveryStatus;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;

import static lv.janis.notification_platform.support.EntityTestData.delivery;
import static lv.janis.notification_platform.support.EntityTestData.endpoint;
import static lv.janis.notification_platform.support.EntityTestData.event;
import static lv.janis.notification_platform.support.EntityTestData.subscription;
import static lv.janis.notification_platform.support.EntityTestData.tenant;

class DeliveryProcessingServiceTest {
  private static final Instant NOW = Instant.parse("2026-02-01T12:00:00Z");

  private final DeliveryProcessingService service = new DeliveryProcessingService();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void hasCompletedReturnsTrueOnlyForDeliveredAndFailedStatuses() {
    assertTrue(service.hasCompleted(DeliveryStatus.DELIVERED));
    assertTrue(service.hasCompleted(DeliveryStatus.FAILED));
    assertFalse(service.hasCompleted(DeliveryStatus.PENDING));
    assertFalse(service.hasCompleted(DeliveryStatus.IN_PROGRESS));
  }

  @Test
  void isFreshInProgressChecksRecoveryWindow() {
    Delivery fresh = deliveryTree(EndpointType.WEBHOOK);
    fresh.markInProgress(NOW.minusSeconds(60));
    Delivery stale = deliveryTree(EndpointType.WEBHOOK);
    stale.markInProgress(NOW.minusSeconds(301));

    assertTrue(service.isFreshInProgress(fresh, clock));
    assertFalse(service.isFreshInProgress(stale, clock));
  }

  @Test
  void ensureNotFreshlyInProgressThrowsForFreshDelivery() {
    Delivery delivery = deliveryTree(EndpointType.WEBHOOK);
    delivery.markInProgress(NOW.minusSeconds(30));

    assertThrows(DeliveryInProgressRetryableException.class, () -> service.ensureNotFreshlyInProgress(delivery, clock));
  }

  @Test
  void checkTenantConsistencyReturnsFalseWhenAnyReferenceDiffers() {
    Tenant tenant = tenant(UUID.randomUUID());
    Endpoint endpoint = endpoint(UUID.randomUUID(), tenant, EndpointType.WEBHOOK);
    Event event = event(UUID.randomUUID(), tenant, "order.created");
    Subscription subscription = subscription(UUID.randomUUID(), tenant, "order.created", endpoint);
    Delivery delivery = delivery(UUID.randomUUID(), tenant, event, subscription, endpoint);
    ReflectionTestUtils.setField(event, "tenantId", UUID.randomUUID());

    assertFalse(service.checkTenantConsistency(delivery));
  }

  @Test
  void checkEndpointTypeAndStatusValidateExpectedValues() {
    Endpoint emailEndpoint = endpoint(UUID.randomUUID(), tenant(UUID.randomUUID()), EndpointType.EMAIL, JsonNodeFactory.instance.objectNode(), EndpointStatus.ACTIVE);
    Endpoint inactiveWebhook = endpoint(
        UUID.randomUUID(),
        tenant(UUID.randomUUID()),
        EndpointType.WEBHOOK,
        JsonNodeFactory.instance.objectNode(),
        EndpointStatus.INACTIVE);

    assertTrue(service.checkEndpointType(emailEndpoint, EndpointType.EMAIL));
    assertFalse(service.checkEndpointType(emailEndpoint, EndpointType.WEBHOOK));
    assertTrue(service.checkEndpointStatus(emailEndpoint));
    assertFalse(service.checkEndpointStatus(inactiveWebhook));
    assertFalse(service.checkEndpointStatus(null));
  }

  private static Delivery deliveryTree(EndpointType endpointType) {
    Tenant tenant = tenant(UUID.randomUUID());
    Endpoint endpoint = endpoint(UUID.randomUUID(), tenant, endpointType);
    Event event = event(UUID.randomUUID(), tenant, "order.created");
    Subscription subscription = subscription(UUID.randomUUID(), tenant, "order.created", endpoint);
    return delivery(UUID.randomUUID(), tenant, event, subscription, endpoint);
  }
}
