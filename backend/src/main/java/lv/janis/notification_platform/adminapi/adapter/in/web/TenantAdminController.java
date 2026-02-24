package lv.janis.notification_platform.adminapi.adapter.in.web;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.CreateTenantRequest;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.PageResponse;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.TenantResponse;
import lv.janis.notification_platform.adminapi.application.port.in.CreateTenantCommand;
import lv.janis.notification_platform.adminapi.application.port.in.CreateTenantUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.ListTenantUseCase;
import lv.janis.notification_platform.tenant.domain.Tenant;


@RestController
@Validated
@RequestMapping("/admin/tenants")
public class TenantAdminController {
  private final ListTenantUseCase listTenantUseCase;
  private final CreateTenantUseCase createTenantUseCase;

  public TenantAdminController(ListTenantUseCase listTenantUseCase, CreateTenantUseCase createTenantUseCase) {
    this.listTenantUseCase = listTenantUseCase;
    this.createTenantUseCase = createTenantUseCase;
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
  public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
    Tenant tenant = createTenantUseCase.createTenant(
        new CreateTenantCommand(request.slug(), request.name(), request.status()));

    return ResponseEntity
        .created(URI.create("/admin/tenants/" + tenant.getId()))
        .body(TenantResponse.from(tenant));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'TENANT_ADMIN')")
  public ResponseEntity<PageResponse<TenantResponse>> getTenants(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    Page<Tenant> tenantsPage = listTenantUseCase.listTenants(page, size);
    List<TenantResponse> tenants = tenantsPage.getContent().stream()
        .map(TenantResponse::from)
        .toList();

    return ResponseEntity.ok(
        new PageResponse<>(
            tenants,
            tenantsPage.getNumber(),
            tenantsPage.getSize(),
            tenantsPage.getTotalElements(),
            tenantsPage.getTotalPages(),
            tenantsPage.hasNext(),
            tenantsPage.hasPrevious()
        )
    );
  }

}
