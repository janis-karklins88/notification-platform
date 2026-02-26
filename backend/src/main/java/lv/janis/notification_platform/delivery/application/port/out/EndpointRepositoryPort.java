package lv.janis.notification_platform.delivery.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointStatus;

public interface EndpointRepositoryPort {
  Endpoint save(Endpoint endpoint);

  Optional<Endpoint> findById(UUID id);

  Page<Endpoint> findAll(EndpointFilter filter, Pageable pageable);

  List<Endpoint> findByTenantId(UUID tenantId);

  List<Endpoint> findByTenantIdAndStatus(UUID tenantId, EndpointStatus status);
}
