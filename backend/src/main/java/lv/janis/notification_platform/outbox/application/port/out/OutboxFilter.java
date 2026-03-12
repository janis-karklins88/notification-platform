package lv.janis.notification_platform.outbox.application.port.out;

import java.time.Instant;
import java.util.UUID;

import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;

public record OutboxFilter(
    OutboxStatus status,
    UUID tenantId,
    OutboxEventType eventType,
    OutboxEventAggregateType aggregateType,
    UUID aggregateId,
    Instant from,
    Instant to) {
}
