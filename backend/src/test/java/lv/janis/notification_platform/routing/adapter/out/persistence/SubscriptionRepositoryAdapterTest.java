package lv.janis.notification_platform.routing.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
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
import lv.janis.notification_platform.routing.application.port.out.SubscriptionFilter;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.routing.domain.SubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SubscriptionRepositoryAdapterTest {
  private final SubscriptionJpaRepository repository = mock(SubscriptionJpaRepository.class);
  private final SubscriptionRepositoryAdapter adapter = new SubscriptionRepositoryAdapter(repository);

  @Test
  void saveAndFindByIdDelegateToRepository() {
    UUID id = UUID.randomUUID();
    Subscription subscription = mock(Subscription.class);
    when(repository.save(subscription)).thenReturn(subscription);
    when(repository.findById(id)).thenReturn(Optional.of(subscription));

    Subscription saved = adapter.save(subscription);
    Optional<Subscription> found = adapter.findById(id);

    assertSame(subscription, saved);
    assertSame(subscription, found.orElseThrow());
    verify(repository).save(subscription);
    verify(repository).findById(id);
  }

  @Test
  void findAllBuildsSpecificationForAllFilters() {
    UUID tenantId = UUID.randomUUID();
    UUID endpointId = UUID.randomUUID();
    Instant createdFrom = Instant.parse("2026-01-01T00:00:00Z");
    Instant createdTo = Instant.parse("2026-01-02T00:00:00Z");
    Pageable pageable = PageRequest.of(0, 25);
    SubscriptionFilter filter = new SubscriptionFilter(
        tenantId,
        "  order.created  ",
        endpointId,
        SubscriptionStatus.ACTIVE,
        createdFrom,
        createdTo);
    Page<Subscription> expected = new PageImpl<>(List.of());
    when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<Subscription>>any(), eq(pageable))).thenReturn(expected);

    Page<Subscription> result = adapter.findAll(filter, pageable);

    assertSame(expected, result);

    ArgumentCaptor<Specification<Subscription>> specCaptor = specificationCaptor();
    verify(repository).findAll(specCaptor.capture(), eq(pageable));

    Specification<Subscription> spec = specCaptor.getValue();
    Root<Subscription> root = deepRootMock();
    CriteriaQuery<Subscription> criteriaQuery = criteriaQuery();
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    Predicate predicate = mock(Predicate.class);
    lenient().when(cb.conjunction()).thenReturn(predicate);
    lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
    lenient().when(cb.equal(any(), any())).thenReturn(predicate);
    lenient().when(cb.greaterThanOrEqualTo(any(), eq(createdFrom))).thenReturn(predicate);
    lenient().when(cb.lessThanOrEqualTo(any(), eq(createdTo))).thenReturn(predicate);

    assertNotNull(spec.toPredicate(root, criteriaQuery, cb));
    verify(cb).conjunction();
    verify(cb).equal(any(), eq("order.created"));
    verify(cb).greaterThanOrEqualTo(any(), eq(createdFrom));
    verify(cb).lessThanOrEqualTo(any(), eq(createdTo));
  }

  @Test
  void findByTenantIdAndFindActiveByTenantIdAndEventTypeDelegateToRepository() {
    UUID tenantId = UUID.randomUUID();
    String eventType = "order.created";
    List<Subscription> all = List.of(mock(Subscription.class));
    List<Subscription> active = List.of(mock(Subscription.class));
    when(repository.findByTenant_Id(tenantId)).thenReturn(all);
    when(repository.findByTenant_IdAndEventTypeAndStatus(tenantId, eventType, SubscriptionStatus.ACTIVE)).thenReturn(active);

    List<Subscription> byTenant = adapter.findByTenantId(tenantId);
    List<Subscription> byTenantEventActive = adapter.findActiveByTenantIdAndEventType(tenantId, eventType);

    assertSame(all, byTenant);
    assertSame(active, byTenantEventActive);
    verify(repository).findByTenant_Id(tenantId);
    verify(repository).findByTenant_IdAndEventTypeAndStatus(tenantId, eventType, SubscriptionStatus.ACTIVE);
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<Specification<Subscription>> specificationCaptor() {
    return (ArgumentCaptor<Specification<Subscription>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Specification.class);
  }

  @SuppressWarnings("unchecked")
  private static <T> CriteriaQuery<T> criteriaQuery() {
    return (CriteriaQuery<T>) mock(CriteriaQuery.class);
  }

  @SuppressWarnings("unchecked")
  private static Root<Subscription> deepRootMock() {
    return (Root<Subscription>) mock(Root.class, Answers.RETURNS_DEEP_STUBS);
  }
}
