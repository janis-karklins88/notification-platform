package lv.janis.notification_platform.routing.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lv.janis.notification_platform.routing.domain.Subscription;

public interface SubscriptionRepositoryPort {
  Subscription save(Subscription subscription);

  Optional<Subscription> findById(UUID id);

  List<Subscription> findByTenantId(UUID tenantId);

  List<Subscription> findActiveByTenantIdAndEventType(UUID tenantId, String eventType);
}
