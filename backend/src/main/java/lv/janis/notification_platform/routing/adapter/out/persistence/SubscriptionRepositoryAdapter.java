package lv.janis.notification_platform.routing.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import lv.janis.notification_platform.routing.application.port.out.SubscriptionFilter;
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
  public Page<Subscription> findAll(SubscriptionFilter filter, Pageable pageable) {
    Specification<Subscription> spec = (root, query, cb) -> cb.conjunction();

    if (filter.tenantId() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("tenant").get("id"), filter.tenantId()));
    }
    if (StringUtils.hasText(filter.eventType())) {
      String normalized = filter.eventType().trim();
      spec = spec.and((root, q, cb) -> cb.equal(root.get("eventType"), normalized));
    }
    if (filter.endpointId() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("endpoint").get("id"), filter.endpointId()));
    }
    if (filter.status() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), filter.status()));
    }
    if (filter.createdFrom() != null) {
      spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), filter.createdFrom()));
    }
    if (filter.createdTo() != null) {
      spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), filter.createdTo()));
    }

    return subscriptionJpaRepository.findAll(spec, pageable);
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
