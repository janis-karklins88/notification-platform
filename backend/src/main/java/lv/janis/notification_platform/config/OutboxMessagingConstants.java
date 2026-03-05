package lv.janis.notification_platform.config;

public class OutboxMessagingConstants {
  public static final String EXCHANGE_OUTBOX_EVENTS = "np.outbox.events";
  public static final String EXCHANGE_OUTBOX_EVENTS_DLQ = "np.outbox.events.dlq";

  public static final String QUEUE_ROUTING_EVENT_ACCEPTED = "np.routing.event.accepted";
  public static final String QUEUE_DELIVERY_CREATED_WEBHOOK = "np.delivery.delivery.created.web";
  public static final String QUEUE_DELIVERY_CREATED_EMAIL = "np.delivery.delivery.created.email";
  public static final String QUEUE_OUTBOX_DLQ = "np.outbox.dlq";

  public static final String RK_EVENT_ACCEPTED = "event.accepted";
  public static final String RK_DELIVERY_CREATED_WEBHOOK = "delivery.created.web";
  public static final String RK_DELIVERY_CREATED_EMAIL = "delivery.created.email";
  public static final String RK_OUTBOX_DLQ = "outbox.dlq";
}
