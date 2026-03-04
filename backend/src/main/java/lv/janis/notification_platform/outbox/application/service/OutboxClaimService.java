package lv.janis.notification_platform.outbox.application.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lv.janis.notification_platform.outbox.application.port.in.OutboxClaimUseCase;
import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;

@Service
public class OutboxClaimService implements OutboxClaimUseCase {
  private final OutboxEventRepositoryPort outboxEventRepositoryPort;

  public OutboxClaimService(OutboxEventRepositoryPort outboxEventRepositoryPort) {
    this.outboxEventRepositoryPort = outboxEventRepositoryPort;
  }

  @Override
  @Transactional
  public List<OutboxEvent> claim(int batchSize, Instant now) {
    List<OutboxEvent> claimedEvents = outboxEventRepositoryPort.claimNextBatch(batchSize, now, OutboxStatus.PENDING);
    for (var event : claimedEvents) {
      event.markInProgress(now);

    }
    return outboxEventRepositoryPort.saveAll(claimedEvents);
  }
}
