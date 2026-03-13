package lv.janis.notification_platform.outbox.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;

@ExtendWith(MockitoExtension.class)
class OutboxEventRepositoryAdapterTest {
  private final OutboxEventJpaRepository repository = mock(OutboxEventJpaRepository.class);
  private final OutboxEventRepositoryAdapter adapter = new OutboxEventRepositoryAdapter(repository);

  @Test
  void saveDelegatesToRepository() {
    OutboxEvent event = mock(OutboxEvent.class);
    when(repository.save(event)).thenReturn(event);

    OutboxEvent result = adapter.save(event);

    assertSame(event, result);
    verify(repository).save(event);
  }

  @Test
  void saveAllDelegatesToRepository() {
    List<OutboxEvent> events = List.of(mock(OutboxEvent.class), mock(OutboxEvent.class));
    when(repository.saveAll(events)).thenReturn(events);

    List<OutboxEvent> result = adapter.saveAll(events);

    assertSame(events, result);
    verify(repository).saveAll(events);
  }

  @Test
  void findAllBuildsSpecificationAndDelegates() {
    Pageable pageable = mock(Pageable.class);
    var filter = new lv.janis.notification_platform.outbox.application.port.out.OutboxFilter(
        OutboxStatus.PENDING,
        UUID.randomUUID(),
        OutboxEventType.EVENT_ACCEPTED,
        OutboxEventAggregateType.EVENT,
        UUID.randomUUID(),
        Instant.parse("2026-03-01T00:00:00Z"),
        Instant.parse("2026-03-02T00:00:00Z"));
    Page<OutboxEvent> expected = new PageImpl<>(List.of());
    when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<OutboxEvent>>any(), eq(pageable))).thenReturn(expected);

    Page<OutboxEvent> result = adapter.findAll(filter, pageable);
    assertSame(expected, result);

    ArgumentCaptor<Specification<OutboxEvent>> specCaptor = specificationCaptor();
    verify(repository).findAll(specCaptor.capture(), eq(pageable));

    var spec = specCaptor.getValue();
    Root<OutboxEvent> root = deepRootMock();
    CriteriaQuery<OutboxEvent> query = criteriaQuery();
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    Predicate predicate = mock(Predicate.class);
    when(cb.conjunction()).thenReturn(predicate);

    assertSame(predicate, spec.toPredicate(root, query, cb));
  }

  @Test
  void findAllBuildsSpecificationForDateAndAllFilters() {
    Pageable pageable = mock(Pageable.class);
    UUID tenantId = UUID.randomUUID();
    UUID aggregateId = UUID.randomUUID();
    Instant from = Instant.parse("2026-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-01-02T00:00:00Z");

    var filter = new lv.janis.notification_platform.outbox.application.port.out.OutboxFilter(
        OutboxStatus.FAILED,
        tenantId,
        OutboxEventType.DELIVERY_CREATED_WEBHOOK,
        OutboxEventAggregateType.DELIVERY,
        aggregateId,
        from,
        to);

    Page<OutboxEvent> expected = new PageImpl<>(List.of());
    when(repository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<OutboxEvent>>any(), eq(pageable)))
        .thenReturn(expected);

    Page<OutboxEvent> result = adapter.findAll(filter, pageable);
    assertSame(expected, result);

    ArgumentCaptor<Specification<OutboxEvent>> specCaptor = specificationCaptor();
    verify(repository).findAll(specCaptor.capture(), eq(pageable));

    var spec = specCaptor.getValue();
    Root<OutboxEvent> root = deepRootMock();
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    CriteriaQuery<OutboxEvent> query = criteriaQuery();
    Predicate predicate = mock(Predicate.class);
    lenient().when(cb.conjunction()).thenReturn(predicate);
    lenient().when(cb.equal(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(predicate);
    lenient().when(cb.greaterThanOrEqualTo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(Instant.class)))
        .thenReturn(predicate);
    lenient().when(cb.lessThanOrEqualTo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(Instant.class)))
        .thenReturn(predicate);

    assertSame(predicate, spec.toPredicate(root, query, cb));
    verify(cb).equal(org.mockito.ArgumentMatchers.any(), eq(OutboxStatus.FAILED));
    verify(cb).equal(org.mockito.ArgumentMatchers.any(), eq(tenantId));
    verify(cb).equal(org.mockito.ArgumentMatchers.any(), eq(OutboxEventType.DELIVERY_CREATED_WEBHOOK));
    verify(cb).equal(org.mockito.ArgumentMatchers.any(), eq(OutboxEventAggregateType.DELIVERY));
    verify(cb).equal(org.mockito.ArgumentMatchers.any(), eq(aggregateId));
    verify(cb).greaterThanOrEqualTo(org.mockito.ArgumentMatchers.any(), eq(from));
    verify(cb).lessThanOrEqualTo(org.mockito.ArgumentMatchers.any(), eq(to));
  }

  @Test
  void findByIdDelegatesToRepository() {
    UUID id = UUID.randomUUID();
    OutboxEvent event = mock(OutboxEvent.class);
    when(repository.findById(id)).thenReturn(Optional.of(event));

    Optional<OutboxEvent> result = adapter.findById(id);

    assertEquals(Optional.of(event), result);
    verify(repository).findById(id);
  }

  @Test
  void findByTenantIdAndAggregateTypeAndAggregateIdAndEventTypeDelegates() {
    UUID tenantId = UUID.randomUUID();
    UUID aggregateId = UUID.randomUUID();
    OutboxEventAggregateType aggregateType = OutboxEventAggregateType.EVENT;
    OutboxEventType eventType = OutboxEventType.EVENT_ACCEPTED;
    Optional<OutboxEvent> expected = Optional.of(mock(OutboxEvent.class));

    when(repository.findByTenant_IdAndAggregateTypeAndAggregateIdAndEventType(tenantId, aggregateType, aggregateId, eventType))
        .thenReturn(expected);

    Optional<OutboxEvent> result = adapter.findByTenantIdAndAggregateTypeAndAggregateIdAndEventType(
        tenantId,
        aggregateType,
        aggregateId,
        eventType);

    assertSame(expected, result);
    verify(repository).findByTenant_IdAndAggregateTypeAndAggregateIdAndEventType(
        tenantId,
        aggregateType,
        aggregateId,
        eventType);
  }

  @Test
  void findReadyToPublishDelegatesToRepositoryWithPageRequest() {
    Instant now = Instant.parse("2026-03-01T00:00:00Z");
    int limit = 7;
    List<OutboxEvent> expected = List.of(mock(OutboxEvent.class));

    when(repository.findByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAsc(OutboxStatus.PENDING, now,
        org.springframework.data.domain.PageRequest.of(0, limit)))
        .thenReturn(expected);

    List<OutboxEvent> result = adapter.findReadyToPublish(OutboxStatus.PENDING, now, limit);

    assertSame(expected, result);
    verify(repository).findByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAsc(
        OutboxStatus.PENDING,
        now,
        org.springframework.data.domain.PageRequest.of(0, limit));
  }

  @Test
  void claimNextBatchDelegatesToRepositoryWithEnumNames() {
    Instant now = Instant.parse("2026-03-01T00:00:00Z");
    Instant staleBefore = Instant.parse("2026-02-28T00:00:00Z");
    int batchSize = 5;
    List<OutboxEvent> expected = List.of(mock(OutboxEvent.class));

    when(repository.claimNextBatch(
        OutboxStatus.PENDING.name(),
        OutboxStatus.IN_PROGRESS.name(),
        now,
        staleBefore,
        batchSize)).thenReturn(expected);

    List<OutboxEvent> result = adapter.claimNextBatch(batchSize, now, staleBefore);

    assertSame(expected, result);
    verify(repository).claimNextBatch(
        OutboxStatus.PENDING.name(),
        OutboxStatus.IN_PROGRESS.name(),
        now,
        staleBefore,
        batchSize);
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<Specification<OutboxEvent>> specificationCaptor() {
    return (ArgumentCaptor<Specification<OutboxEvent>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Specification.class);
  }

  @SuppressWarnings("unchecked")
  private static <T> CriteriaQuery<T> criteriaQuery() {
    return (CriteriaQuery<T>) mock(CriteriaQuery.class);
  }

  @SuppressWarnings("unchecked")
  private static Root<OutboxEvent> deepRootMock() {
    return (Root<OutboxEvent>) mock(Root.class, Answers.RETURNS_DEEP_STUBS);
  }
}
