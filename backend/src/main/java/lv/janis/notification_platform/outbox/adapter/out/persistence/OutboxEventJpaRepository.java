package lv.janis.notification_platform.outbox.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, UUID> {
  Optional<OutboxEvent> findByTenant_IdAndAggregateTypeAndAggregateIdAndEventType(
      UUID tenantId,
      OutboxEventAggregateType aggregateType,
      UUID aggregateId,
      OutboxEventType eventType);

  List<OutboxEvent> findByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAsc(
      OutboxStatus status,
      Instant availableAt,
      Pageable pageable);
}
