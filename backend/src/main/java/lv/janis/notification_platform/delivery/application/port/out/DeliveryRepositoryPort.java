package lv.janis.notification_platform.delivery.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lv.janis.notification_platform.delivery.domain.Delivery;

public interface DeliveryRepositoryPort {
  Delivery save(Delivery delivery);

  List<Delivery> saveAll(List<Delivery> deliveries);

  Optional<Delivery> findById(UUID id);

  Optional<Delivery> findByTenantIdAndEventIdAndSubscriptionId(UUID tenantId, UUID eventId, UUID subscriptionId);
}
