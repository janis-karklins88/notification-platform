package lv.janis.notification_platform.delivery.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.delivery.application.exception.DeliveryNonRetryableException;
import lv.janis.notification_platform.delivery.adapter.in.messaging.DeliveryListenerFailurePolicy;
import lv.janis.notification_platform.delivery.application.port.in.EmailDeliveryUseCase;
import lv.janis.notification_platform.delivery.application.port.out.EmailSenderPort;
import lv.janis.notification_platform.delivery.application.port.out.DeliveryRepositoryPort;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.shared.metrics.NotificationMetrics;

@Service
public class EmailDeliveryService implements EmailDeliveryUseCase {
  private final DeliveryRepositoryPort deliveryRepositoryPort;
  private final EmailSenderPort emailSenderPort;
  private final DeliveryProcessingService deliveryProcessingService;
  private final Clock clock;
  private final EmailMessageFactory emailMessageFactory;
  private final NotificationMetrics notificationMetrics;

  public EmailDeliveryService(DeliveryRepositoryPort deliveryRepositoryPort,
      EmailSenderPort emailSenderPort, DeliveryProcessingService deliveryProcessingService, EmailMessageFactory emailMessageFactory,
      Clock clock, NotificationMetrics notificationMetrics) {
    this.deliveryRepositoryPort = deliveryRepositoryPort;
    this.emailSenderPort = emailSenderPort;
    this.deliveryProcessingService = deliveryProcessingService;
    this.emailMessageFactory = emailMessageFactory;
    this.clock = clock;
    this.notificationMetrics = notificationMetrics;
  }

  @Override
  @Transactional(dontRollbackOn = {DeliveryNonRetryableException.class})
  public void deliverEmail(UUID deliveryId) {
    var delivery = deliveryRepositoryPort.findById(deliveryId)
        .orElseThrow(() -> new NotFoundException("Delivery not found: " + deliveryId));

    if (deliveryProcessingService.hasCompleted(delivery.getStatus())) {
      return;
    }

    deliveryProcessingService.ensureNotFreshlyInProgress(delivery, clock);

    if (!deliveryProcessingService.checkTenantConsistency(delivery)) {
      throw new BadRequestException("Tenant is not consistent");
    }

    if (!deliveryProcessingService.checkEndpointType(delivery.getEndpoint(), EndpointType.EMAIL)) {
      throw new BadRequestException("Endpoint is not for email delivery");
    }

    if (!deliveryProcessingService.checkEndpointStatus(delivery.getEndpoint())) {
      throw new BadRequestException("Endpoint is not active");
    }

    delivery.markInProgress(clock.instant());
    deliveryRepositoryPort.save(delivery);
    notificationMetrics.incrementDeliveryAttemptStarted();
    try {
      emailSenderPort.send(emailMessageFactory.build(delivery));
      delivery.markDelivered(clock.instant());
      deliveryRepositoryPort.save(delivery);
      notificationMetrics.incrementDeliverySuccess();
      return;
    } catch (Exception ex) {
      if (DeliveryListenerFailurePolicy.isNonRetryable(ex)) {
        delivery.markFailed(clock.instant(), ex.getMessage());
        deliveryRepositoryPort.save(delivery);
        notificationMetrics.incrementDeliveryFailure();
        throw new DeliveryNonRetryableException(ex);
      }
      notificationMetrics.incrementDeliveryRetryScheduled();
      throw ex;
    }
  }
}
