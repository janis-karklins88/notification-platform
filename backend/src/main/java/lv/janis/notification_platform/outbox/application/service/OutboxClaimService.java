package lv.janis.notification_platform.outbox.application.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lv.janis.notification_platform.outbox.application.port.in.OutboxClaimUseCase;
import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;

@Service
public class OutboxClaimService implements OutboxClaimUseCase {
  private final OutboxEventRepositoryPort outboxEventRepositoryPort;
  private final OutboxDispatchProperties outboxDispatchProperties;

  public OutboxClaimService(
      OutboxEventRepositoryPort outboxEventRepositoryPort,
      OutboxDispatchProperties outboxDispatchProperties) {
    this.outboxEventRepositoryPort = outboxEventRepositoryPort;
    this.outboxDispatchProperties = outboxDispatchProperties;
  }

  @Override
  @Transactional
  public List<OutboxEvent> claim(int batchSize, Instant now) {
    Instant staleBefore = now.minusMillis(outboxDispatchProperties.inProgressTimeoutMs());
    List<OutboxEvent> claimedEvents = outboxEventRepositoryPort.claimNextBatch(batchSize, now, staleBefore);
    for (var event : claimedEvents) {
      event.markInProgress(now);
    }
    return outboxEventRepositoryPort.saveAll(claimedEvents);
  }
}
