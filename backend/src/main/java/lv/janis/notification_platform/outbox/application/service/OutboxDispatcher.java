package lv.janis.notification_platform.outbox.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.outbox.application.port.in.OutboxClaimUseCase;
import lv.janis.notification_platform.outbox.application.port.in.OutboxDispatchUseCase;
import lv.janis.notification_platform.outbox.application.port.in.OutboxFinalizeUseCase;
import lv.janis.notification_platform.outbox.application.port.in.OutboxPublishUseCase;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.shared.metrics.NotificationMetrics;

@Service
public class OutboxDispatcher implements OutboxDispatchUseCase {
  private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

  private final OutboxClaimUseCase outboxClaimUseCase;
  private final OutboxPublishUseCase outboxPublishUseCase;
  private final OutboxFinalizeUseCase outboxFinalizeUseCase;
  private final OutboxDispatchProperties properties;
  private final Clock clock;
  private final NotificationMetrics notificationMetrics;

  public OutboxDispatcher(OutboxClaimUseCase outboxClaimUseCase, OutboxPublishUseCase outboxPublishUseCase,
      OutboxFinalizeUseCase outboxFinalizeUseCase, OutboxDispatchProperties properties, Clock clock,
      NotificationMetrics notificationMetrics) {
    this.outboxClaimUseCase = outboxClaimUseCase;
    this.outboxPublishUseCase = outboxPublishUseCase;
    this.outboxFinalizeUseCase = outboxFinalizeUseCase;
    this.properties = properties;
    this.clock = clock;
    this.notificationMetrics = notificationMetrics;
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
      Instant nowPerEvent = clock.instant();
      try {
        outboxPublishUseCase.publish(event);
      } catch (Exception ex) {
        handlePublishFailure(event, nowPerEvent, ex);
        continue;
      }
      notificationMetrics.incrementOutboxPublished();
      log.info("Outbox event published outboxEventId={} tenantId={} attempt={}", event.getId(), event.getTenantId(),
          event.getAttemptCount());

      markPublishedWithRetry(event, nowPerEvent);
    }

  }

  private void handlePublishFailure(OutboxEvent event, Instant now, Exception ex) {
    if (event.getAttemptCount() >= properties.maxAttempts()) {
      notificationMetrics.incrementOutboxPublishFailed();
      log.warn("Outbox event publish failed permanently outboxEventId={} tenantId={} attempt={} reason={}", event.getId(),
          event.getTenantId(), event.getAttemptCount(), ex.getMessage());
      outboxFinalizeUseCase.markFailed(event.getId(), now, ex.getMessage());
      return;
    }
    Instant next = computeBackoff(now, event.getAttemptCount());
    notificationMetrics.incrementOutboxRescheduled();
    log.info("Outbox event rescheduled outboxEventId={} tenantId={} attempt={} nextAttemptAt={}", event.getId(),
        event.getTenantId(), event.getAttemptCount(), next);
    outboxFinalizeUseCase.reschedule(event.getId(), next, ex.getMessage());
  }

  private Instant computeBackoff(Instant now, int attemptCount) {
    long backoffMillis = (long) (Math.pow(2, attemptCount) * 1000L);
    return now.plusMillis(backoffMillis);
  }

  private void markPublishedWithRetry(OutboxEvent event, Instant publishedAt) {
    int attempts = Math.max(1, properties.finalizeRetryAttempts());

    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        outboxFinalizeUseCase.markPublished(event.getId(), publishedAt);
        return;
      } catch (Exception ex) {
        boolean retryable = isRetryableFinalizeFailure(ex);
        if (!retryable || attempt == attempts) {
          log.error(
              "Failed to mark outbox event {} as published after {} attempt(s); it will be reclaimed as stale IN_PROGRESS",
              event.getId(), attempt, ex);
          return;
        }
        sleepBeforeFinalizeRetry(event.getId(), attempt);
      }
    }
  }

  private void sleepBeforeFinalizeRetry(UUID outboxEventId, int attempt) {
    long delayMs = Math.max(0L, properties.finalizeRetryDelayMs());
    if (delayMs == 0L) {
      return;
    }
    try {
      Thread.sleep(delayMs);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.warn("Interrupted during markPublished retry sleep for outbox event {} at attempt {}",
          outboxEventId, attempt);
    }
  }

  private boolean isRetryableFinalizeFailure(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof InterruptedException) {
        Thread.currentThread().interrupt();
        return false;
      }
      if (current instanceof NotFoundException || current instanceof IllegalArgumentException) {
        return false;
      }
      current = current.getCause();
    }
    return true;
  }

}
