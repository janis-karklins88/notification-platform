package lv.janis.notification_platform.delivery.application.port.in;

import java.util.UUID;

public interface WebhookDeliveryUseCase {
  public void deliverWebhook(UUID deliveryId);
}
