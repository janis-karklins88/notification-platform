package lv.janis.notification_platform.adminapi.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.DeliveryMonitoringResponse;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.PageResponse;
import lv.janis.notification_platform.adminapi.application.port.in.DeliveryMonitoringUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.ListDeliveriesQuery;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.DeliveryStatus;

@RestController
@Validated
@RequestMapping("/admin")
public class DeliveryMonitoringController {
  private final DeliveryMonitoringUseCase deliveryMonitoringUseCase;

  public DeliveryMonitoringController(DeliveryMonitoringUseCase deliveryMonitoringUseCase) {
    this.deliveryMonitoringUseCase = deliveryMonitoringUseCase;
  }

  @GetMapping("/deliveries")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<PageResponse<DeliveryMonitoringResponse>> listDeliveries(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(required = false) DeliveryStatus status,
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) UUID eventId,
      @RequestParam(required = false) UUID endpointId,
      @RequestParam(required = false) String channel,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
    var query = new ListDeliveriesQuery(page, size, status, tenantId, eventId, endpointId, channel, from, to);
    Page<Delivery> deliveries = deliveryMonitoringUseCase.listDeliveries(query);
    return ResponseEntity.ok(PageResponse.from(deliveries, DeliveryMonitoringResponse::from));
  }

  @GetMapping("/deliveries/{deliveryId}")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<DeliveryMonitoringResponse> getDelivery(@PathVariable UUID deliveryId) {
    Delivery delivery = deliveryMonitoringUseCase.getDeliveryById(deliveryId);
    return ResponseEntity.ok(DeliveryMonitoringResponse.from(delivery));
  }
}
