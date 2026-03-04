package lv.janis.notification_platform.config;

public class OutboxMessagingConstants {
  public static final String EXCHANGE_OUTBOX_EVENTS = "np.outbox.events";

  public static final String QUEUE_ROUTING_EVENT_ACCEPTED = "np.routing.event.accepted";
  public static final String QUEUE_DELIVERY_CREATED = "np.delivery.delivery.created";

  public static final String RK_EVENT_ACCEPTED = "event.accepted";
  public static final String RK_DELIVERY_CREATED = "delivery.created";
}
