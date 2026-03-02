package lv.janis.notification_platform.adminapi.application.port.in;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.routing.domain.SubscriptionStatus;

public record ListSubscriptionsQuery(UUID tenantId, String eventType, UUID endpointId, SubscriptionStatus status,
    Instant createdFrom, Instant createdTo, int page, int size) {

}
