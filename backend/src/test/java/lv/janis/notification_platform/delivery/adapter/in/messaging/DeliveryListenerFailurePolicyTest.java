package lv.janis.notification_platform.delivery.adapter.in.messaging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.delivery.application.exception.DeliveryNonRetryableException;
import org.junit.jupiter.api.Test;

class DeliveryListenerFailurePolicyTest {

  @Test
  void isNonRetryableReturnsTrueForKnownExceptionTypes() {
    assertTrue(DeliveryListenerFailurePolicy.isNonRetryable(new BadRequestException("bad")));
    assertTrue(DeliveryListenerFailurePolicy.isNonRetryable(new DeliveryNonRetryableException("failed")));
    assertTrue(DeliveryListenerFailurePolicy.isNonRetryable(new IllegalArgumentException("bad")));
  }

  @Test
  void isNonRetryableReturnsTrueWhenKnownExceptionIsNestedCause() {
    RuntimeException wrapped = new RuntimeException(new IllegalStateException("bad state"));

    assertTrue(DeliveryListenerFailurePolicy.isNonRetryable(wrapped));
  }

  @Test
  void isNonRetryableReturnsFalseForRetryableExceptionChain() {
    RuntimeException wrapped = new RuntimeException(new RuntimeException("temporary"));

    assertFalse(DeliveryListenerFailurePolicy.isNonRetryable(wrapped));
  }
}
