package lv.janis.notification_platform.adminapi.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.ApiKeyUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.CreateApiKeyResult;
import lv.janis.notification_platform.auth.application.port.out.ApiKeyRepositoryPort;
import lv.janis.notification_platform.auth.application.port.out.ListApiKeyQuery;
import lv.janis.notification_platform.auth.domain.ApiKey;
import lv.janis.notification_platform.auth.domain.ApiKeyStatus;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;

@Service
public class ApiKeyService implements ApiKeyUseCase {
  private static final int MAX_PAGE_SIZE = 100;
  private final ApiKeyRepositoryPort apiKeyPort;
  private final TenantRepositoryPort tenantRepositoryPort;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  public ApiKeyService(ApiKeyRepositoryPort apiKeyPort, TenantRepositoryPort tenantRepositoryPort) {
    this.apiKeyPort = apiKeyPort;
    this.tenantRepositoryPort = tenantRepositoryPort;
  }

  @Override
  @Transactional
  public CreateApiKeyResult createApiKey(UUID tenantId) {
    var tenant = tenantRepositoryPort.findById(tenantId)
        .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantId));
    String rawKey = generateRawKey();
    String prefix = rawKey.substring(0, 8);
    String keyHash = hash(rawKey);
    ApiKey apiKey = new ApiKey(tenant, prefix, keyHash);
    ApiKey saved = apiKeyPort.save(apiKey);
    return CreateApiKeyResult.from(saved, rawKey);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ApiKey> listApiKeys(ListApiKeyQuery query) {
    int safePage = Math.max(query.page(), 0);
    int safeSize = Math.min(Math.max(query.size(), 1), MAX_PAGE_SIZE);
    if (query.createdFrom() != null && query.createdTo() != null && query.createdFrom().isAfter(query.createdTo())) {
      throw new BadRequestException("createdFrom must be before or equal to createdTo");
    }

    Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    return apiKeyPort.findAll(query, pageable);
  }

  @Override
  @Transactional
  public void revokeApiKey(UUID apiKeyId) {
    ApiKey apiKey = apiKeyPort.findById(apiKeyId)
        .orElseThrow(() -> new NotFoundException("API key not found: " + apiKeyId));
    if (apiKey.getStatus() == ApiKeyStatus.REVOKED) {
      throw new BadRequestException("API key is already revoked: " + apiKeyId);
    }
    Instant now = Instant.now();
    apiKey.revoke(now);
    apiKeyPort.save(apiKey);
  }

  private String generateRawKey() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String rawKey) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

}
