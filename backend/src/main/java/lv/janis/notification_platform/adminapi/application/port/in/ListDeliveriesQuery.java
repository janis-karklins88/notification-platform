package lv.janis.notification_platform.adminapi.application.port.in;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.delivery.domain.DeliveryStatus;

public record ListDeliveriesQuery(
    int page,
    int size,
    DeliveryStatus status,
    UUID tenantId,
    UUID eventId,
    UUID endpointId,
    String channel,
    Instant from,
    Instant to) {
}
