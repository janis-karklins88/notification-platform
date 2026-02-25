package lv.janis.notification_platform.auth.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import lv.janis.notification_platform.auth.application.port.out.ApiKeyRepositoryPort;
import lv.janis.notification_platform.auth.application.port.out.ListApiKeyQuery;
import lv.janis.notification_platform.auth.domain.ApiKey;

@Repository
public class ApiKeyRepositoryAdapter implements ApiKeyRepositoryPort {
  private final ApiKeyJpaRepository apiKeyJpaRepository;

  public ApiKeyRepositoryAdapter(ApiKeyJpaRepository apiKeyJpaRepository) {
    this.apiKeyJpaRepository = apiKeyJpaRepository;
  }

  @Override
  public ApiKey save(ApiKey apiKey) {
    return apiKeyJpaRepository.save(apiKey);
  }

  @Override
  public Optional<ApiKey> findById(UUID id) {
    return apiKeyJpaRepository.findById(id);
  }

  @Override
  public Optional<ApiKey> findByKeyHash(String keyHash) {
    return apiKeyJpaRepository.findByKeyHash(keyHash);
  }

  @Override
  public Page<ApiKey> findAll(ListApiKeyQuery query, Pageable pageable) {
    Specification<ApiKey> spec = (root, q, cb) -> cb.conjunction();

    if (query.tenantId() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("tenant").get("id"), query.tenantId()));
    }
    if (query.status() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), query.status()));
    }
    if (StringUtils.hasText(query.prefix())) {
      String normalized = query.prefix().trim().toLowerCase();
      String pattern = normalized + "%";
      spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("keyPrefix")), pattern));
    }
    if (query.createdFrom() != null) {
      spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), query.createdFrom()));
    }
    if (query.createdTo() != null) {
      spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), query.createdTo()));
    }

    return apiKeyJpaRepository.findAll(spec, pageable);
  }
}
