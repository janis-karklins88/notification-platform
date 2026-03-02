package lv.janis.notification_platform.ingest.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import lv.janis.notification_platform.ingest.application.port.out.EventRepositoryPort;
import lv.janis.notification_platform.ingest.domain.Event;

@Repository
public class EventRepositoryAdapter implements EventRepositoryPort {
  private final EventJpaRepository eventJpaRepository;

  public EventRepositoryAdapter(EventJpaRepository eventJpaRepository) {
    this.eventJpaRepository = eventJpaRepository;
  }

  @Override
  public Event save(Event event) {
    return eventJpaRepository.save(event);
  }

  @Override
  public Optional<Event> findById(UUID id) {
    return eventJpaRepository.findById(id);
  }

  @Override
  public Optional<Event> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey) {
    return eventJpaRepository.findByTenant_IdAndIdempotencyKey(tenantId, idempotencyKey);
  }

  @Override
  public Optional<Event> findByIdAndTenantId(UUID id, UUID tenantId) {
    return eventJpaRepository.findByIdAndTenant_Id(id, tenantId);
  }
}
