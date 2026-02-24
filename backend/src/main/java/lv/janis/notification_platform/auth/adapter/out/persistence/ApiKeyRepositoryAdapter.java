package lv.janis.notification_platform.auth.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import lv.janis.notification_platform.auth.application.port.out.ApiKeyRepositoryPort;
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
}
