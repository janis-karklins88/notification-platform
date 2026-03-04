package lv.janis.notification_platform.outbox.domain;

public enum OutboxEventType {
  EVENT_ACCEPTED("event.accepted"),
  DELIVERY_CREATED("delivery.created");

  private final String routingKey;

  OutboxEventType(String routingKey) {
    this.routingKey = routingKey;
  }

  public String routingKey() {
    return routingKey;
  }
}
