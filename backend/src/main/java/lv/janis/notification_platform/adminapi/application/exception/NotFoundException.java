package lv.janis.notification_platform.adminapi.application.exception;

public class NotFoundException extends RuntimeException {
  public NotFoundException(String message) {
    super(message);
  }

  public static NotFoundException of(String entity, Object id) {
    return new NotFoundException(entity + " not found: " + id);
  }
}
