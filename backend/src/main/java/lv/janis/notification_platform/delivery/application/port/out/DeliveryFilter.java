package lv.janis.notification_platform.delivery.application.port.out;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.delivery.domain.DeliveryStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;

public record DeliveryFilter(
    DeliveryStatus status,
    UUID tenantId,
    UUID eventId,
    UUID endpointId,
    EndpointType channel,
    Instant from,
    Instant to) {
}
