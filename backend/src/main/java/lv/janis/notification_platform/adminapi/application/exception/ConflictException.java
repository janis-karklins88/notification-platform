package lv.janis.notification_platform.adminapi.application.exception;

public class ConflictException extends RuntimeException {
  public ConflictException(String message) {
    super(message);
  }

  public static ConflictException of(String message) {
    return new ConflictException(message);
  }
}
