package lv.janis.notification_platform.adminapi.application.port.in;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.delivery.domain.EndpointStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;

public record ListEndpointQuery(
    int page,
    int size,
    UUID tenantId,
    EndpointStatus status,
    EndpointType type,
    Instant createdFrom,
    Instant createdTo) {
}
