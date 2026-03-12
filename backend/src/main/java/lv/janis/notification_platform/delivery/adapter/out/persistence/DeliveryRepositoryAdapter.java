package lv.janis.notification_platform.delivery.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import lv.janis.notification_platform.delivery.application.port.out.DeliveryFilter;
import lv.janis.notification_platform.delivery.application.port.out.DeliveryRepositoryPort;
import lv.janis.notification_platform.delivery.domain.Delivery;

@Repository
public class DeliveryRepositoryAdapter implements DeliveryRepositoryPort {
  private final DeliveryJpaRepository deliveryJpaRepository;

  public DeliveryRepositoryAdapter(DeliveryJpaRepository deliveryJpaRepository) {
    this.deliveryJpaRepository = deliveryJpaRepository;
  }

  @Override
  public Delivery save(Delivery delivery) {
    return deliveryJpaRepository.save(delivery);
  }

  @Override
  public List<Delivery> saveAll(List<Delivery> deliveries) {
    return deliveryJpaRepository.saveAll(deliveries);
  }

  @Override
  public Page<Delivery> findAll(DeliveryFilter filter, Pageable pageable) {
    Specification<Delivery> spec = (root, query, cb) -> {
      if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
        root.fetch("endpoint", JoinType.LEFT);
        query.distinct(true);
      }
      return cb.conjunction();
    };

    if (filter.status() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), filter.status()));
    }
    if (filter.tenantId() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("tenant").get("id"), filter.tenantId()));
    }
    if (filter.eventId() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("event").get("id"), filter.eventId()));
    }
    if (filter.endpointId() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("endpoint").get("id"), filter.endpointId()));
    }
    if (filter.channel() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("endpoint").get("type"), filter.channel()));
    }
    if (filter.from() != null) {
      spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
    }
    if (filter.to() != null) {
      spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), filter.to()));
    }

    return deliveryJpaRepository.findAll(spec, pageable);
  }

  @Override
  public Optional<Delivery> findById(UUID id) {
    return deliveryJpaRepository.findByIdWithEndpoint(id);
  }

  @Override
  public Optional<Delivery> findByTenantIdAndEventIdAndSubscriptionId(UUID tenantId, UUID eventId, UUID subscriptionId) {
    return deliveryJpaRepository.findByTenant_IdAndEvent_IdAndSubscription_Id(tenantId, eventId, subscriptionId);
  }
}
