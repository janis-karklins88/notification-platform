package lv.janis.notification_platform.routing.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lv.janis.notification_platform.routing.domain.Subscription;

public interface SubscriptionRepositoryPort {
  Subscription save(Subscription subscription);

  Optional<Subscription> findById(UUID id);

  Page<Subscription> findAll(SubscriptionFilter filter, Pageable pageable);

  List<Subscription> findByTenantId(UUID tenantId);

  List<Subscription> findActiveByTenantIdAndEventType(UUID tenantId, String eventType);
}
