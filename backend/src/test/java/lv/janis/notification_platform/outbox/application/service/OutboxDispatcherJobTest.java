package lv.janis.notification_platform.outbox.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import lv.janis.notification_platform.outbox.application.port.in.OutboxDispatchUseCase;

class OutboxDispatcherJobTest {
  private final OutboxDispatchUseCase useCase = mock(OutboxDispatchUseCase.class);

  @Test
  void tickDoesNothingWhenDispatchIsDisabled() {
    OutboxDispatcherJob job = new OutboxDispatcherJob(useCase, new OutboxDispatchProperties(100, false, 1000L, 60000L, 10, 3, 200L));

    job.tick();

    verify(useCase, never()).dispatch();
  }

  @Test
  void tickDispatchesWhenEnabled() {
    OutboxDispatcherJob job = new OutboxDispatcherJob(useCase, new OutboxDispatchProperties(100, true, 1000L, 60000L, 10, 3, 200L));

    job.tick();

    verify(useCase).dispatch();
  }
}
