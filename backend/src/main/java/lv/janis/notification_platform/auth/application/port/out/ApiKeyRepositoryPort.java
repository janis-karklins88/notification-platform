package lv.janis.notification_platform.auth.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lv.janis.notification_platform.auth.domain.ApiKey;

public interface ApiKeyRepositoryPort {
  ApiKey save(ApiKey apiKey);

  Optional<ApiKey> findById(UUID id);

  Optional<ApiKey> findByKeyHash(String keyHash);

  Page<ApiKey> findAll(ListApiKeyQuery query, Pageable pageable);
}
