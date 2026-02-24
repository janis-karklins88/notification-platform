package lv.janis.notification_platform.auth.application.port.out;

import java.util.Optional;
import java.util.UUID;

import lv.janis.notification_platform.auth.domain.ApiKey;

public interface ApiKeyRepositoryPort {
  ApiKey save(ApiKey apiKey);

  Optional<ApiKey> findById(UUID id);

  Optional<ApiKey> findByKeyHash(String keyHash);
}
