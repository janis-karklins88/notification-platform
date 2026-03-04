package lv.janis.notification_platform.outbox.application.port.in;

import lv.janis.notification_platform.outbox.domain.OutboxEvent;

public interface OutboxPublishUseCase {
  void publish(OutboxEvent event);
}
