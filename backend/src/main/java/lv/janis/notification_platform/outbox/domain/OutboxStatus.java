package lv.janis.notification_platform.outbox.domain;

public enum OutboxStatus {
  PENDING,
  IN_PROGRESS,
  PUBLISHED,
  FAILED
}
