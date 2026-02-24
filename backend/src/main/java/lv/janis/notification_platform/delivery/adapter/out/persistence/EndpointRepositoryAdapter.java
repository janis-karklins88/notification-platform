package lv.janis.notification_platform.delivery.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import lv.janis.notification_platform.delivery.application.port.out.EndpointRepositoryPort;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointStatus;

@Repository
public class EndpointRepositoryAdapter implements EndpointRepositoryPort {
  private final EndpointJpaRepository endpointJpaRepository;

  public EndpointRepositoryAdapter(EndpointJpaRepository endpointJpaRepository) {
    this.endpointJpaRepository = endpointJpaRepository;
  }

  @Override
  public Endpoint save(Endpoint endpoint) {
    return endpointJpaRepository.save(endpoint);
  }

  @Override
  public Optional<Endpoint> findById(UUID id) {
    return endpointJpaRepository.findById(id);
  }

  @Override
  public List<Endpoint> findByTenantId(UUID tenantId) {
    return endpointJpaRepository.findByTenant_Id(tenantId);
  }

  @Override
  public List<Endpoint> findByTenantIdAndStatus(UUID tenantId, EndpointStatus status) {
    return endpointJpaRepository.findByTenant_IdAndStatus(tenantId, status);
  }
}
