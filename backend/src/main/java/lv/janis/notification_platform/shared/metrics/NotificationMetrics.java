package lv.janis.notification_platform.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

  private final Counter eventAcceptedTotal;
  private final Counter deliveriesCreatedTotal;
  private final Counter outboxClaimedTotal;
  private final Counter outboxPublishedTotal;
  private final Counter outboxPublishFailedTotal;
  private final Counter outboxRescheduledTotal;
  private final Counter deliveryAttemptStartedTotal;
  private final Counter deliverySuccessTotal;
  private final Counter deliveryFailureTotal;
  private final Counter deliveryRetryScheduledTotal;

  public NotificationMetrics(MeterRegistry meterRegistry) {
    this.eventAcceptedTotal = Counter.builder("notification_event_accepted_total")
        .description("Number of accepted events")
        .register(meterRegistry);

    this.deliveriesCreatedTotal = Counter.builder("notification_deliveries_created_total")
        .description("Number of deliveries created from events")
        .register(meterRegistry);

    this.outboxClaimedTotal = Counter.builder("notification_outbox_claimed_total")
        .description("Number of outbox events claimed for publishing")
        .register(meterRegistry);

    this.outboxPublishedTotal = Counter.builder("notification_outbox_published_total")
        .description("Number of outbox events successfully published")
        .register(meterRegistry);

    this.outboxPublishFailedTotal = Counter.builder("notification_outbox_publish_failed_total")
        .description("Number of outbox publish failures")
        .register(meterRegistry);

    this.outboxRescheduledTotal = Counter.builder("notification_outbox_rescheduled_total")
        .description("Number of outbox events rescheduled after publish failure")
        .register(meterRegistry);

    this.deliveryAttemptStartedTotal = Counter.builder("notification_delivery_attempt_started_total")
        .description("Number of delivery attempts started")
        .register(meterRegistry);

    this.deliverySuccessTotal = Counter.builder("notification_delivery_success_total")
        .description("Number of successful deliveries")
        .register(meterRegistry);

    this.deliveryFailureTotal = Counter.builder("notification_delivery_failure_total")
        .description("Number of failed deliveries")
        .register(meterRegistry);

    this.deliveryRetryScheduledTotal = Counter.builder("notification_delivery_retry_scheduled_total")
        .description("Number of deliveries scheduled for retry")
        .register(meterRegistry);
  }

  public void incrementEventAccepted() {
    eventAcceptedTotal.increment();
  }

  public void incrementDeliveriesCreated(int count) {
    deliveriesCreatedTotal.increment(count);
  }

  public void incrementOutboxClaimed(int count) {
    outboxClaimedTotal.increment(count);
  }

  public void incrementOutboxPublished() {
    outboxPublishedTotal.increment();
  }

  public void incrementOutboxPublishFailed() {
    outboxPublishFailedTotal.increment();
  }

  public void incrementOutboxRescheduled() {
    outboxRescheduledTotal.increment();
  }

  public void incrementDeliveryAttemptStarted() {
    deliveryAttemptStartedTotal.increment();
  }

  public void incrementDeliverySuccess() {
    deliverySuccessTotal.increment();
  }

  public void incrementDeliveryFailure() {
    deliveryFailureTotal.increment();
  }

  public void incrementDeliveryRetryScheduled() {
    deliveryRetryScheduledTotal.increment();
  }
}