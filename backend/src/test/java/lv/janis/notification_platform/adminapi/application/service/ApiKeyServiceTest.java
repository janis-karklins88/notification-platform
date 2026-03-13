package lv.janis.notification_platform.adminapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.CreateApiKeyResult;
import lv.janis.notification_platform.auth.application.port.out.ApiKeyRepositoryPort;
import lv.janis.notification_platform.auth.application.port.out.ListApiKeyQuery;
import lv.janis.notification_platform.auth.application.service.ApiKeyHasher;
import lv.janis.notification_platform.auth.domain.ApiKey;
import lv.janis.notification_platform.auth.domain.ApiKeyStatus;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static lv.janis.notification_platform.support.EntityTestData.apiKey;
import static lv.janis.notification_platform.support.EntityTestData.tenant;

class ApiKeyServiceTest {
  private static final Instant NOW = Instant.parse("2026-02-01T10:15:30Z");

  private final ApiKeyRepositoryPort apiKeyRepository = mock(ApiKeyRepositoryPort.class);
  private final TenantRepositoryPort tenantRepository = mock(TenantRepositoryPort.class);
  private final ApiKeyHasher apiKeyHasher = mock(ApiKeyHasher.class);
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final ApiKeyService service = new ApiKeyService(apiKeyRepository, tenantRepository, apiKeyHasher, clock);

  @Test
  void createApiKeyHashesGeneratedKeyAndReturnsPlaintextResult() {
    UUID tenantId = UUID.randomUUID();
    UUID apiKeyId = UUID.randomUUID();
    Tenant tenant = tenant(tenantId);
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(apiKeyHasher.hash(any())).thenReturn("hashed-key");
    when(apiKeyRepository.save(any())).thenAnswer(invocation -> {
      ApiKey apiKey = invocation.getArgument(0);
      ReflectionTestUtils.setField(apiKey, "id", apiKeyId);
      ReflectionTestUtils.setField(apiKey, "tenantId", tenantId);
      ReflectionTestUtils.setField(apiKey, "createdAt", NOW);
      return apiKey;
    });

    CreateApiKeyResult result = service.createApiKey(tenantId);

    assertEquals(apiKeyId, result.id());
    assertEquals(tenantId, result.tenantId());
    assertEquals(ApiKeyStatus.ACTIVE, result.status());
    assertEquals(NOW, result.createdAt());
    assertNotNull(result.plaintextKey());
    assertEquals(result.plaintextKey().substring(0, 8), result.keyPrefix());

    ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
    verify(apiKeyRepository).save(apiKeyCaptor.capture());
    assertEquals("hashed-key", apiKeyCaptor.getValue().getKeyHash());
    assertEquals(result.keyPrefix(), apiKeyCaptor.getValue().getKeyPrefix());
    verify(apiKeyHasher).hash(result.plaintextKey());
  }

  @Test
  void listApiKeysClampsPageAndSizeAndDelegatesQuery() {
    ListApiKeyQuery query = new ListApiKeyQuery(
        -1,
        500,
        UUID.randomUUID(),
        ApiKeyStatus.ACTIVE,
        "pre",
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-02T00:00:00Z"));
    Page<ApiKey> expected = new PageImpl<>(List.of());
    when(apiKeyRepository.findAll(any(), any())).thenReturn(expected);

    Page<ApiKey> result = service.listApiKeys(query);

    assertEquals(expected, result);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(apiKeyRepository).findAll(org.mockito.ArgumentMatchers.same(query), pageableCaptor.capture());
    assertEquals(0, pageableCaptor.getValue().getPageNumber());
    assertEquals(100, pageableCaptor.getValue().getPageSize());
    assertEquals(Sort.Direction.DESC, pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection());
  }

  @Test
  void listApiKeysRejectsInvalidDateRange() {
    ListApiKeyQuery query = new ListApiKeyQuery(
        0,
        10,
        null,
        null,
        null,
        Instant.parse("2026-01-02T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"));

    BadRequestException ex = assertThrows(BadRequestException.class, () -> service.listApiKeys(query));

    assertEquals("createdFrom must be before or equal to createdTo", ex.getMessage());
  }

  @Test
  void revokeApiKeyMarksKeyRevokedAndSaves() {
    UUID apiKeyId = UUID.randomUUID();
    ApiKey apiKey = apiKey(apiKeyId, tenant(UUID.randomUUID()), "prefix", "hash", NOW.minusSeconds(60));
    when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));

    service.revokeApiKey(apiKeyId);

    assertEquals(ApiKeyStatus.REVOKED, apiKey.getStatus());
    assertEquals(NOW, apiKey.getRevokedAt());
    verify(apiKeyRepository).save(apiKey);
  }

  @Test
  void revokeApiKeyRejectsAlreadyRevokedKey() {
    UUID apiKeyId = UUID.randomUUID();
    ApiKey apiKey = apiKey(apiKeyId, tenant(UUID.randomUUID()), "prefix", "hash", NOW.minusSeconds(60));
    apiKey.revoke(NOW.minusSeconds(5));
    when(apiKeyRepository.findById(apiKeyId)).thenReturn(Optional.of(apiKey));

    BadRequestException ex = assertThrows(BadRequestException.class, () -> service.revokeApiKey(apiKeyId));

    assertEquals("API key is already revoked: " + apiKeyId, ex.getMessage());
  }

  @Test
  void createApiKeyThrowsNotFoundWhenTenantMissing() {
    UUID tenantId = UUID.randomUUID();
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(NotFoundException.class, () -> service.createApiKey(tenantId));

    assertEquals("Tenant not found: " + tenantId, ex.getMessage());
  }
}
