package lv.janis.notification_platform.adminapi.application.port.in;

import org.springframework.data.domain.Page;

import lv.janis.notification_platform.delivery.domain.Delivery;

import java.util.UUID;

public interface DeliveryMonitoringUseCase {
  Page<Delivery> listDeliveries(ListDeliveriesQuery query);

  Delivery getDeliveryById(UUID deliveryId);
}
