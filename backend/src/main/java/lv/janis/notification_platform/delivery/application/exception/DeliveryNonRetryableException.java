package lv.janis.notification_platform.delivery.application.exception;

public class DeliveryNonRetryableException extends RuntimeException {
  public DeliveryNonRetryableException(String message) {
    super(message);
  }

  public DeliveryNonRetryableException(Throwable cause) {
    super(cause);
  }

  public DeliveryNonRetryableException(String message, Throwable cause) {
    super(message, cause);
  }
}
