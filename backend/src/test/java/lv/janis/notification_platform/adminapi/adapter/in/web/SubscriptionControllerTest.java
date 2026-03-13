package lv.janis.notification_platform.adminapi.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.CreateSubscriptionCommand;
import lv.janis.notification_platform.adminapi.application.port.in.ListSubscriptionsQuery;
import lv.janis.notification_platform.adminapi.application.port.in.SubscriptionUseCase;
import lv.janis.notification_platform.auth.adapter.in.security.ApiKeyAuthenticationFilter;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.routing.domain.SubscriptionStatus;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubscriptionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private SubscriptionUseCase subscriptionUseCase;

  @MockBean
  private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

  @Test
  void createSubscriptionReturnsCreated() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID endpointId = UUID.randomUUID();
    String eventType = "EVENT_CREATED";
    var endpoint = mockEndpoint(endpointId);
    var subscription = subscription(UUID.randomUUID(), tenantId, eventType, endpoint, SubscriptionStatus.ACTIVE,
        Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    when(subscriptionUseCase.createSubscription(new CreateSubscriptionCommand(tenantId, eventType, endpointId)))
        .thenReturn(subscription);

    CreateSubscriptionRequestPayload request = new CreateSubscriptionRequestPayload(eventType, endpointId);
    mockMvc.perform(
        post("/admin/tenants/" + tenantId + "/subscriptions")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/admin/tenants/" + tenantId + "/subscriptions/" + subscription.getId()))
        .andExpect(jsonPath("$.id").value(subscription.getId().toString()))
        .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
        .andExpect(jsonPath("$.eventType").value(eventType));
  }

  @Test
  void listSubscriptionsReturnsPage() throws Exception {
    UUID tenantId = UUID.randomUUID();
    var tenant = mockTenant(tenantId);
    UUID endpointId = UUID.randomUUID();
    UUID subscriptionId = UUID.randomUUID();
    var endpoint = mockEndpoint(endpointId);
    var subscription = subscription(subscriptionId, tenant, "EVENT_UPDATED", endpoint, SubscriptionStatus.ACTIVE,
        Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    when(subscriptionUseCase.listSubscriptions(org.mockito.ArgumentMatchers.argThat(
        query -> query.tenantId().equals(tenantId) && query.page() == 0 && query.size() == 20
            && query.status() == SubscriptionStatus.ACTIVE)))
        .thenReturn(new PageImpl<>(List.of(subscription), PageRequest.of(0, 20), 1));

    mockMvc.perform(get("/admin/tenants/" + tenantId + "/subscriptions").param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items[0].id").value(subscriptionId.toString()))
        .andExpect(jsonPath("$.items[0].eventType").value("EVENT_UPDATED"));
  }

  @Test
  void deactivateSubscriptionReturnsNoContent() throws Exception {
    UUID subscriptionId = UUID.randomUUID();

    mockMvc.perform(post("/admin/subscriptions/" + subscriptionId + "/deactivate"))
        .andExpect(status().isNoContent());

    verify(subscriptionUseCase).deactivateSubscription(subscriptionId);
  }

  @Test
  void reactivateSubscriptionReturnsNoContent() throws Exception {
    UUID subscriptionId = UUID.randomUUID();

    mockMvc.perform(post("/admin/subscriptions/" + subscriptionId + "/reactivate"))
        .andExpect(status().isNoContent());

    verify(subscriptionUseCase).activateSubscription(subscriptionId);
  }

  @Test
  void deleteSubscriptionReturnsNoContent() throws Exception {
    UUID subscriptionId = UUID.randomUUID();

    mockMvc.perform(post("/admin/subscriptions/" + subscriptionId + "/delete"))
        .andExpect(status().isNoContent());

    verify(subscriptionUseCase).deleteSubscription(subscriptionId);
  }

  @Test
  void listSubscriptionsReturnsBadRequestOnInvalidStatus() throws Exception {
    UUID tenantId = UUID.randomUUID();

    mockMvc.perform(get("/admin/tenants/" + tenantId + "/subscriptions").param("status", "INVALID"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", containsString("Invalid value for 'status'")));
  }

  @Test
  void listSubscriptionsReturnsBadRequestOnServiceValidationError() throws Exception {
    UUID tenantId = UUID.randomUUID();
    when(subscriptionUseCase.listSubscriptions(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new BadRequestException("createdFrom must be before or equal to createdTo"));

    mockMvc.perform(
        get("/admin/tenants/" + tenantId + "/subscriptions")
            .param("createdAfter", "2026-01-02T00:00:00Z")
            .param("createdBefore", "2026-01-01T00:00:00Z"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("createdFrom must be before or equal to createdTo"));
  }

  @Test
  void createSubscriptionReturnsNotFoundOnMissingTenant() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID endpointId = UUID.randomUUID();
    when(subscriptionUseCase.createSubscription(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new NotFoundException("Tenant with " + tenantId + " not found"));

    CreateSubscriptionRequestPayload request = new CreateSubscriptionRequestPayload("EVENT_CREATED", endpointId);
    mockMvc.perform(
        post("/admin/tenants/" + tenantId + "/subscriptions")
            .contentType("application/json")
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", containsString(tenantId.toString())));
  }

  private Subscription subscription(
      UUID id,
      Tenant tenant,
      String eventType,
      Endpoint endpoint,
      SubscriptionStatus status,
      Instant createdAt,
      Instant updatedAt) {
    Subscription subscription = org.mockito.Mockito.mock(Subscription.class);
    when(subscription.getId()).thenReturn(id);
    when(subscription.getTenant()).thenReturn(tenant);
    when(subscription.getEventType()).thenReturn(eventType);
    when(subscription.getStatus()).thenReturn(status);
    when(subscription.getEndpoint()).thenReturn(endpoint);
    when(subscription.getCreatedAt()).thenReturn(createdAt);
    when(subscription.getUpdatedAt()).thenReturn(updatedAt);
    return subscription;
  }

  private Subscription subscription(
      UUID id,
      UUID tenantId,
      String eventType,
      Endpoint endpoint,
      SubscriptionStatus status,
      Instant createdAt,
      Instant updatedAt) {
    Tenant tenant = mockTenant(tenantId);
    return subscription(id, tenant, eventType, endpoint, status, createdAt, updatedAt);
  }

  private Tenant mockTenant(UUID tenantId) {
    Tenant tenant = org.mockito.Mockito.mock(Tenant.class);
    when(tenant.getId()).thenReturn(tenantId);
    return tenant;
  }

  private Endpoint mockEndpoint(UUID endpointId) {
    Endpoint endpoint = org.mockito.Mockito.mock(Endpoint.class);
    when(endpoint.getId()).thenReturn(endpointId);
    return endpoint;
  }

  private record CreateSubscriptionRequestPayload(String eventType, UUID endpointId) {
  }
}
