package lv.janis.notification_platform.adminapi.application.port.in;

import java.util.UUID;

import org.springframework.data.domain.Page;

import lv.janis.notification_platform.outbox.domain.OutboxEvent;

public interface OutboxMonitoringUseCase {
  Page<OutboxEvent> listOutboxEvents(ListOutboxEventsQuery query);

  OutboxEvent getOutboxEventById(UUID outboxEventId);
}
