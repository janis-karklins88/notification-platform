package lv.janis.notification_platform.adminapi.application.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.ListOutboxEventsQuery;
import lv.janis.notification_platform.adminapi.application.port.in.OutboxMonitoringUseCase;
import lv.janis.notification_platform.outbox.application.port.out.OutboxEventRepositoryPort;
import lv.janis.notification_platform.outbox.application.port.out.OutboxFilter;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;

@Service
public class OutboxMonitoringService implements OutboxMonitoringUseCase {
  private static final int MAX_PAGE_SIZE = 100;

  private final OutboxEventRepositoryPort outboxEventRepositoryPort;

  public OutboxMonitoringService(OutboxEventRepositoryPort outboxEventRepositoryPort) {
    this.outboxEventRepositoryPort = outboxEventRepositoryPort;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<OutboxEvent> listOutboxEvents(ListOutboxEventsQuery query) {
    int safePage = Math.max(query.page(), 0);
    int safeSize = Math.min(Math.max(query.size(), 1), MAX_PAGE_SIZE);
    validateTimeRange(query.from(), query.to());

    Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    var filter = new OutboxFilter(
        query.status(),
        query.tenantId(),
        query.eventType(),
        query.aggregateType(),
        query.aggregateId(),
        query.from(),
        query.to());

    return outboxEventRepositoryPort.findAll(filter, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public OutboxEvent getOutboxEventById(UUID outboxEventId) {
    return outboxEventRepositoryPort.findById(outboxEventId)
        .orElseThrow(() -> new NotFoundException("Outbox event with id " + outboxEventId + " not found"));
  }

  private void validateTimeRange(Instant from, Instant to) {
    if (from != null && to != null && from.isAfter(to)) {
      throw new BadRequestException("from must be before or equal to to");
    }
  }
}
