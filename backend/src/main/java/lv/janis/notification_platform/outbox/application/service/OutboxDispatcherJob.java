package lv.janis.notification_platform.outbox.application.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lv.janis.notification_platform.outbox.application.port.in.OutboxDispatchUseCase;

@Component
public class OutboxDispatcherJob {
  private final OutboxDispatchUseCase outboxDispatchUseCase;
  private final OutboxDispatchProperties properties;

  public OutboxDispatcherJob(OutboxDispatchUseCase outboxDispatchUseCase, OutboxDispatchProperties properties) {
    this.outboxDispatchUseCase = outboxDispatchUseCase;
    this.properties = properties;
  }

  // *******PROPERTIES DISABLED FOR NOW SO THIS WONT RUN*************/
  @Scheduled(fixedDelayString = "#{@outboxDispatchProperties.fixedDelayMs()}")
  public void tick() {
    if (!properties.enabled()) {
      return;
    }
    outboxDispatchUseCase.dispatch();
  }
}
