package lv.janis.notification_platform.outbox.application.port.in;

import java.time.Instant;
import java.util.List;

import lv.janis.notification_platform.outbox.domain.OutboxEvent;

public interface OutboxClaimUseCase {
  List<OutboxEvent> claim(int batchSize, Instant now);
}
