package lv.janis.notification_platform.outbox.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface OutboxFinalizeUseCase {
  void markPublished(UUID outboxEventId, Instant publishedAt);

  void reschedule(UUID outboxEventId, Instant nextAttemptAt, String errorMessage);
}
