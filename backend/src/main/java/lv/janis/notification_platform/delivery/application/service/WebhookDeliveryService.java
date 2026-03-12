package lv.janis.notification_platform.delivery.application.service;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.delivery.application.exception.DeliveryNonRetryableException;
import lv.janis.notification_platform.delivery.adapter.in.messaging.DeliveryListenerFailurePolicy;
import lv.janis.notification_platform.delivery.application.port.in.WebhookDeliveryUseCase;
import lv.janis.notification_platform.delivery.application.port.out.DeliveryRepositoryPort;
import lv.janis.notification_platform.delivery.application.port.out.WebhookSenderPort;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.shared.metrics.NotificationMetrics;

@Service
public class WebhookDeliveryService implements WebhookDeliveryUseCase {
  private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);

  private final DeliveryRepositoryPort deliveryRepositoryPort;
  private final WebhookSenderPort webhookSenderPort;
  private final DeliveryProcessingService deliveryProcessingService;
  private final Clock clock;
  private final NotificationMetrics notificationMetrics;

  public WebhookDeliveryService(DeliveryRepositoryPort deliveryRepositoryPort,
      WebhookSenderPort webhookSenderPort, DeliveryProcessingService deliveryProcessingService, Clock clock,
      NotificationMetrics notificationMetrics) {
    this.deliveryRepositoryPort = deliveryRepositoryPort;
    this.webhookSenderPort = webhookSenderPort;
    this.deliveryProcessingService = deliveryProcessingService;
    this.clock = clock;
    this.notificationMetrics = notificationMetrics;
  }

  @Override
  @Transactional(dontRollbackOn = { DeliveryNonRetryableException.class })
  public void deliverWebhook(UUID deliveryId) {
    var delivery = deliveryRepositoryPort.findById(deliveryId)
        .orElseThrow(() -> new NotFoundException("Delivery not found: " + deliveryId));

    if (deliveryProcessingService.hasCompleted(delivery.getStatus())) {
      return;
    }

    deliveryProcessingService.ensureNotFreshlyInProgress(delivery, clock);

    if (!deliveryProcessingService.checkTenantConsistency(delivery)) {
      throw new BadRequestException("Tenant is not consistent");
    }

    if (!deliveryProcessingService.checkEndpointType(delivery.getEndpoint(), EndpointType.WEBHOOK)) {
      throw new BadRequestException("Endpoint is not for webhook delivery");
    }

    if (!deliveryProcessingService.checkEndpointStatus(delivery.getEndpoint())) {
      throw new BadRequestException("Endpoint is not active");
    }

    delivery.markInProgress(clock.instant());
    deliveryRepositoryPort.save(delivery);
    notificationMetrics.incrementDeliveryAttemptStarted();
    log.info("Delivery attempt started deliveryId={} tenantId={} endpointId={}", delivery.getId(),
        delivery.getTenantId(), delivery.getEndpointId());
    try {
      webhookSenderPort.send(delivery);
      delivery.markDelivered(clock.instant());
      deliveryRepositoryPort.save(delivery);
      notificationMetrics.incrementDeliverySuccess();
      log.info("Delivery succeeded deliveryId={} tenantId={} endpointId={}", delivery.getId(),
          delivery.getTenantId(), delivery.getEndpointId());
    } catch (Exception ex) {
      if (DeliveryListenerFailurePolicy.isNonRetryable(ex)) {
        delivery.markFailed(clock.instant(), ex.getMessage());
        deliveryRepositoryPort.save(delivery);
        notificationMetrics.incrementDeliveryFailure();
        log.warn("Delivery failed (non-retryable) deliveryId={} tenantId={} endpointId={} reason={}", delivery.getId(),
            delivery.getTenantId(), delivery.getEndpointId(), ex.getMessage());
        throw new DeliveryNonRetryableException(ex);
      }
      notificationMetrics.incrementDeliveryRetryScheduled();
      log.info("Delivery retry scheduled deliveryId={} tenantId={} endpointId={} reason={}", delivery.getId(),
          delivery.getTenantId(), delivery.getEndpointId(), ex.getMessage());
      throw ex;
    }

  }

}
