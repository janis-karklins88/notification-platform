package lv.janis.notification_platform.delivery.application.port.out;

import lv.janis.notification_platform.delivery.application.model.PreparedEmailMessage;

public interface EmailSenderPort {
  void send(PreparedEmailMessage message);
}
