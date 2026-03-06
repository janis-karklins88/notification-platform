package lv.janis.notification_platform.delivery.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;

import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.DeliveryStatus;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.delivery.application.exception.DeliveryInProgressRetryableException;

@Service
public class DeliveryProcessingService {
  private static final Duration IN_PROGRESS_RECOVER_WINDOW = Duration.ofMinutes(5);

  public boolean hasCompleted(DeliveryStatus status) {
    return status == DeliveryStatus.DELIVERED || status == DeliveryStatus.FAILED;
  }

  public boolean isFreshInProgress(Delivery delivery, Clock clock) {
    if (delivery.getStatus() != DeliveryStatus.IN_PROGRESS) {
      return false;
    }
    Instant lastAttemptAt = delivery.getLastAttemptAt();
    if (lastAttemptAt == null) {
      return false;
    }
    return !lastAttemptAt.isBefore(Instant.now(clock).minus(IN_PROGRESS_RECOVER_WINDOW));
  }

  public void ensureNotFreshlyInProgress(Delivery delivery, Clock clock) {
    if (isFreshInProgress(delivery, clock)) {
      throw new DeliveryInProgressRetryableException("Delivery is already in progress: " + delivery.getId());
    }
  }

  public boolean checkTenantConsistency(Delivery delivery) {
    if (!Objects.equals(delivery.getTenantId(), delivery.getEvent().getTenantId()) ||
        !Objects.equals(delivery.getTenantId(), delivery.getSubscription().getTenantId()) ||
        !Objects.equals(delivery.getTenantId(), delivery.getEndpoint().getTenantId())) {
      return false;
    }
    return true;
  }

  public boolean checkEndpointType(Endpoint endpoint, EndpointType type) {
    if (endpoint == null || endpoint.getType() != type) {
      return false;
    }
    return true;
  }

  public boolean checkEndpointStatus(Endpoint endpoint) {
    if (endpoint == null || endpoint.getStatus() != EndpointStatus.ACTIVE) {
      return false;
    }
    return true;
  }
}
