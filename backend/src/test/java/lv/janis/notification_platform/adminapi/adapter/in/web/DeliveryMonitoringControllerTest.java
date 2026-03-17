package lv.janis.notification_platform.adminapi.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.DeliveryMonitoringUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.ListDeliveriesQuery;
import lv.janis.notification_platform.auth.adapter.in.security.ApiKeyAuthenticationFilter;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.DeliveryStatus;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeliveryMonitoringController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliveryMonitoringControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DeliveryMonitoringUseCase deliveryMonitoringUseCase;

  @MockitoBean
  private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

  @Test
  void listDeliveriesReturnsPage() throws Exception {
    Delivery item = delivery(
        UUID.randomUUID(),
        DeliveryStatus.PENDING,
        EndpointType.EMAIL);

    when(deliveryMonitoringUseCase.listDeliveries(argThat(query ->
        query.page() == 0
            && query.size() == 20
            && query.status() == DeliveryStatus.PENDING))).thenReturn(
                new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

    mockMvc.perform(get("/admin/deliveries").param("status", "PENDING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items[0].status").value("PENDING"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void getDeliveryReturnsMapping() throws Exception {
    UUID deliveryId = UUID.randomUUID();
    Delivery item = delivery(deliveryId, DeliveryStatus.DELIVERED, EndpointType.WEBHOOK);

    when(deliveryMonitoringUseCase.getDeliveryById(deliveryId)).thenReturn(item);

    mockMvc.perform(get("/admin/deliveries/" + deliveryId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(deliveryId.toString()))
        .andExpect(jsonPath("$.status").value("DELIVERED"))
        .andExpect(jsonPath("$.channel").value("WEBHOOK"));
  }

  @Test
  void listDeliveriesReturnsBadRequestForUnsupportedChannel() throws Exception {
    when(deliveryMonitoringUseCase.listDeliveries(new ListDeliveriesQuery(
        0,
        20,
        null,
        null,
        null,
        null,
        "SMS",
        null,
        null))).thenThrow(new BadRequestException("Only EMAIL and WEBHOOK channels are supported"));

    mockMvc.perform(get("/admin/deliveries").param("channel", "SMS"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Only EMAIL and WEBHOOK channels are supported"));
  }

  @Test
  void listDeliveriesReturnsBadRequestOnServiceValidationError() throws Exception {
    when(deliveryMonitoringUseCase.listDeliveries(new ListDeliveriesQuery(
        0,
        20,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2026-01-02T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z")))).thenThrow(
            new BadRequestException("from must be before or equal to to"));

    mockMvc.perform(
        get("/admin/deliveries")
            .param("from", "2026-01-02T00:00:00Z")
            .param("to", "2026-01-01T00:00:00Z"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("from must be before or equal to to"));
  }

  @Test
  void getDeliveryReturnsNotFound() throws Exception {
    UUID deliveryId = UUID.randomUUID();
    when(deliveryMonitoringUseCase.getDeliveryById(deliveryId))
        .thenThrow(new NotFoundException("Delivery not found: " + deliveryId));

    mockMvc.perform(get("/admin/deliveries/" + deliveryId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error", containsString(deliveryId.toString())));
  }

  @Test
  void listDeliveriesReturnsBadRequestOnInvalidStatus() throws Exception {
    mockMvc.perform(get("/admin/deliveries").param("status", "NOT_A_STATUS"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error", containsString("Invalid value for 'status'")));
  }

  private Delivery delivery(UUID id, DeliveryStatus status, EndpointType channel) {
    Delivery delivery = org.mockito.Mockito.mock(Delivery.class);
    Endpoint endpoint = org.mockito.Mockito.mock(Endpoint.class);
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    when(delivery.getId()).thenReturn(id);
    when(delivery.getTenantId()).thenReturn(UUID.randomUUID());
    when(delivery.getEventId()).thenReturn(UUID.randomUUID());
    when(delivery.getEndpointId()).thenReturn(UUID.randomUUID());
    when(delivery.getStatus()).thenReturn(status);
    when(delivery.getLastAttemptAt()).thenReturn(now);
    when(delivery.getLastError()).thenReturn("error");
    when(delivery.getCreatedAt()).thenReturn(now);
    when(delivery.getUpdatedAt()).thenReturn(now);
    when(endpoint.getType()).thenReturn(channel);
    when(delivery.getEndpoint()).thenReturn(endpoint);
    return delivery;
  }
}
