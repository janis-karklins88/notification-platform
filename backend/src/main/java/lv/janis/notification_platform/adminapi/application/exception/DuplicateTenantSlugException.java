package lv.janis.notification_platform.adminapi.application.exception;

public class DuplicateTenantSlugException extends RuntimeException {
  public DuplicateTenantSlugException(String slug) {
    super("Tenant slug already exists: " + slug);
  }
}
