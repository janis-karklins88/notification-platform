package lv.janis.notification_platform.outbox.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = """
            select *
            from outbox_event
            where (
                status = :pendingStatus
                and available_at <= :now
            ) or (
                status = :inProgressStatus
                and last_attempt_at <= :staleBefore
            )
            order by available_at asc, last_attempt_at asc
            for update skip locked
            limit :limit
            """, nativeQuery = true)
    List<OutboxEvent> claimNextBatch(
            @Param("pendingStatus") String pendingStatus,
            @Param("inProgressStatus") String inProgressStatus,
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("limit") int limit);

}
