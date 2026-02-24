package lv.janis.notification_platform.routing.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import lv.janis.notification_platform.routing.application.port.out.SubscriptionRepositoryPort;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.routing.domain.SubscriptionStatus;

@Repository
public class SubscriptionRepositoryAdapter implements SubscriptionRepositoryPort {
  private final SubscriptionJpaRepository subscriptionJpaRepository;

  public SubscriptionRepositoryAdapter(SubscriptionJpaRepository subscriptionJpaRepository) {
    this.subscriptionJpaRepository = subscriptionJpaRepository;
  }

  @Override
  public Subscription save(Subscription subscription) {
    return subscriptionJpaRepository.save(subscription);
  }

  @Override
  public Optional<Subscription> findById(UUID id) {
    return subscriptionJpaRepository.findById(id);
  }

  @Override
  public List<Subscription> findByTenantId(UUID tenantId) {
    return subscriptionJpaRepository.findByTenant_Id(tenantId);
  }

  @Override
  public List<Subscription> findActiveByTenantIdAndEventType(UUID tenantId, String eventType) {
    return subscriptionJpaRepository.findByTenant_IdAndEventTypeAndStatus(
        tenantId,
        eventType,
        SubscriptionStatus.ACTIVE
    );
  }
}
