package lv.janis.notification_platform.delivery.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import lv.janis.notification_platform.delivery.domain.Delivery;

public interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID>, JpaSpecificationExecutor<Delivery> {
  Optional<Delivery> findByTenant_IdAndEvent_IdAndSubscription_Id(UUID tenantId, UUID eventId, UUID subscriptionId);

  @Query("select d from Delivery d join fetch d.endpoint e where d.id = :id")
  Optional<Delivery> findByIdWithEndpoint(@Param("id") UUID id);
}
