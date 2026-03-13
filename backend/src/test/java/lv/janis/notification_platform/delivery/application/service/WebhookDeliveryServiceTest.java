package lv.janis.notification_platform.delivery.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.delivery.application.exception.DeliveryNonRetryableException;
import lv.janis.notification_platform.delivery.application.port.out.DeliveryRepositoryPort;
import lv.janis.notification_platform.delivery.application.port.out.WebhookSenderPort;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.shared.metrics.NotificationMetrics;
import org.junit.jupiter.api.Test;

import static lv.janis.notification_platform.support.EntityTestData.delivery;
import static lv.janis.notification_platform.support.EntityTestData.endpoint;
import static lv.janis.notification_platform.support.EntityTestData.event;
import static lv.janis.notification_platform.support.EntityTestData.subscription;
import static lv.janis.notification_platform.support.EntityTestData.tenant;

class WebhookDeliveryServiceTest {
  private static final Instant NOW = Instant.parse("2026-02-02T10:00:00Z");

  private final DeliveryRepositoryPort repository = mock(DeliveryRepositoryPort.class);
  private final WebhookSenderPort sender = mock(WebhookSenderPort.class);
  private final DeliveryProcessingService processingService = new DeliveryProcessingService();
  private final NotificationMetrics notificationMetrics = mock(NotificationMetrics.class);
  private final WebhookDeliveryService service = new WebhookDeliveryService(
      repository,
      sender,
      processingService,
      Clock.fixed(NOW, ZoneOffset.UTC),
      notificationMetrics);

  @Test
  void deliverWebhookReturnsWhenDeliveryAlreadyCompleted() {
    Delivery delivery = deliveryTree(EndpointType.WEBHOOK);
    delivery.markDelivered(NOW.minusSeconds(5));
    when(repository.findById(delivery.getId())).thenReturn(Optional.of(delivery));

    service.deliverWebhook(delivery.getId());

    verify(sender, never()).send(any());
    verify(repository, never()).save(any());
  }

  @Test
  void deliverWebhookRejectsWrongEndpointType() {
    Delivery delivery = deliveryTree(EndpointType.EMAIL);
    when(repository.findById(delivery.getId())).thenReturn(Optional.of(delivery));

    BadRequestException ex = assertThrows(BadRequestException.class, () -> service.deliverWebhook(delivery.getId()));

    assertEquals("Endpoint is not for webhook delivery", ex.getMessage());
  }

  @Test
  void deliverWebhookMarksDeliveryDeliveredOnSuccess() {
    Delivery delivery = deliveryTree(EndpointType.WEBHOOK);
    when(repository.findById(delivery.getId())).thenReturn(Optional.of(delivery));

    service.deliverWebhook(delivery.getId());

    assertEquals(lv.janis.notification_platform.delivery.domain.DeliveryStatus.DELIVERED, delivery.getStatus());
    assertEquals(NOW, delivery.getDeliveredAt());
    verify(repository, times(2)).save(delivery);
    verify(notificationMetrics).incrementDeliveryAttemptStarted();
    verify(notificationMetrics).incrementDeliverySuccess();
  }

  @Test
  void deliverWebhookMarksDeliveryFailedForNonRetryableError() {
    Delivery delivery = deliveryTree(EndpointType.WEBHOOK);
    when(repository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
    doThrow(new IllegalArgumentException("bad request")).when(sender).send(delivery);

    DeliveryNonRetryableException ex = assertThrows(
        DeliveryNonRetryableException.class,
        () -> service.deliverWebhook(delivery.getId()));

    assertEquals("bad request", ex.getCause().getMessage());
    assertEquals(lv.janis.notification_platform.delivery.domain.DeliveryStatus.FAILED, delivery.getStatus());
    assertEquals("bad request", delivery.getLastError());
    verify(repository, times(2)).save(delivery);
    verify(notificationMetrics).incrementDeliveryFailure();
  }

  @Test
  void deliverWebhookRethrowsRetryableErrorWithoutMarkingFailed() {
    Delivery delivery = deliveryTree(EndpointType.WEBHOOK);
    RuntimeException failure = new RuntimeException("temporary");
    when(repository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
    doThrow(failure).when(sender).send(delivery);

    RuntimeException ex = assertThrows(RuntimeException.class, () -> service.deliverWebhook(delivery.getId()));

    assertEquals(failure, ex);
    assertEquals(lv.janis.notification_platform.delivery.domain.DeliveryStatus.IN_PROGRESS, delivery.getStatus());
    verify(repository).save(delivery);
    verify(notificationMetrics).incrementDeliveryRetryScheduled();
  }

  private static Delivery deliveryTree(EndpointType type) {
    var tenant = tenant(UUID.randomUUID());
    var endpoint = endpoint(UUID.randomUUID(), tenant, type);
    var event = event(UUID.randomUUID(), tenant, "order.created");
    var subscription = subscription(UUID.randomUUID(), tenant, "order.created", endpoint);
    return delivery(UUID.randomUUID(), tenant, event, subscription, endpoint);
  }
}
