package lv.janis.notification_platform.ingest.application.port.out;

import java.util.Optional;
import java.util.UUID;

import lv.janis.notification_platform.ingest.domain.Event;

public interface EventRepositoryPort {
  Event save(Event event);

  Optional<Event> findById(UUID id);

  Optional<Event> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

  Optional<Event> findByIdAndTenantId(UUID id, UUID tenantId);
}
