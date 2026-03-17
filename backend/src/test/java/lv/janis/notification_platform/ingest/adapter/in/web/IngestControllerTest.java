package lv.janis.notification_platform.ingest.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import static org.hamcrest.Matchers.containsString;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.auth.application.security.ApiKeyPrincipal;
import lv.janis.notification_platform.auth.adapter.in.security.ApiKeyAuthenticationFilter;
import lv.janis.notification_platform.ingest.application.port.in.IngestCommand;
import lv.janis.notification_platform.ingest.application.port.in.IngestResult;
import lv.janis.notification_platform.ingest.application.port.in.IngestUseCase;
import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.ingest.domain.EventStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IngestController.class)
@AutoConfigureMockMvc(addFilters = false)
class IngestControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private IngestUseCase ingestUseCase;

  @MockitoBean
  private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void ingestReturnsCreated() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID apiKeyId = UUID.randomUUID();
    JsonNode payload = objectMapper.createObjectNode().put("email", "one@example.com");
    IngestResult result = new IngestResult(UUID.randomUUID(), EventStatus.RECEIVED, false);

    when(ingestUseCase.ingest(new IngestCommand(
        tenantId,
        "USER_CREATED",
        payload,
        "idem-1",
        "ingest-service",
        "trace-1")))
        .thenReturn(result);

    withAuthentication(tenantId, apiKeyId);

    mockMvc.perform(post("/ingest").contentType("application/json")
        .content(
            objectMapper.writeValueAsString(
                new IngestRequestPayload("USER_CREATED", payload, "idem-1", "ingest-service", "trace-1"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.eventId").value(result.eventId().toString()))
        .andExpect(jsonPath("$.status").value("RECEIVED"))
        .andExpect(jsonPath("$.duplicate").value(false));
  }

  @Test
  void ingestReturnsOkWhenDuplicate() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID apiKeyId = UUID.randomUUID();
    JsonNode payload = objectMapper.createObjectNode().put("email", "one@example.com");
    IngestResult result = new IngestResult(UUID.randomUUID(), EventStatus.RECEIVED, true);

    when(ingestUseCase.ingest(new IngestCommand(
        tenantId,
        "USER_CREATED",
        payload,
        "idem-1",
        "ingest-service",
        "trace-1")))
        .thenReturn(result);

    withAuthentication(tenantId, apiKeyId);

    mockMvc.perform(post("/ingest").contentType("application/json")
        .content(
            objectMapper.writeValueAsString(
                new IngestRequestPayload("USER_CREATED", payload, "idem-1", "ingest-service", "trace-1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventId").value(result.eventId().toString()))
        .andExpect(jsonPath("$.duplicate").value(true));
  }

  @Test
  void ingestReturnsBadRequestOnServiceValidationError() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID apiKeyId = UUID.randomUUID();
    JsonNode payload = objectMapper.createObjectNode().put("email", "one@example.com");
    when(ingestUseCase.ingest(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new BadRequestException("payload too large"));

    withAuthentication(tenantId, apiKeyId);

    mockMvc.perform(post("/ingest").contentType("application/json")
        .content(
            objectMapper.writeValueAsString(new IngestRequestPayload("USER_CREATED", payload, "idem-1", null, null))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("payload too large"));
  }

  @Test
  void ingestReturnsBadRequestOnValidationError() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID apiKeyId = UUID.randomUUID();

    withAuthentication(tenantId, apiKeyId);

    mockMvc.perform(post("/ingest").contentType("application/json")
        .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getEventReturnsMapping() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID apiKeyId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    Event event = event(eventId, tenantId, "USER_CREATED", EventStatus.RECEIVED, now, now, "src", "trace", "idem");

    when(ingestUseCase.getEventById(tenantId, eventId)).thenReturn(event);

    withAuthentication(tenantId, apiKeyId);

    mockMvc.perform(get("/ingest/events/" + eventId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventId").value(eventId.toString()))
        .andExpect(jsonPath("$.eventType").value("USER_CREATED"))
        .andExpect(jsonPath("$.status").value("RECEIVED"))
        .andExpect(jsonPath("$.source").value("src"));
  }

  @Test
  void getEventReturnsNotFound() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID apiKeyId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();

    when(ingestUseCase.getEventById(tenantId, eventId))
        .thenThrow(new NotFoundException("Event with id " + eventId + " not found"));

    withAuthentication(tenantId, apiKeyId);

    mockMvc.perform(get("/ingest/events/" + eventId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", containsString(eventId.toString())));
  }

  @Test
  void getEventCallsUseCase() throws Exception {
    UUID tenantId = UUID.randomUUID();
    UUID apiKeyId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    Event event = event(eventId, tenantId, "USER_CREATED", EventStatus.RECEIVED,
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"), "src", "trace", "idem");

    when(ingestUseCase.getEventById(tenantId, eventId)).thenReturn(event);

    withAuthentication(tenantId, apiKeyId);

    mockMvc.perform(get("/ingest/events/" + eventId));
    verify(ingestUseCase).getEventById(tenantId, eventId);
  }

  private void withAuthentication(UUID tenantId, UUID apiKeyId) {
    Authentication authentication = new UsernamePasswordAuthenticationToken(new ApiKeyPrincipal(apiKeyId, tenantId), null);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private Event event(
      UUID id,
      UUID tenantId,
      String eventType,
      EventStatus status,
      Instant receivedAt,
      Instant updatedAt,
      String source,
      String traceId,
      String idempotencyKey) {
    Event event = org.mockito.Mockito.mock(Event.class);
    when(event.getId()).thenReturn(id);
    when(event.getTenantId()).thenReturn(tenantId);
    when(event.getEventType()).thenReturn(eventType);
    when(event.getStatus()).thenReturn(status);
    when(event.getReceivedAt()).thenReturn(receivedAt);
    when(event.getUpdatedAt()).thenReturn(updatedAt);
    when(event.getSource()).thenReturn(source);
    when(event.getTraceId()).thenReturn(traceId);
    when(event.getIdempotencyKey()).thenReturn(idempotencyKey);
    return event;
  }

  private record IngestRequestPayload(
      String eventType,
      JsonNode payload,
      String idempotencyKey,
      String source,
      String traceId) {
  }
}
