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
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.OutboxEventMonitoringResponse;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.PageResponse;
import lv.janis.notification_platform.adminapi.application.port.in.ListOutboxEventsQuery;
import lv.janis.notification_platform.adminapi.application.port.in.OutboxMonitoringUseCase;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;

@RestController
@Validated
@RequestMapping("/admin")
public class OutboxMonitoringController {
  private final OutboxMonitoringUseCase outboxMonitoringUseCase;

  public OutboxMonitoringController(OutboxMonitoringUseCase outboxMonitoringUseCase) {
    this.outboxMonitoringUseCase = outboxMonitoringUseCase;
  }

  @GetMapping("/outbox-events")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<PageResponse<OutboxEventMonitoringResponse>> listOutboxEvents(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(required = false) OutboxStatus status,
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) OutboxEventType eventType,
      @RequestParam(required = false) OutboxEventAggregateType aggregateType,
      @RequestParam(required = false) UUID aggregateId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
    var query = new ListOutboxEventsQuery(page, size, status, tenantId, eventType, aggregateType, aggregateId, from, to);
    Page<OutboxEvent> outboxEvents = outboxMonitoringUseCase.listOutboxEvents(query);
    return ResponseEntity.ok(PageResponse.from(outboxEvents, OutboxEventMonitoringResponse::from));
  }

  @GetMapping("/outbox-events/{outboxEventId}")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<OutboxEventMonitoringResponse> getOutboxEvent(@PathVariable UUID outboxEventId) {
    OutboxEvent outboxEvent = outboxMonitoringUseCase.getOutboxEventById(outboxEventId);
    return ResponseEntity.ok(OutboxEventMonitoringResponse.from(outboxEvent));
  }
}
