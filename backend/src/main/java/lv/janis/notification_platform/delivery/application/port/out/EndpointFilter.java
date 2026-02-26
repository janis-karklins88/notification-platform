package lv.janis.notification_platform.delivery.application.port.out;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.delivery.domain.EndpointStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;

public record EndpointFilter(
    UUID tenantId,
    EndpointStatus status,
    EndpointType type,
    Instant createdFrom,
    Instant createdTo) {
}
