package lv.janis.notification_platform.routing.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.routing.domain.SubscriptionStatus;

public interface SubscriptionJpaRepository extends JpaRepository<Subscription, UUID> {
  List<Subscription> findByTenant_Id(UUID tenantId);

  List<Subscription> findByTenant_IdAndEventTypeAndStatus(
      UUID tenantId,
      String eventType,
      SubscriptionStatus status
  );
}
