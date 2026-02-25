package lv.janis.notification_platform.auth.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import lv.janis.notification_platform.auth.domain.ApiKey;

public interface ApiKeyJpaRepository extends JpaRepository<ApiKey, UUID>, JpaSpecificationExecutor<ApiKey> {
  Optional<ApiKey> findByKeyHash(String keyHash);
}
