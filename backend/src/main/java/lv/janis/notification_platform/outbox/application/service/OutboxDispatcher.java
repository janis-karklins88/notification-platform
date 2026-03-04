package lv.janis.notification_platform.outbox.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import lv.janis.notification_platform.outbox.application.port.in.OutboxClaimUseCase;
import lv.janis.notification_platform.outbox.application.port.in.OutboxDispatchUseCase;
import lv.janis.notification_platform.outbox.application.port.in.OutboxFinalizeUseCase;
import lv.janis.notification_platform.outbox.application.port.in.OutboxPublishUseCase;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;

@Service
public class OutboxDispatcher implements OutboxDispatchUseCase {
  private final OutboxClaimUseCase outboxClaimUseCase;
  private final OutboxPublishUseCase outboxPublishUseCase;
  private final OutboxFinalizeUseCase outboxFinalizeUseCase;
  private final OutboxDispatchProperties properties;
  private final Clock clock;

  public OutboxDispatcher(OutboxClaimUseCase outboxClaimUseCase, OutboxPublishUseCase outboxPublishUseCase,
      OutboxFinalizeUseCase outboxFinalizeUseCase, OutboxDispatchProperties properties, Clock clock) {
    this.outboxClaimUseCase = outboxClaimUseCase;
    this.outboxPublishUseCase = outboxPublishUseCase;
    this.outboxFinalizeUseCase = outboxFinalizeUseCase;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public void dispatch() {

    int batchSize = properties.batchSize();
    Instant now = clock.instant();

    List<OutboxEvent> claimed = outboxClaimUseCase.claim(batchSize, now);

    if (claimed.isEmpty()) {
      return;
    }

    for (var event : claimed) {
      try {
        outboxPublishUseCase.publish(event);
        outboxFinalizeUseCase.markPublished(event.getId(), now);
      } catch (Exception ex) {
        Instant next = computeBackoff(now, event.getAttemptCount());
        outboxFinalizeUseCase.reschedule(event.getId(), next, ex.getMessage());
      }
    }

  }

  private Instant computeBackoff(Instant now, int attemptCount) {
    long backoffMillis = (long) (Math.pow(2, attemptCount) * 1000L);
    return now.plusMillis(backoffMillis);
  }

}
