package lv.janis.notification_platform.adminapi.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.ListOutboxEventsQuery;
import lv.janis.notification_platform.adminapi.application.port.in.OutboxMonitoringUseCase;
import lv.janis.notification_platform.auth.adapter.in.security.ApiKeyAuthenticationFilter;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OutboxMonitoringController.class)
@AutoConfigureMockMvc(addFilters = false)
class OutboxMonitoringControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private OutboxMonitoringUseCase outboxMonitoringUseCase;

  @MockitoBean
  private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

  @Test
  void listOutboxEventsReturnsPage() throws Exception {
    OutboxEvent item = event(
        UUID.randomUUID(),
        OutboxStatus.PENDING,
        OutboxEventAggregateType.EVENT,
        OutboxEventType.EVENT_ACCEPTED);

    when(outboxMonitoringUseCase.listOutboxEvents(new ListOutboxEventsQuery(
        0,
        20,
        OutboxStatus.PENDING,
        null,
        null,
        null,
        null,
        null,
        null))).thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

    mockMvc.perform(get("/admin/outbox-events").param("status", "PENDING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items[0].status").value("PENDING"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void getOutboxEventReturnsMapping() throws Exception {
    UUID outboxEventId = UUID.randomUUID();
    OutboxEvent item = event(
        outboxEventId,
        OutboxStatus.PUBLISHED,
        OutboxEventAggregateType.DELIVERY,
        OutboxEventType.DELIVERY_CREATED_WEBHOOK);

    when(outboxMonitoringUseCase.getOutboxEventById(outboxEventId)).thenReturn(item);

    mockMvc.perform(get("/admin/outbox-events/" + outboxEventId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(outboxEventId.toString()))
        .andExpect(jsonPath("$.status").value("PUBLISHED"));
  }

  @Test
  void listOutboxEventsReturnsBadRequestOnServiceValidationError() throws Exception {
    when(outboxMonitoringUseCase.listOutboxEvents(new ListOutboxEventsQuery(
        0,
        20,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2026-01-02T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z")))).thenThrow(new BadRequestException("from must be before or equal to to"));

    mockMvc.perform(
        get("/admin/outbox-events")
            .param("from", "2026-01-02T00:00:00Z")
            .param("to", "2026-01-01T00:00:00Z"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("from must be before or equal to to"));
  }

  @Test
  void getOutboxEventReturnsNotFound() throws Exception {
    UUID outboxEventId = UUID.randomUUID();
    when(outboxMonitoringUseCase.getOutboxEventById(outboxEventId))
        .thenThrow(new NotFoundException("Outbox event with id " + outboxEventId + " not found"));

    mockMvc.perform(get("/admin/outbox-events/" + outboxEventId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", containsString(outboxEventId.toString())));
  }

  @Test
  void listOutboxEventsReturnsBadRequestOnInvalidStatus() throws Exception {
    mockMvc.perform(get("/admin/outbox-events").param("status", "NOT_A_STATUS"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", containsString("Invalid value for 'status'")));
  }

  private OutboxEvent event(UUID id, OutboxStatus status, OutboxEventAggregateType aggregateType, OutboxEventType eventType) {
    OutboxEvent event = org.mockito.Mockito.mock(OutboxEvent.class);
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    when(event.getId()).thenReturn(id);
    when(event.getTenantId()).thenReturn(UUID.randomUUID());
    when(event.getAggregateType()).thenReturn(aggregateType);
    when(event.getAggregateId()).thenReturn(UUID.randomUUID());
    when(event.getEventType()).thenReturn(eventType);
    when(event.getStatus()).thenReturn(status);
    when(event.getAttemptCount()).thenReturn(2);
    when(event.getAvailableAt()).thenReturn(now);
    when(event.getLastAttemptAt()).thenReturn(now);
    when(event.getPublishedAt()).thenReturn(now);
    when(event.getLastError()).thenReturn("error");
    when(event.getCreatedAt()).thenReturn(now);
    when(event.getUpdatedAt()).thenReturn(now);
    return event;
  }
}
