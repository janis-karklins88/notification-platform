package lv.janis.notification_platform.tenant.adapter.out.persistence;

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
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lv.janis.notification_platform.tenant.application.port.out.TenantFilter;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;
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
class TenantRepositoryAdapterTest {
  private final TenantJpaRepository repository = mock(TenantJpaRepository.class);
  private final TenantRepositoryAdapter adapter = new TenantRepositoryAdapter(repository);

  @Test
  void saveDelegatesToRepository() {
    Tenant tenant = mock(Tenant.class);
    when(repository.save(tenant)).thenReturn(tenant);

    Tenant result = adapter.save(tenant);

    assertSame(tenant, result);
    verify(repository).save(tenant);
  }

  @Test
  void findByIdDelegatesToRepository() {
    UUID id = UUID.randomUUID();
    Optional<Tenant> expected = Optional.of(mock(Tenant.class));
    when(repository.findById(id)).thenReturn(expected);

    Optional<Tenant> result = adapter.findById(id);

    assertSame(expected, result);
    verify(repository).findById(id);
  }

  @Test
  void findBySlugAndExistsBySlugDelegateToRepository() {
    String slug = "tenant-a";
    Optional<Tenant> expected = Optional.of(mock(Tenant.class));
    when(repository.findBySlug(slug)).thenReturn(expected);
    when(repository.existsBySlug(slug)).thenReturn(true);

    Optional<Tenant> found = adapter.findBySlug(slug);
    boolean exists = adapter.existsBySlug(slug);

    assertSame(expected, found);
    org.junit.jupiter.api.Assertions.assertTrue(exists);
    verify(repository).findBySlug(slug);
    verify(repository).existsBySlug(slug);
  }

  @Test
  void findAllBuildsSpecificationForAllFilters() {
    Instant createdFrom = Instant.parse("2026-01-01T00:00:00Z");
    Instant createdTo = Instant.parse("2026-01-02T00:00:00Z");
    Pageable pageable = PageRequest.of(0, 25);
    TenantFilter filter = new TenantFilter(TenantStatus.ACTIVE, "Acme", createdFrom, createdTo);
    Page<Tenant> expected = new PageImpl<>(List.of());
    when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<Tenant>>any(), eq(pageable))).thenReturn(expected);

    Page<Tenant> result = adapter.findAll(filter, pageable);

    assertSame(expected, result);

    ArgumentCaptor<Specification<Tenant>> specCaptor = specificationCaptor();
    verify(repository).findAll(specCaptor.capture(), eq(pageable));

    Specification<Tenant> spec = specCaptor.getValue();
    Root<Tenant> root = deepRootMock();
    CriteriaQuery<Tenant> criteriaQuery = criteriaQuery();
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    Predicate predicate = mock(Predicate.class);
    lenient().when(cb.conjunction()).thenReturn(predicate);
    lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
    lenient().when(cb.equal(any(), any())).thenReturn(predicate);
    lenient().when(cb.lower(any())).thenReturn(stringExpressionMock());
    lenient().when(cb.like(any(), eq("%acme%"))).thenReturn(predicate);
    lenient().when(cb.greaterThanOrEqualTo(any(), eq(createdFrom))).thenReturn(predicate);
    lenient().when(cb.lessThanOrEqualTo(any(), eq(createdTo))).thenReturn(predicate);

    assertNotNull(spec.toPredicate(root, criteriaQuery, cb));
    verify(cb).conjunction();
    verify(cb).like(any(), eq("%acme%"));
    verify(cb).greaterThanOrEqualTo(any(), eq(createdFrom));
    verify(cb).lessThanOrEqualTo(any(), eq(createdTo));
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<Specification<Tenant>> specificationCaptor() {
    return (ArgumentCaptor<Specification<Tenant>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Specification.class);
  }

  @SuppressWarnings("unchecked")
  private static <T> CriteriaQuery<T> criteriaQuery() {
    return (CriteriaQuery<T>) mock(CriteriaQuery.class);
  }

  @SuppressWarnings("unchecked")
  private static Root<Tenant> deepRootMock() {
    return (Root<Tenant>) mock(Root.class, Answers.RETURNS_DEEP_STUBS);
  }

  @SuppressWarnings("unchecked")
  private static Expression<String> stringExpressionMock() {
    return (Expression<String>) mock(Expression.class);
  }
}
