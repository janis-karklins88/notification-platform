package lv.janis.notification_platform.adminapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.ListDeliveriesQuery;
import lv.janis.notification_platform.delivery.application.port.out.DeliveryFilter;
import lv.janis.notification_platform.delivery.application.port.out.DeliveryRepositoryPort;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.DeliveryStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class DeliveryMonitoringServiceTest {
  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID EVENT_ID = UUID.randomUUID();
  private static final UUID ENDPOINT_ID = UUID.randomUUID();

  private final DeliveryRepositoryPort repository = mock(DeliveryRepositoryPort.class);
  private final DeliveryMonitoringService service = new DeliveryMonitoringService(repository);

  @Test
  void listDeliveriesClampsPageAndSizeAndDelegatesFilter() {
    Instant from = Instant.parse("2026-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-01-02T00:00:00Z");
    ListDeliveriesQuery query = new ListDeliveriesQuery(
        -1,
        500,
        DeliveryStatus.PENDING,
        TENANT_ID,
        EVENT_ID,
        ENDPOINT_ID,
        "webhook",
        from,
        to);

    Page<Delivery> expected = new PageImpl<>(List.of());
    when(repository.findAll(any(), any())).thenReturn(expected);

    Page<Delivery> result = service.listDeliveries(query);

    assertSame(expected, result);

    ArgumentCaptor<DeliveryFilter> filterCaptor = ArgumentCaptor.forClass(DeliveryFilter.class);
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).findAll(filterCaptor.capture(), pageableCaptor.capture());

    DeliveryFilter filter = filterCaptor.getValue();
    Pageable pageable = pageableCaptor.getValue();
    assertEquals(0, pageable.getPageNumber());
    assertEquals(100, pageable.getPageSize());
    assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
    assertEquals(DeliveryStatus.PENDING, filter.status());
    assertEquals(TENANT_ID, filter.tenantId());
    assertEquals(EVENT_ID, filter.eventId());
    assertEquals(ENDPOINT_ID, filter.endpointId());
    assertEquals(EndpointType.WEBHOOK, filter.channel());
    assertEquals(from, filter.from());
    assertEquals(to, filter.to());
  }

  @Test
  void listDeliveriesRejectsInvalidTimeRange() {
    ListDeliveriesQuery query = new ListDeliveriesQuery(
        0,
        10,
        null,
        null,
        null,
        null,
        null,
        Instant.parse("2026-01-02T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"));

    BadRequestException ex = assertThrows(BadRequestException.class, () -> service.listDeliveries(query));
    assertEquals("from must be before or equal to to", ex.getMessage());
  }

  @Test
  void listDeliveriesRejectsUnsupportedChannel() {
    ListDeliveriesQuery query = new ListDeliveriesQuery(0, 20, null, null, null, null, "SMS", null, null);

    BadRequestException ex = assertThrows(BadRequestException.class, () -> service.listDeliveries(query));
    assertEquals("Only EMAIL and WEBHOOK channels are supported", ex.getMessage());
  }

  @Test
  void listDeliveriesRejectsInvalidChannelValue() {
    ListDeliveriesQuery query = new ListDeliveriesQuery(0, 20, null, null, null, null, "invalid", null, null);

    BadRequestException ex = assertThrows(BadRequestException.class, () -> service.listDeliveries(query));
    assertEquals("Invalid channel: invalid", ex.getMessage());
  }

  @Test
  void getDeliveryByIdReturnsDeliveryWhenPresent() {
    UUID deliveryId = UUID.randomUUID();
    Delivery delivery = mock(Delivery.class);
    when(repository.findById(deliveryId)).thenReturn(Optional.of(delivery));

    Delivery result = service.getDeliveryById(deliveryId);

    assertSame(delivery, result);
    verify(repository).findById(deliveryId);
  }

  @Test
  void getDeliveryByIdThrowsNotFoundWhenMissing() {
    UUID deliveryId = UUID.randomUUID();
    when(repository.findById(deliveryId)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getDeliveryById(deliveryId));
    assertEquals("Delivery with id " + deliveryId + " not found", ex.getMessage());
  }
}
