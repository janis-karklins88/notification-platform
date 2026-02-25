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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.ApiKeyListResponse;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.CreateApiKeyResponse;
import lv.janis.notification_platform.adminapi.adapter.in.web.dto.PageResponse;
import lv.janis.notification_platform.adminapi.application.port.in.ApiKeyUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.CreateApiKeyResult;
import lv.janis.notification_platform.auth.application.port.out.ListApiKeyQuery;
import lv.janis.notification_platform.auth.domain.ApiKey;
import lv.janis.notification_platform.auth.domain.ApiKeyStatus;

@RestController
@Validated
@RequestMapping("/admin")
public class ApiKeyAdminController {
  private final ApiKeyUseCase apiKeyUseCase;

  public ApiKeyAdminController(ApiKeyUseCase apiKeyUseCase) {
    this.apiKeyUseCase = apiKeyUseCase;
  }

  @PostMapping("/tenants/{tenantId}/api-keys")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<CreateApiKeyResponse> createApiKey(@PathVariable UUID tenantId) {
    CreateApiKeyResult apiKey = apiKeyUseCase.createApiKey(tenantId);
    return ResponseEntity
        .created(URI.create("/admin/api-keys?tenantId=" + apiKey.tenantId()))
        .body(CreateApiKeyResponse.from(apiKey));
  }

  @GetMapping("/api-keys")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<PageResponse<ApiKeyListResponse>> listApiKeys(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) ApiKeyStatus status,
      @RequestParam(required = false) String prefix,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo) {

    var query = new ListApiKeyQuery(page, size, tenantId, status, prefix, createdFrom, createdTo);

    Page<ApiKey> apiKeys = apiKeyUseCase.listApiKeys(query);

    List<ApiKeyListResponse> content = apiKeys.getContent().stream()
        .map(ApiKeyListResponse::from)
        .toList();
    return ResponseEntity.ok(new PageResponse<>(
        content,
        apiKeys.getNumber(),
        apiKeys.getSize(),
        apiKeys.getTotalElements(),
        apiKeys.getTotalPages(),
        apiKeys.hasNext(),
        apiKeys.hasPrevious()));
  }

  @PostMapping("/api-keys/{apiKeyId}/revoke")
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<Void> revokeApiKey(@PathVariable UUID apiKeyId) {
    apiKeyUseCase.revokeApiKey(apiKeyId);
    return ResponseEntity.noContent().build();
  }
}
