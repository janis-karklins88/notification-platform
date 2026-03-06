package lv.janis.notification_platform.delivery.adapter.in.messaging;

import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.ConflictException;
import lv.janis.notification_platform.delivery.application.exception.DeliveryNonRetryableException;

public final class DeliveryListenerFailurePolicy {

  private DeliveryListenerFailurePolicy() {
  }

  public static boolean isNonRetryable(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof NotFoundException
          || current instanceof IllegalArgumentException
          || current instanceof IllegalStateException
          || current instanceof UnsupportedOperationException
          || current instanceof BadRequestException
          || current instanceof DeliveryNonRetryableException
          || current instanceof ConflictException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
