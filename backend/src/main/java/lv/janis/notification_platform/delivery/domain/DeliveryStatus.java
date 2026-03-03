package lv.janis.notification_platform.delivery.domain;

public enum DeliveryStatus {
  PENDING,
  IN_PROGRESS,
  RETRY_SCHEDULED,
  DELIVERED,
  FAILED
}
