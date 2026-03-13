package lv.janis.notification_platform.delivery.adapter.out.persistence;

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
import lv.janis.notification_platform.delivery.application.port.out.EndpointFilter;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;
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
class EndpointRepositoryAdapterTest {
  private final EndpointJpaRepository repository = mock(EndpointJpaRepository.class);
  private final EndpointRepositoryAdapter adapter = new EndpointRepositoryAdapter(repository);

  @Test
  void saveAndFindByIdDelegateToRepository() {
    UUID id = UUID.randomUUID();
    Endpoint endpoint = mock(Endpoint.class);
    when(repository.save(endpoint)).thenReturn(endpoint);
    when(repository.findById(id)).thenReturn(Optional.of(endpoint));

    Endpoint saved = adapter.save(endpoint);
    Optional<Endpoint> found = adapter.findById(id);

    assertSame(endpoint, saved);
    assertSame(endpoint, found.orElseThrow());
    verify(repository).save(endpoint);
    verify(repository).findById(id);
  }

  @Test
  void findAllBuildsSpecificationForAllFilters() {
    UUID tenantId = UUID.randomUUID();
    Instant createdFrom = Instant.parse("2026-01-01T00:00:00Z");
    Instant createdTo = Instant.parse("2026-01-02T00:00:00Z");
    Pageable pageable = PageRequest.of(0, 25);
    EndpointFilter filter = new EndpointFilter(tenantId, EndpointStatus.ACTIVE, EndpointType.WEBHOOK, createdFrom, createdTo);
    Page<Endpoint> expected = new PageImpl<>(List.of());
    when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<Endpoint>>any(), eq(pageable))).thenReturn(expected);

    Page<Endpoint> result = adapter.findAll(filter, pageable);

    assertSame(expected, result);

    ArgumentCaptor<Specification<Endpoint>> specCaptor = specificationCaptor();
    verify(repository).findAll(specCaptor.capture(), eq(pageable));

    Specification<Endpoint> spec = specCaptor.getValue();
    Root<Endpoint> root = deepRootMock();
    CriteriaQuery<Endpoint> criteriaQuery = criteriaQuery();
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    Predicate predicate = mock(Predicate.class);
    lenient().when(cb.conjunction()).thenReturn(predicate);
    lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
    lenient().when(cb.equal(any(), any())).thenReturn(predicate);
    lenient().when(cb.greaterThanOrEqualTo(any(), eq(createdFrom))).thenReturn(predicate);
    lenient().when(cb.lessThanOrEqualTo(any(), eq(createdTo))).thenReturn(predicate);

    assertNotNull(spec.toPredicate(root, criteriaQuery, cb));
    verify(cb).conjunction();
    verify(cb).greaterThanOrEqualTo(any(), eq(createdFrom));
    verify(cb).lessThanOrEqualTo(any(), eq(createdTo));
  }

  @Test
  void findByTenantIdAndStatusDelegatesToRepository() {
    UUID tenantId = UUID.randomUUID();
    List<Endpoint> all = List.of(mock(Endpoint.class));
    List<Endpoint> active = List.of(mock(Endpoint.class));
    when(repository.findByTenant_Id(tenantId)).thenReturn(all);
    when(repository.findByTenant_IdAndStatus(tenantId, EndpointStatus.ACTIVE)).thenReturn(active);

    List<Endpoint> byTenant = adapter.findByTenantId(tenantId);
    List<Endpoint> byTenantAndStatus = adapter.findByTenantIdAndStatus(tenantId, EndpointStatus.ACTIVE);

    assertSame(all, byTenant);
    assertSame(active, byTenantAndStatus);
    verify(repository).findByTenant_Id(tenantId);
    verify(repository).findByTenant_IdAndStatus(tenantId, EndpointStatus.ACTIVE);
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<Specification<Endpoint>> specificationCaptor() {
    return (ArgumentCaptor<Specification<Endpoint>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Specification.class);
  }

  @SuppressWarnings("unchecked")
  private static <T> CriteriaQuery<T> criteriaQuery() {
    return (CriteriaQuery<T>) mock(CriteriaQuery.class);
  }

  @SuppressWarnings("unchecked")
  private static Root<Endpoint> deepRootMock() {
    return (Root<Endpoint>) mock(Root.class, Answers.RETURNS_DEEP_STUBS);
  }
}
