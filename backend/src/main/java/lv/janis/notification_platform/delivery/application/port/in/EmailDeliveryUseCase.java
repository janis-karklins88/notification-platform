package lv.janis.notification_platform.delivery.application.port.in;

import java.util.UUID;

public interface EmailDeliveryUseCase {
  void deliverEmail(UUID deliveryId);
}
