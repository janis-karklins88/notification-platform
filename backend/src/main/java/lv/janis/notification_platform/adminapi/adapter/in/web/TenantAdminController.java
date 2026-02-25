package lv.janis.notification_platform.adminapi.adapter.in.web;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.CreateTenantRequest;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.EditTenantRequest;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.PageResponse;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.TenantResponse;
import lv.janis.notification_platform.adminapi.application.port.in.CreateTenantCommand;
import lv.janis.notification_platform.adminapi.application.port.in.CreateTenantUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.EditTenantByIdUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.EditTenantCommand;
import lv.janis.notification_platform.adminapi.application.port.in.GetTenantByIdUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.ListTenantUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.ListTenantsQuery;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

@RestController
@Validated
@RequestMapping("/admin/tenants")
public class TenantAdminController {
    private final ListTenantUseCase listTenantUseCase;
    private final CreateTenantUseCase createTenantUseCase;
    private final GetTenantByIdUseCase getTenantByIdUseCase;
    private final EditTenantByIdUseCase editTenantByIdUseCase;

    public TenantAdminController(ListTenantUseCase listTenantUseCase, CreateTenantUseCase createTenantUseCase,
            GetTenantByIdUseCase getTenantByIdUseCase, EditTenantByIdUseCase editTenantByIdUseCase) {
        this.listTenantUseCase = listTenantUseCase;
        this.createTenantUseCase = createTenantUseCase;
        this.getTenantByIdUseCase = getTenantByIdUseCase;
        this.editTenantByIdUseCase = editTenantByIdUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        Tenant tenant = createTenantUseCase.createTenant(
                new CreateTenantCommand(request.slug(), request.name(), request.status()));

        return ResponseEntity
                .created(URI.create("/admin/tenants/" + tenant.getId()))
                .body(TenantResponse.from(tenant));
    }

    @PatchMapping("/{tenantId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<TenantResponse> editTenant(@PathVariable UUID tenantId,
            @Valid @RequestBody EditTenantRequest request) {
        Tenant tenant = editTenantByIdUseCase
                .editTenantById(new EditTenantCommand(tenantId, request.name(), request.status()));
        return ResponseEntity.ok(TenantResponse.from(tenant));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<PageResponse<TenantResponse>> getTenants(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) TenantStatus status,
            @RequestParam(required = false) String nameContains,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo) {
        Page<Tenant> tenantsPage = listTenantUseCase.listTenants(
                new ListTenantsQuery(page, size, status, nameContains, createdFrom, createdTo));
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
                        tenantsPage.hasPrevious()));
    }

    @GetMapping("/{tenantId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<TenantResponse> getTenantById(@PathVariable UUID tenantId) {
        Tenant tenant = getTenantByIdUseCase.getTenantById(tenantId);
        return ResponseEntity.ok(TenantResponse.from(tenant));
    }

}
