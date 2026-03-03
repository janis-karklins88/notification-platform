package lv.janis.notification_platform.delivery.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import lv.janis.notification_platform.delivery.domain.Delivery;

public interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID> {
  Optional<Delivery> findByTenant_IdAndEvent_IdAndSubscription_Id(UUID tenantId, UUID eventId, UUID subscriptionId);
}
