package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.DeliveryStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;

public record DeliveryMonitoringResponse(
    UUID id,
    UUID tenantId,
    UUID eventId,
    UUID endpointId,
    EndpointType channel,
    DeliveryStatus status,
    Instant lastAttemptAt,
    String lastError,
    Instant createdAt,
    Instant updatedAt) {
  public static DeliveryMonitoringResponse from(Delivery delivery) {
    return new DeliveryMonitoringResponse(
        delivery.getId(),
        delivery.getTenantId(),
        delivery.getEventId(),
        delivery.getEndpointId(),
        delivery.getEndpoint().getType(),
        delivery.getStatus(),
        delivery.getLastAttemptAt(),
        delivery.getLastError(),
        delivery.getCreatedAt(),
        delivery.getUpdatedAt());
  }
}
