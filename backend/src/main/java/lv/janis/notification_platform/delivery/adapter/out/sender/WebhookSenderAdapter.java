package lv.janis.notification_platform.delivery.adapter.out.sender;

import org.springframework.stereotype.Component;

import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.application.port.out.WebhookSenderPort;

@Component
public class WebhookSenderAdapter implements WebhookSenderPort {
  public void send(Delivery delivery) {

  }
}
