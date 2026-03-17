package lv.janis.notification_platform.adminapi.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.ApiKeyUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.CreateApiKeyResult;
import lv.janis.notification_platform.auth.adapter.in.security.ApiKeyAuthenticationFilter;
import lv.janis.notification_platform.auth.domain.ApiKey;
import lv.janis.notification_platform.auth.domain.ApiKeyStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApiKeyAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApiKeyAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ApiKeyUseCase apiKeyUseCase;

  @MockitoBean
  private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

  @Test
  void createApiKeyReturnsCreatedResult() throws Exception {
    UUID tenantId = UUID.randomUUID();
    CreateApiKeyResult result = new CreateApiKeyResult(
        UUID.randomUUID(),
        tenantId,
        "abc12345",
        "raw-key",
        ApiKeyStatus.ACTIVE,
        Instant.parse("2026-01-01T00:00:00Z"));

    when(apiKeyUseCase.createApiKey(tenantId)).thenReturn(result);

    mockMvc.perform(post("/admin/tenants/" + tenantId + "/api-keys"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/admin/api-keys?tenantId=" + tenantId))
        .andExpect(jsonPath("$.id").value(result.id().toString()))
        .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
        .andExpect(jsonPath("$.plaintextKey").value("raw-key"));
  }

  @Test
  void listApiKeysReturnsPage() throws Exception {
    ApiKey item = apiKey(
        UUID.randomUUID(),
        "abc12345",
        ApiKeyStatus.ACTIVE,
        Instant.parse("2026-01-01T00:00:00Z"),
        null,
        null);
    when(apiKeyUseCase.listApiKeys(org.mockito.ArgumentMatchers.argThat(query ->
        query.page() == 0 && query.size() == 20 && query.status() == ApiKeyStatus.ACTIVE)))
        .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

    mockMvc.perform(get("/admin/api-keys").param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items[0].id").value(item.getId().toString()))
        .andExpect(jsonPath("$.items[0].keyPrefix").value("abc12345"));
  }

  @Test
  void revokeApiKeyReturnsNoContent() throws Exception {
    UUID apiKeyId = UUID.randomUUID();

    mockMvc.perform(post("/admin/api-keys/" + apiKeyId + "/revoke"))
        .andExpect(status().isNoContent());

    verify(apiKeyUseCase).revokeApiKey(apiKeyId);
  }

  @Test
  void listApiKeysReturnsBadRequestOnInvalidStatus() throws Exception {
    mockMvc.perform(get("/admin/api-keys").param("status", "WRONG"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", containsString("Invalid value for 'status'")));
  }

  @Test
  void listApiKeysReturnsBadRequestOnServiceValidationError() throws Exception {
    when(apiKeyUseCase.listApiKeys(org.mockito.ArgumentMatchers.any())).thenThrow(
        new BadRequestException("createdFrom must be before or equal to createdTo"));

    mockMvc.perform(
        get("/admin/api-keys")
            .param("createdFrom", "2026-01-02T00:00:00Z")
            .param("createdTo", "2026-01-01T00:00:00Z"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("createdFrom must be before or equal to createdTo"));
  }

  @Test
  void revokeApiKeyReturnsNotFound() throws Exception {
    UUID apiKeyId = UUID.randomUUID();
    doThrow(new NotFoundException("API key not found: " + apiKeyId)).when(apiKeyUseCase).revokeApiKey(apiKeyId);

    mockMvc.perform(post("/admin/api-keys/" + apiKeyId + "/revoke"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", containsString(apiKeyId.toString())));
  }

  @Test
  void listApiKeysMapsResponse() throws Exception {
    UUID id = UUID.randomUUID();
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    ApiKey item = apiKey(id, "pref", ApiKeyStatus.INACTIVE, now, null, null);

    when(apiKeyUseCase.listApiKeys(org.mockito.ArgumentMatchers.any()))
        .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

    mockMvc.perform(get("/admin/api-keys").param("tenantId", UUID.randomUUID().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(id.toString()))
        .andExpect(jsonPath("$.items[0].keyPrefix").value("pref"))
        .andExpect(jsonPath("$.items[0].status").value("INACTIVE"));
  }

  private ApiKey apiKey(
      UUID id,
      String keyPrefix,
      ApiKeyStatus status,
      Instant createdAt,
      Instant revokedAt,
      Instant lastUsedAt) {
    ApiKey key = org.mockito.Mockito.mock(ApiKey.class);
    when(key.getId()).thenReturn(id);
    when(key.getKeyPrefix()).thenReturn(keyPrefix);
    when(key.getStatus()).thenReturn(status);
    when(key.getCreatedAt()).thenReturn(createdAt);
    when(key.getRevokedAt()).thenReturn(revokedAt);
    when(key.getLastUsedAt()).thenReturn(lastUsedAt);
    return key;
  }
}
