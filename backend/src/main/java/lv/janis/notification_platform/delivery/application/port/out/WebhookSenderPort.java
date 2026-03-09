package lv.janis.notification_platform.delivery.application.port.out;

import lv.janis.notification_platform.delivery.domain.Delivery;

public interface WebhookSenderPort {
  void send(Delivery delivery);
}
