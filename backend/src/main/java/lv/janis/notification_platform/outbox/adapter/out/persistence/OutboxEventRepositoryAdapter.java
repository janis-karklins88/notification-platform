package lv.janis.notification_platform.outbox.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;
import lv.janis.notification_platform.outbox.application.port.out.OutboxFilter;
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
  public Page<OutboxEvent> findAll(OutboxFilter filter, Pageable pageable) {
    Specification<OutboxEvent> spec = (root, query, cb) -> cb.conjunction();

    if (filter.status() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), filter.status()));
    }
    if (filter.tenantId() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("tenant").get("id"), filter.tenantId()));
    }
    if (filter.eventType() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("eventType"), filter.eventType()));
    }
    if (filter.aggregateType() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("aggregateType"), filter.aggregateType()));
    }
    if (filter.aggregateId() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("aggregateId"), filter.aggregateId()));
    }
    if (filter.from() != null) {
      spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
    }
    if (filter.to() != null) {
      spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), filter.to()));
    }

    return outboxEventJpaRepository.findAll(spec, pageable);
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
        org.springframework.data.domain.PageRequest.of(0, limit));
  }

  @Override
  public List<OutboxEvent> claimNextBatch(int batchSize, Instant now, Instant staleBefore) {
    return outboxEventJpaRepository.claimNextBatch(
        OutboxStatus.PENDING.name(),
        OutboxStatus.IN_PROGRESS.name(),
        now,
        staleBefore,
        batchSize);
  }
}
