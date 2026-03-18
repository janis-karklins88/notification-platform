package lv.janis.notification_platform.auth.adapter.out.persistence;

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
import lv.janis.notification_platform.auth.application.port.out.ListApiKeyQuery;
import lv.janis.notification_platform.auth.domain.ApiKey;
import lv.janis.notification_platform.auth.domain.ApiKeyStatus;
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
class ApiKeyRepositoryAdapterTest {
  private final ApiKeyJpaRepository repository = mock(ApiKeyJpaRepository.class);
  private final ApiKeyRepositoryAdapter adapter = new ApiKeyRepositoryAdapter(repository);

  @Test
  void saveDelegatesToRepository() {
    ApiKey apiKey = mock(ApiKey.class);
    when(repository.save(apiKey)).thenReturn(apiKey);

    ApiKey result = adapter.save(apiKey);

    assertSame(apiKey, result);
    verify(repository).save(apiKey);
  }

  @Test
  void findByIdDelegatesToRepository() {
    UUID id = UUID.randomUUID();
    Optional<ApiKey> expected = Optional.of(mock(ApiKey.class));
    when(repository.findById(id)).thenReturn(expected);

    Optional<ApiKey> result = adapter.findById(id);

    assertSame(expected, result);
    verify(repository).findById(id);
  }

  @Test
  void findByKeyHashDelegatesToRepository() {
    String keyHash = "hash";
    Optional<ApiKey> expected = Optional.of(mock(ApiKey.class));
    when(repository.findByKeyHash(keyHash)).thenReturn(expected);

    Optional<ApiKey> result = adapter.findByKeyHash(keyHash);

    assertSame(expected, result);
    verify(repository).findByKeyHash(keyHash);
  }

  @Test
  void findAllBuildsSpecificationForAllFilters() {
    UUID tenantId = UUID.randomUUID();
    Instant createdFrom = Instant.parse("2026-01-01T00:00:00Z");
    Instant createdTo = Instant.parse("2026-01-02T00:00:00Z");
    Pageable pageable = PageRequest.of(0, 20);
    ListApiKeyQuery query = new ListApiKeyQuery(
        0,
        20,
        tenantId,
        ApiKeyStatus.ACTIVE,
        "  preFix ",
        createdFrom,
        createdTo);
    Page<ApiKey> expected = new PageImpl<>(List.of());
    when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<ApiKey>>any(), eq(pageable))).thenReturn(expected);

    Page<ApiKey> result = adapter.findAll(query, pageable);

    assertSame(expected, result);

    ArgumentCaptor<Specification<ApiKey>> specCaptor = specificationCaptor();
    verify(repository).findAll(specCaptor.capture(), eq(pageable));

    Specification<ApiKey> spec = specCaptor.getValue();
    Root<ApiKey> root = deepRootMock();
    CriteriaQuery<ApiKey> criteriaQuery = criteriaQuery();
    CriteriaBuilder cb = mock(CriteriaBuilder.class);
    Predicate predicate = mock(Predicate.class);
    lenient().when(cb.conjunction()).thenReturn(predicate);
    lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
    lenient().when(cb.equal(any(), any())).thenReturn(predicate);
    lenient().when(cb.lower(any())).thenReturn(stringExpressionMock());
    lenient().when(cb.like(any(), eq("prefix%"))).thenReturn(predicate);
    lenient().when(cb.greaterThanOrEqualTo(any(), eq(createdFrom))).thenReturn(predicate);
    lenient().when(cb.lessThanOrEqualTo(any(), eq(createdTo))).thenReturn(predicate);

    assertNotNull(spec.toPredicate(root, criteriaQuery, cb));
    verify(cb).conjunction();
    verify(cb).like(any(), eq("prefix%"));
    verify(cb).greaterThanOrEqualTo(any(), eq(createdFrom));
    verify(cb).lessThanOrEqualTo(any(), eq(createdTo));
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCaptor<Specification<ApiKey>> specificationCaptor() {
    return (ArgumentCaptor<Specification<ApiKey>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Specification.class);
  }

  @SuppressWarnings("unchecked")
  private static <T> CriteriaQuery<T> criteriaQuery() {
    return (CriteriaQuery<T>) mock(CriteriaQuery.class);
  }

  @SuppressWarnings("unchecked")
  private static Root<ApiKey> deepRootMock() {
    return (Root<ApiKey>) mock(Root.class, Answers.RETURNS_DEEP_STUBS);
  }

  @SuppressWarnings("unchecked")
  private static Expression<String> stringExpressionMock() {
    return (Expression<String>) mock(Expression.class);
  }
}
