package lv.janis.notification_platform.adminapi.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
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
import com.fasterxml.jackson.databind.node.ObjectNode;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.CreateEndpointCommand;
import lv.janis.notification_platform.adminapi.application.port.in.EndpointUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.UpdateEndpointCommand;
import lv.janis.notification_platform.auth.adapter.in.security.ApiKeyAuthenticationFilter;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EndpointAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class EndpointAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private EndpointUseCase endpointUseCase;

  @MockBean
  private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

  @Test
  void createEndpointReturnsCreated() throws Exception {
    UUID tenantId = UUID.randomUUID();
    ObjectNode config = objectMapper.createObjectNode().put("url", "https://example.com");
    var endpoint = endpoint(tenantId, UUID.randomUUID(), EndpointType.WEBHOOK, EndpointStatus.ACTIVE, config, Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"));

    when(endpointUseCase.createEndpoint(new CreateEndpointCommand(tenantId, EndpointType.WEBHOOK, config)))
        .thenReturn(endpoint);

    mockMvc.perform(
        post("/admin/tenants/" + tenantId + "/endpoints").contentType("application/json")
            .content(objectMapper.writeValueAsString(new CreateEndpointRequestPayload(EndpointType.WEBHOOK, config))))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/admin/endpoints/" + endpoint.getId()))
        .andExpect(jsonPath("$.id").value(endpoint.getId().toString()))
        .andExpect(jsonPath("$.type").value("WEBHOOK"));
  }

  @Test
  void getEndpointReturnsMapping() throws Exception {
    UUID endpointId = UUID.randomUUID();
    var endpoint = endpoint(UUID.randomUUID(), endpointId, EndpointType.EMAIL, EndpointStatus.INACTIVE,
        objectMapper.createObjectNode().put("from", "noreply@example.com"), Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"));

    when(endpointUseCase.getEndpointById(endpointId)).thenReturn(endpoint);

    mockMvc.perform(get("/admin/endpoints/" + endpointId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(endpointId.toString()))
        .andExpect(jsonPath("$.status").value("INACTIVE"));
  }

  @Test
  void updateEndpointReturnsMapping() throws Exception {
    UUID endpointId = UUID.randomUUID();
    ObjectNode config = objectMapper.createObjectNode().put("url", "https://updated.example.com");
    var endpoint = endpoint(UUID.randomUUID(), endpointId, EndpointType.WEBHOOK, EndpointStatus.ACTIVE, config,
        Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));

    when(endpointUseCase.updateEndpoint(new UpdateEndpointCommand(endpointId, config))).thenReturn(endpoint);

    mockMvc.perform(
        patch("/admin/endpoints/" + endpointId)
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(new UpdateEndpointRequestPayload(config))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(endpointId.toString()));
  }

  @Test
  void deactivateEndpointReturnsNoContent() throws Exception {
    UUID endpointId = UUID.randomUUID();

    mockMvc.perform(post("/admin/endpoints/" + endpointId + "/deactivate"))
        .andExpect(status().isNoContent());

    verify(endpointUseCase).deactivateEndpoint(endpointId);
  }

  @Test
  void reactivateEndpointReturnsNoContent() throws Exception {
    UUID endpointId = UUID.randomUUID();

    mockMvc.perform(post("/admin/endpoints/" + endpointId + "/reactivate"))
        .andExpect(status().isNoContent());

    verify(endpointUseCase).reactivateEndpoint(endpointId);
  }

  @Test
  void deleteEndpointReturnsNoContent() throws Exception {
    UUID endpointId = UUID.randomUUID();

    mockMvc.perform(post("/admin/endpoints/" + endpointId + "/delete"))
        .andExpect(status().isNoContent());

    verify(endpointUseCase).deleteEndpoint(endpointId);
  }

  @Test
  void listEndpointsReturnsPage() throws Exception {
    var endpoint = endpoint(UUID.randomUUID(), UUID.randomUUID(), EndpointType.EMAIL, EndpointStatus.ACTIVE,
        objectMapper.createObjectNode().put("from", "noreply@example.com"), Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"));
    when(endpointUseCase.listEndpoints(org.mockito.ArgumentMatchers.argThat(query ->
        query.page() == 0 && query.size() == 20 && query.status() == EndpointStatus.ACTIVE)))
        .thenReturn(new PageImpl<>(List.of(endpoint), PageRequest.of(0, 20), 1));

    mockMvc.perform(get("/admin/endpoints").param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items[0].type").value("EMAIL"));
  }

  @Test
  void listEndpointsReturnsBadRequestOnServiceValidationError() throws Exception {
    when(endpointUseCase.listEndpoints(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new BadRequestException("createdFrom must be before or equal to createdTo"));

    mockMvc.perform(
        get("/admin/endpoints").param("createdFrom", "2026-01-02T00:00:00Z").param("createdTo", "2026-01-01T00:00:00Z"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("createdFrom must be before or equal to createdTo"));
  }

  @Test
  void getEndpointReturnsNotFound() throws Exception {
    UUID endpointId = UUID.randomUUID();
    when(endpointUseCase.getEndpointById(endpointId)).thenThrow(new NotFoundException("Endpoint with " + endpointId + " not found"));

    mockMvc.perform(get("/admin/endpoints/" + endpointId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", containsString(endpointId.toString())));
  }

  @Test
  void listEndpointsReturnsBadRequestOnInvalidType() throws Exception {
    mockMvc.perform(get("/admin/endpoints").param("type", "INVALID"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", containsString("Invalid value for 'type'")));
  }

  private Endpoint endpoint(
      UUID tenantId,
      UUID id,
      EndpointType type,
      EndpointStatus status,
      ObjectNode config,
      Instant createdAt,
      Instant updatedAt) {
    Endpoint endpoint = org.mockito.Mockito.mock(Endpoint.class);
    when(endpoint.getTenantId()).thenReturn(tenantId);
    when(endpoint.getId()).thenReturn(id);
    when(endpoint.getType()).thenReturn(type);
    when(endpoint.getStatus()).thenReturn(status);
    when(endpoint.getConfig()).thenReturn(config);
    when(endpoint.getCreatedAt()).thenReturn(createdAt);
    when(endpoint.getUpdatedAt()).thenReturn(updatedAt);
    return endpoint;
  }

  private record CreateEndpointRequestPayload(EndpointType type, ObjectNode config) {
  }

  private record UpdateEndpointRequestPayload(ObjectNode config) {
  }
}
