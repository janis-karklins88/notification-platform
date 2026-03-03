package lv.janis.notification_platform.ingest.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.ingest.domain.EventStatus;

public interface EventJpaRepository extends JpaRepository<Event, UUID> {
  Optional<Event> findByTenant_IdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

  Optional<Event> findByIdAndTenant_Id(UUID id, UUID tenantId);

  List<Event> findTopNByStatus(EventStatus status, int n);
}
