package lv.janis.notification_platform.adminapi.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;

public record OutboxEventMonitoringResponse(
    UUID id,
    UUID tenantId,
    OutboxEventAggregateType aggregateType,
    UUID aggregateId,
    OutboxEventType eventType,
    OutboxStatus status,
    int attemptCount,
    Instant availableAt,
    Instant lastAttemptAt,
    Instant publishedAt,
    String lastError,
    Instant createdAt,
    Instant updatedAt) {
  public static OutboxEventMonitoringResponse from(OutboxEvent event) {
    return new OutboxEventMonitoringResponse(
        event.getId(),
        event.getTenantId(),
        event.getAggregateType(),
        event.getAggregateId(),
        event.getEventType(),
        event.getStatus(),
        event.getAttemptCount(),
        event.getAvailableAt(),
        event.getLastAttemptAt(),
        event.getPublishedAt(),
        event.getLastError(),
        event.getCreatedAt(),
        event.getUpdatedAt());
  }
}
