package lv.janis.notification_platform.outbox.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;

public interface OutboxEventRepositoryPort {
  OutboxEvent save(OutboxEvent event);

  List<OutboxEvent> saveAll(List<OutboxEvent> events);

  Page<OutboxEvent> findAll(OutboxFilter filter, Pageable pageable);

  Optional<OutboxEvent> findById(UUID id);

  Optional<OutboxEvent> findByTenantIdAndAggregateTypeAndAggregateIdAndEventType(
      UUID tenantId,
      OutboxEventAggregateType aggregateType,
      UUID aggregateId,
      OutboxEventType eventType);

  List<OutboxEvent> findReadyToPublish(OutboxStatus status, Instant now, int limit);

  List<OutboxEvent> claimNextBatch(int batchSize, Instant now, Instant staleBefore);
}
