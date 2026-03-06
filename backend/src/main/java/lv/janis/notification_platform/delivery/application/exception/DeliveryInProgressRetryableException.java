package lv.janis.notification_platform.delivery.application.exception;

public class DeliveryInProgressRetryableException extends RuntimeException {
  public DeliveryInProgressRetryableException(String message) {
    super(message);
  }
}
