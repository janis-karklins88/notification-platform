package lv.janis.notification_platform.delivery.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointStatus;

public interface EndpointJpaRepository extends JpaRepository<Endpoint, UUID>, JpaSpecificationExecutor<Endpoint> {
  List<Endpoint> findByTenant_Id(UUID tenantId);

  List<Endpoint> findByTenant_IdAndStatus(UUID tenantId, EndpointStatus status);
}
