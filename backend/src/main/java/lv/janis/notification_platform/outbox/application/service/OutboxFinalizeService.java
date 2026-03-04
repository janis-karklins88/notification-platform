package lv.janis.notification_platform.outbox.application.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.outbox.application.port.in.OutboxFinalizeUseCase;
import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;

@Service
@Transactional
public class OutboxFinalizeService implements OutboxFinalizeUseCase {
  private final OutboxEventRepositoryPort outboxEventRepositoryPort;

  public OutboxFinalizeService(OutboxEventRepositoryPort outboxEventRepositoryPort) {
    this.outboxEventRepositoryPort = outboxEventRepositoryPort;
  }

  @Override
  public void markPublished(UUID outboxEventId, Instant publishedAt) {
    var event = outboxEventRepositoryPort.findById(outboxEventId)
        .orElseThrow(() -> new NotFoundException("Outbox event not found: " + outboxEventId));
    event.markPublished(publishedAt);
    outboxEventRepositoryPort.save(event);
  }

  @Override
  public void reschedule(UUID outboxEventId, Instant nextAttemptAt, String errorMessage) {
    var event = outboxEventRepositoryPort.findById(outboxEventId)
        .orElseThrow(() -> new NotFoundException("Outbox event not found: " + outboxEventId));
    event.reschedule(nextAttemptAt, errorMessage);
    outboxEventRepositoryPort.save(event);
  }

  @Override
  public void markFailed(UUID outboxEventId, Instant failedAt, String errorMessage) {
    var event = outboxEventRepositoryPort.findById(outboxEventId)
        .orElseThrow(() -> new NotFoundException("Outbox event not found: " + outboxEventId));
    event.markFailed(errorMessage, failedAt);
    outboxEventRepositoryPort.save(event);
  }
}
