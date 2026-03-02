package lv.janis.notification_platform.adminapi.adapter.in.web;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.CreateSubscriptionRequest;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.PageResponse;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.SubscriptionResponse;
import lv.janis.notification_platform.adminapi.application.port.in.CreateSubscriptionCommand;
import lv.janis.notification_platform.adminapi.application.port.in.ListSubscriptionsQuery;
import lv.janis.notification_platform.adminapi.application.port.in.SubscriptionUseCase;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.routing.domain.SubscriptionStatus;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@Validated
@RequestMapping("/admin")
public class SubscriptionController {
  private final SubscriptionUseCase subscriptionUseCase;

  public SubscriptionController(SubscriptionUseCase subscriptionUseCase) {
    this.subscriptionUseCase = subscriptionUseCase;
  }

  @PostMapping("/tenants/{tenantId}/subscriptions")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<SubscriptionResponse> createSubscription(@PathVariable UUID tenantId,
      @Valid @RequestBody CreateSubscriptionRequest request) {
    var command = new CreateSubscriptionCommand(tenantId, request.enventType(), request.endpointId());
    var result = subscriptionUseCase.createSubscription(command);
    return ResponseEntity.created(null).body(SubscriptionResponse.from(result));
  }

  @GetMapping("/tenants/{tenantId}/subscriptions")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<PageResponse<SubscriptionResponse>> listSubscriptions(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) String eventType,
      @RequestParam(required = false) UUID endpointId,
      @RequestParam(required = false) SubscriptionStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdAfter,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdBefore) {
    var query = new ListSubscriptionsQuery(tenantId, eventType, endpointId, status, createdAfter, createdBefore, page,
        size);
    Page<Subscription> result = subscriptionUseCase.listSubscriptions(query);
    List<SubscriptionResponse> response = result.getContent().stream().map(SubscriptionResponse::from).toList();
    return ResponseEntity.ok(
        new PageResponse<>(
            response,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.hasNext(),
            result.hasPrevious()));
  }

  @PostMapping("/subscriptions/{subscriptionId}/deactivate")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<Void> deactivateSubscription(@PathVariable UUID subscriptionId) {
    subscriptionUseCase.deactivateSubscription(subscriptionId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/subscriptions/{subscriptionId}/reactivate")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<Void> activateSubscription(@PathVariable UUID subscriptionId) {
    subscriptionUseCase.activateSubscription(subscriptionId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/subscriptions/{subscriptionId}/delete")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<Void> deleteSubscription(@PathVariable UUID subscriptionId) {
    subscriptionUseCase.deleteSubscription(subscriptionId);
    return ResponseEntity.noContent().build();
  }

}
