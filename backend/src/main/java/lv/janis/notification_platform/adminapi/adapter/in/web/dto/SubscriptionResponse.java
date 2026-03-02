package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.routing.domain.SubscriptionStatus;

public record SubscriptionResponse(UUID id, UUID tenantId, String eventType, SubscriptionStatus status, UUID endpointID,
    Instant createdAt) {

  public static SubscriptionResponse from(Subscription subscription) {
    return new SubscriptionResponse(
        subscription.getId(),
        subscription.getTenant().getId(),
        subscription.getEventType(),
        subscription.getStatus(),
        subscription.getEndpoint().getId(),
        subscription.getCreatedAt());
  }
}
