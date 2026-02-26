package lv.janis.notification_platform.delivery.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import lv.janis.notification_platform.delivery.application.port.out.EndpointFilter;
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
  public Page<Endpoint> findAll(EndpointFilter filter, Pageable pageable) {
    Specification<Endpoint> spec = (root, query, cb) -> cb.conjunction();

    if (filter.tenantId() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("tenant").get("id"), filter.tenantId()));
    }
    if (filter.status() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), filter.status()));
    }
    if (filter.type() != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("type"), filter.type()));
    }
    if (filter.createdFrom() != null) {
      spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), filter.createdFrom()));
    }
    if (filter.createdTo() != null) {
      spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), filter.createdTo()));
    }

    return endpointJpaRepository.findAll(spec, pageable);
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
