package lv.janis.notification_platform.adminapi.adapter.in.web;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.CreateEndpointRequest;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.EndpointResponse;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.PageResponse;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.UpdateEndpointRequest;
import lv.janis.notification_platform.adminapi.application.port.in.CreateEndpointCommand;
import lv.janis.notification_platform.adminapi.application.port.in.EndpointUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.ListEndpointQuery;
import lv.janis.notification_platform.adminapi.application.port.in.UpdateEndpointCommand;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@Validated
@RequestMapping("/admin")
public class EndpointAdminController {
  private final EndpointUseCase endpointUseCase;

  public EndpointAdminController(EndpointUseCase endpointUseCase) {
    this.endpointUseCase = endpointUseCase;
  }

  @PostMapping("/tenants/{tenantId}/endpoints")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<EndpointResponse> createEndpoint(@PathVariable UUID tenantId,
      @Valid @RequestBody CreateEndpointRequest request) {
    Endpoint result = endpointUseCase.createEndpoint(
        new CreateEndpointCommand(tenantId, request.type(), request.config()));
    return ResponseEntity.created(URI.create("/admin/endpoints/" + result.getId())).body(EndpointResponse.from(result));
  }

  @GetMapping("/endpoints/{endpointId}")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<EndpointResponse> getEndpointById(@PathVariable UUID endpointId) {
    var result = endpointUseCase.getEndpointById(endpointId);
    return ResponseEntity.ok(EndpointResponse.from(result));
  }

  @PatchMapping("/endpoints/{endpointId}")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<EndpointResponse> updateEndpoint(@PathVariable UUID endpointId,
      @Valid @RequestBody UpdateEndpointRequest request) {
    var result = endpointUseCase.updateEndpoint(
        new UpdateEndpointCommand(endpointId, request.config()));
    return ResponseEntity.ok(EndpointResponse.from(result));
  }

  @PostMapping("/endpoints/{endpointId}/deactivate")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<Void> deactivateEndpoint(@PathVariable UUID endpointId) {
    endpointUseCase.deactivateEndpoint(endpointId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/endpoints/{endpointId}/delete")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<Void> deleteEndpoint(@PathVariable UUID endpointId) {
    endpointUseCase.deleteEndpoint(endpointId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/endpoints/{endpointId}/reactivate")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<Void> reactivateEndpoint(@PathVariable UUID endpointId) {
    endpointUseCase.reactivateEndpoint(endpointId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/endpoints")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<PageResponse<EndpointResponse>> listEndpoints(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) EndpointStatus status,
      @RequestParam(required = false) EndpointType type,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo) {

    var query = new ListEndpointQuery(page, size, tenantId, status, type, createdFrom, createdTo);

    Page<Endpoint> endpoints = endpointUseCase.listEndpoints(query);
    return ResponseEntity.ok(PageResponse.from(endpoints, EndpointResponse::from));
  }
}
