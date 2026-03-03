package lv.janis.notification_platform.outbox.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;

@Repository
public class OutboxEventRepositoryAdapter implements OutboxEventRepositoryPort {
  private final OutboxEventJpaRepository outboxEventJpaRepository;

  public OutboxEventRepositoryAdapter(OutboxEventJpaRepository outboxEventJpaRepository) {
    this.outboxEventJpaRepository = outboxEventJpaRepository;
  }

  @Override
  public OutboxEvent save(OutboxEvent event) {
    return outboxEventJpaRepository.save(event);
  }

  @Override
  public List<OutboxEvent> saveAll(List<OutboxEvent> events) {
    return outboxEventJpaRepository.saveAll(events);
  }

  @Override
  public Optional<OutboxEvent> findById(UUID id) {
    return outboxEventJpaRepository.findById(id);
  }

  @Override
  public Optional<OutboxEvent> findByTenantIdAndAggregateTypeAndAggregateIdAndEventType(
      UUID tenantId,
      OutboxEventAggregateType aggregateType,
      UUID aggregateId,
      OutboxEventType eventType) {
    return outboxEventJpaRepository.findByTenant_IdAndAggregateTypeAndAggregateIdAndEventType(
        tenantId,
        aggregateType,
        aggregateId,
        eventType);
  }

  @Override
  public List<OutboxEvent> findReadyToPublish(OutboxStatus status, Instant now, int limit) {
    return outboxEventJpaRepository.findByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAsc(
        status,
        now,
        PageRequest.of(0, limit));
  }
}
