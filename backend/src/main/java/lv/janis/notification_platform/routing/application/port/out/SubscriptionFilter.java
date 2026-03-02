package lv.janis.notification_platform.routing.application.port.out;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.routing.domain.SubscriptionStatus;

public record SubscriptionFilter(UUID tenantId, String eventType, UUID endpointId, SubscriptionStatus status,
        Instant createdFrom, Instant createdTo) {

}
