package lv.janis.notification_platform.delivery.application.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.DeliveryStatus;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;

@Service
public class DeliveryProcessingService {
  public boolean hasCompleted(DeliveryStatus status) {
    return status == DeliveryStatus.DELIVERED || status == DeliveryStatus.FAILED;
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
