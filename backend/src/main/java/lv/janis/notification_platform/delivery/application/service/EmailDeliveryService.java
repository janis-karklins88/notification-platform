package lv.janis.notification_platform.delivery.application.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.delivery.application.exception.DeliveryNonRetryableException;
import lv.janis.notification_platform.delivery.adapter.in.messaging.DeliveryListenerFailurePolicy;
import lv.janis.notification_platform.delivery.adapter.out.sender.EmailSenderAdapter;
import lv.janis.notification_platform.delivery.application.port.in.EmailDeliveryUseCase;
import lv.janis.notification_platform.delivery.application.port.out.DeliveryRepositoryPort;
import lv.janis.notification_platform.delivery.domain.EndpointType;

@Service
public class EmailDeliveryService implements EmailDeliveryUseCase {
  private final DeliveryRepositoryPort deliveryRepositoryPort;
  private final EmailSenderAdapter emailSenderAdapter;
  private final DeliveryProcessingService deliveryProcessingService;
  private final Clock clock;

  public EmailDeliveryService(DeliveryRepositoryPort deliveryRepositoryPort,
      EmailSenderAdapter emailSenderAdapter, DeliveryProcessingService deliveryProcessingService, Clock clock) {
    this.deliveryRepositoryPort = deliveryRepositoryPort;
    this.emailSenderAdapter = emailSenderAdapter;
    this.deliveryProcessingService = deliveryProcessingService;
    this.clock = clock;
  }

  @Override
  @Transactional(dontRollbackOn = {DeliveryNonRetryableException.class})
  public void deliverEmail(UUID deliveryId) {
    var delivery = deliveryRepositoryPort.findById(deliveryId)
        .orElseThrow(() -> new NotFoundException("Delivery not found: " + deliveryId));

    if (deliveryProcessingService.hasCompleted(delivery.getStatus())) {
      return;
    }

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
    try {
      emailSenderAdapter.send(delivery);
      delivery.markDelivered(clock.instant());
      deliveryRepositoryPort.save(delivery);
      return;
    } catch (Exception ex) {
      if (DeliveryListenerFailurePolicy.isNonRetryable(ex)) {
        delivery.markFailed(clock.instant(), ex.getMessage());
        deliveryRepositoryPort.save(delivery);
        throw new DeliveryNonRetryableException(ex);
      }
      throw ex;
    }
  }
}
