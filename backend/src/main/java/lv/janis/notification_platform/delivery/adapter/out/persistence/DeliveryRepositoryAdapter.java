package lv.janis.notification_platform.delivery.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

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
  public Optional<Delivery> findById(UUID id) {
    return deliveryJpaRepository.findById(id);
  }

  @Override
  public Optional<Delivery> findByTenantIdAndEventIdAndSubscriptionId(UUID tenantId, UUID eventId, UUID subscriptionId) {
    return deliveryJpaRepository.findByTenant_IdAndEvent_IdAndSubscription_Id(tenantId, eventId, subscriptionId);
  }
}
