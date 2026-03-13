package lv.janis.notification_platform.delivery.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import lv.janis.notification_platform.delivery.application.port.out.DeliveryFilter;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.DeliveryStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.Answers;

@ExtendWith(MockitoExtension.class)
class DeliveryRepositoryAdapterTest {

  private final DeliveryJpaRepository repository = mock(DeliveryJpaRepository.class);
  private final DeliveryRepositoryAdapter adapter = new DeliveryRepositoryAdapter(repository);

  @Test
  void saveDelegatesToRepository() {
    Delivery delivery = mock(Delivery.class);
    when(repository.save(delivery)).thenReturn(delivery);

    Delivery result = adapter.save(delivery);

    assertSame(delivery, result);
    verify(repository).save(delivery);
  }

  @Test
  void saveAllDelegatesToRepository() {
    List<Delivery> deliveries = List.of(mock(Delivery.class), mock(Delivery.class));
    when(repository.saveAll(deliveries)).thenReturn(deliveries);

    List<Delivery> result = adapter.saveAll(deliveries);

    assertSame(deliveries, result);
    verify(repository).saveAll(deliveries);
  }

  @Test
  void findAllBuildsSpecificationForRegularQueries() {
    Pageable pageable = PageRequest.of(1, 25);
    DeliveryFilter filter = new DeliveryFilter(
        DeliveryStatus.PENDING,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        EndpointType.WEBHOOK,
        null,
        null);
    Page<Delivery> expected = new PageImpl<>(List.of());

    when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<Delivery>>any(), eq(pageable))).thenReturn(expected);

    Page<Delivery> result = adapter.findAll(filter, pageable);
    assertSame(expected, result);

    ArgumentCaptor<Specification<Delivery>> specCaptor = specificationCaptor();
    verify(repository).findAll(specCaptor.capture(), eq(pageable));

    var spec = specCaptor.getValue();
    CriteriaQuery<Delivery> query = criteriaQuery();
    when(query.getResultType()).thenReturn(Delivery.class);
    Root<Delivery> root = deepRootMock();
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    when(cb.conjunction()).thenReturn(mock(Predicate.class));

    assertNotNull(spec.toPredicate(root, query, cb));
    verify(root).fetch("endpoint", JoinType.LEFT);
  }

  @Test
  void findAllBuildsSpecificationForDateAndAllFilters() {
    Pageable pageable = PageRequest.of(0, 25);
    Instant from = Instant.parse("2026-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-01-02T00:00:00Z");
    DeliveryFilter filter = new DeliveryFilter(
        DeliveryStatus.FAILED,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        EndpointType.SMS,
        from,
        to);
    Page<Delivery> expected = new PageImpl<>(List.of());
    when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<Delivery>>any(), eq(pageable))).thenReturn(expected);

    Page<Delivery> result = adapter.findAll(filter, pageable);
    assertSame(expected, result);

    ArgumentCaptor<Specification<Delivery>> specCaptor = specificationCaptor();
    verify(repository).findAll(specCaptor.capture(), eq(pageable));

    var spec = specCaptor.getValue();
    Root<Delivery> root = deepRootMock();
    CriteriaQuery<Delivery> query = criteriaQuery();
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    Predicate predicate = mock(Predicate.class);
    lenient().when(cb.conjunction()).thenReturn(predicate);
    lenient().when(cb.equal(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(predicate);
    lenient().when(cb.greaterThanOrEqualTo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(Instant.class))).thenReturn(
        predicate);
    lenient().when(cb.lessThanOrEqualTo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(Instant.class))).thenReturn(
        predicate);

    assertNotNull(spec.toPredicate(root, query, cb));
    verify(cb).conjunction();
    verify(cb).greaterThanOrEqualTo(org.mockito.ArgumentMatchers.any(), eq(from));
    verify(cb).lessThanOrEqualTo(org.mockito.ArgumentMatchers.any(), eq(to));
    verify(root).fetch("endpoint", JoinType.LEFT);
  }

  @Test
  void findAllSkipsFetchForCountQuery() {
    Pageable pageable = PageRequest.of(0, 10);
    when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<Delivery>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

    adapter.findAll(new DeliveryFilter(null, null, null, null, null, null, null), pageable);

    ArgumentCaptor<Specification<Delivery>> specCaptor = specificationCaptor();
    verify(repository).findAll(specCaptor.capture(), eq(pageable));

    var spec = specCaptor.getValue();
    Root<Delivery> root = deepRootMock();
    CriteriaQuery<Long> query = criteriaQuery();
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    when(query.getResultType()).thenReturn(Long.class);
    when(cb.conjunction()).thenReturn(mock(Predicate.class));

    spec.toPredicate(root, query, cb);

    verify(root, never()).fetch("endpoint", JoinType.LEFT);
  }

  @Test
  void findByIdUsesEndpointFetch() {
    UUID id = UUID.randomUUID();
    Delivery delivery = mock(Delivery.class);
    when(repository.findByIdWithEndpoint(id)).thenReturn(Optional.of(delivery));

    Optional<Delivery> result = adapter.findById(id);

    assertEquals(Optional.of(delivery), result);
    verify(repository).findByIdWithEndpoint(id);
  }

  @Test
  void findByTenantIdAndEventIdAndSubscriptionIdDelegates() {
    UUID tenantId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    UUID subscriptionId = UUID.randomUUID();
    Optional<Delivery> expected = Optional.of(mock(Delivery.class));

    when(repository.findByTenant_IdAndEvent_IdAndSubscription_Id(tenantId, eventId, subscriptionId))
        .thenReturn(expected);

    Optional<Delivery> result = adapter.findByTenantIdAndEventIdAndSubscriptionId(tenantId, eventId, subscriptionId);

    assertSame(expected, result);
    verify(repository).findByTenant_IdAndEvent_IdAndSubscription_Id(tenantId, eventId, subscriptionId);
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<Specification<Delivery>> specificationCaptor() {
    return (ArgumentCaptor<Specification<Delivery>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Specification.class);
  }

  @SuppressWarnings("unchecked")
  private static <T> CriteriaQuery<T> criteriaQuery() {
    return (CriteriaQuery<T>) mock(CriteriaQuery.class);
  }

  @SuppressWarnings("unchecked")
  private static Root<Delivery> deepRootMock() {
    return (Root<Delivery>) mock(Root.class, Answers.RETURNS_DEEP_STUBS);
  }
}
