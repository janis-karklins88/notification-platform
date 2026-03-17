package lv.janis.notification_platform.adminapi.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.CreateTenantCommand;
import lv.janis.notification_platform.adminapi.application.port.in.CreateTenantUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.EditTenantByIdUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.EditTenantCommand;
import lv.janis.notification_platform.adminapi.application.port.in.GetTenantByIdUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.ListTenantUseCase;
import lv.janis.notification_platform.auth.adapter.in.security.ApiKeyAuthenticationFilter;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TenantAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class TenantAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ListTenantUseCase listTenantUseCase;

  @MockitoBean
  private CreateTenantUseCase createTenantUseCase;

  @MockitoBean
  private GetTenantByIdUseCase getTenantByIdUseCase;

  @MockitoBean
  private EditTenantByIdUseCase editTenantByIdUseCase;

  @MockitoBean
  private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

  @Test
  void createTenantReturnsCreated() throws Exception {
    UUID tenantId = UUID.randomUUID();
    var tenant = tenant(tenantId, "slug-1", "Acme Corp", TenantStatus.ACTIVE, Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"), 1L);

    when(createTenantUseCase.createTenant(new CreateTenantCommand("slug-1", "Acme Corp", TenantStatus.ACTIVE)))
        .thenReturn(tenant);

    var request = new CreateTenantRequestPayload("slug-1", "Acme Corp", TenantStatus.ACTIVE);

    mockMvc.perform(post("/admin/tenants")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/admin/tenants/" + tenantId))
        .andExpect(jsonPath("$.id").value(tenantId.toString()))
        .andExpect(jsonPath("$.slug").value("slug-1"))
        .andExpect(jsonPath("$.name").value("Acme Corp"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void createTenantReturnsBadRequestForBlankSlug() throws Exception {
    var request = new CreateTenantRequestPayload(" ", "Acme Corp", TenantStatus.ACTIVE);

    mockMvc.perform(post("/admin/tenants")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createTenantReturnsBadRequestOnValidationError() throws Exception {
    when(createTenantUseCase.createTenant(org.mockito.ArgumentMatchers.any())).thenThrow(
        new BadRequestException("slug already exists"));

    var request = new CreateTenantRequestPayload("slug-1", "Acme Corp", TenantStatus.ACTIVE);

    mockMvc.perform(post("/admin/tenants")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("slug already exists"));
  }

  @Test
  void listTenantsReturnsPage() throws Exception {
    Tenant tenant = tenant(UUID.randomUUID(), "slug-1", "Acme Corp", TenantStatus.ACTIVE,
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"), 1L);

    when(listTenantUseCase.listTenants(org.mockito.ArgumentMatchers.argThat(
        query -> query.page() == 0 && query.size() == 20 && query.status() == TenantStatus.ACTIVE)))
        .thenReturn(new PageImpl<>(List.of(tenant), PageRequest.of(0, 20), 1));

    mockMvc.perform(get("/admin/tenants").param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items[0].slug").value("slug-1"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void getTenantByIdReturnsMapping() throws Exception {
    UUID tenantId = UUID.randomUUID();
    Tenant tenant = tenant(tenantId, "slug-1", "Acme Corp", TenantStatus.SUSPENDED,
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"), 2L);

    when(getTenantByIdUseCase.getTenantById(tenantId)).thenReturn(tenant);

    mockMvc.perform(get("/admin/tenants/" + tenantId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tenantId.toString()))
        .andExpect(jsonPath("$.slug").value("slug-1"))
        .andExpect(jsonPath("$.status").value("SUSPENDED"));
  }

  @Test
  void editTenantReturnsMapping() throws Exception {
    UUID tenantId = UUID.randomUUID();
    Tenant tenant = tenant(tenantId, "slug-1", "Acme Updated", TenantStatus.INACTIVE,
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"), 2L);
    var request = new EditTenantRequestPayload("Acme Updated", TenantStatus.INACTIVE);

    when(editTenantByIdUseCase.editTenantById(new EditTenantCommand(tenantId, "Acme Updated", TenantStatus.INACTIVE)))
        .thenReturn(tenant);

    mockMvc.perform(patch("/admin/tenants/" + tenantId)
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Acme Updated"))
        .andExpect(jsonPath("$.status").value("INACTIVE"));
  }

  @Test
  void editTenantReturnsNotFound() throws Exception {
    UUID tenantId = UUID.randomUUID();
    var request = new EditTenantRequestPayload("New", TenantStatus.ACTIVE);

    doThrow(new NotFoundException("Tenant not found: " + tenantId))
        .when(editTenantByIdUseCase)
        .editTenantById(new EditTenantCommand(tenantId, "New", TenantStatus.ACTIVE));

    mockMvc.perform(patch("/admin/tenants/" + tenantId)
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", containsString(tenantId.toString())));
  }

  @Test
  void listTenantsReturnsBadRequestOnInvalidStatus() throws Exception {
    mockMvc.perform(get("/admin/tenants").param("status", "UNKNOWN"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", containsString("Invalid value for 'status'")));
  }

  @Test
  void getTenantByIdReturnsNotFound() throws Exception {
    UUID tenantId = UUID.randomUUID();
    when(getTenantByIdUseCase.getTenantById(tenantId))
        .thenThrow(new NotFoundException("Tenant not found: " + tenantId));

    mockMvc.perform(get("/admin/tenants/" + tenantId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", containsString(tenantId.toString())));
  }

  @Test
  void listTenantsReturnsBadRequestOnServiceValidationError() throws Exception {
    when(listTenantUseCase.listTenants(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new BadRequestException("createdFrom must be before or equal to createdTo"));

    mockMvc.perform(
        get("/admin/tenants")
            .param("createdFrom", "2026-01-02T00:00:00Z")
            .param("createdTo", "2026-01-01T00:00:00Z"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("createdFrom must be before or equal to createdTo"));
  }

  @Test
  void createTenantCallsUseCaseOnce() throws Exception {
    UUID tenantId = UUID.randomUUID();
    Tenant tenant = tenant(tenantId, "slug-1", "Acme Corp", TenantStatus.ACTIVE,
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"), 1L);

    when(createTenantUseCase.createTenant(new CreateTenantCommand("slug-1", "Acme Corp", TenantStatus.ACTIVE)))
        .thenReturn(tenant);

    var request = new CreateTenantRequestPayload("slug-1", "Acme Corp", TenantStatus.ACTIVE);
    mockMvc.perform(post("/admin/tenants")
        .contentType("application/json")
        .content(objectMapper.writeValueAsString(request)));

    verify(createTenantUseCase).createTenant(new CreateTenantCommand("slug-1", "Acme Corp", TenantStatus.ACTIVE));
  }

  private Tenant tenant(
      UUID id,
      String slug,
      String name,
      TenantStatus status,
      Instant createdAt,
      Instant updatedAt,
      Long version) {
    Tenant tenant = org.mockito.Mockito.mock(Tenant.class);
    when(tenant.getId()).thenReturn(id);
    when(tenant.getSlug()).thenReturn(slug);
    when(tenant.getName()).thenReturn(name);
    when(tenant.getStatus()).thenReturn(status);
    when(tenant.getVersion()).thenReturn(version);
    when(tenant.getCreatedAt()).thenReturn(createdAt);
    when(tenant.getUpdatedAt()).thenReturn(updatedAt);
    return tenant;
  }

  private record CreateTenantRequestPayload(String slug, String name, TenantStatus status) {
  }

  private record EditTenantRequestPayload(String name, TenantStatus status) {
  }
}
