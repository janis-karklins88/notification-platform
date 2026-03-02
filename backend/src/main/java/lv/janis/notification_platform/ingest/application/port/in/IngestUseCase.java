package lv.janis.notification_platform.ingest.application.port.in;

import java.util.UUID;

import lv.janis.notification_platform.ingest.domain.Event;

public interface IngestUseCase {

  IngestResult ingest(IngestCommand command);

  Event getEventById(UUID tenantId, UUID eventId);
}
