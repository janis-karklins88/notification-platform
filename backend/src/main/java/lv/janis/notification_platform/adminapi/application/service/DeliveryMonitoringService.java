package lv.janis.notification_platform.adminapi.application.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.DeliveryMonitoringUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.ListDeliveriesQuery;
import lv.janis.notification_platform.delivery.application.port.out.DeliveryFilter;
import lv.janis.notification_platform.delivery.application.port.out.DeliveryRepositoryPort;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.EndpointType;

@Service
public class DeliveryMonitoringService implements DeliveryMonitoringUseCase {
  private static final int MAX_PAGE_SIZE = 100;

  private final DeliveryRepositoryPort deliveryRepositoryPort;

  public DeliveryMonitoringService(DeliveryRepositoryPort deliveryRepositoryPort) {
    this.deliveryRepositoryPort = deliveryRepositoryPort;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Delivery> listDeliveries(ListDeliveriesQuery query) {
    int safePage = Math.max(query.page(), 0);
    int safeSize = Math.min(Math.max(query.size(), 1), MAX_PAGE_SIZE);
    validateTimeRange(query.from(), query.to());
    validateChannel(query.channel());

    Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    DeliveryFilter filter = new DeliveryFilter(
        query.status(),
        query.tenantId(),
        query.eventId(),
        query.endpointId(),
        resolveChannel(query.channel()),
        query.from(),
        query.to());

    return deliveryRepositoryPort.findAll(filter, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Delivery getDeliveryById(UUID deliveryId) {
    return deliveryRepositoryPort.findById(deliveryId)
        .orElseThrow(() -> new NotFoundException("Delivery with id " + deliveryId + " not found"));
  }

  private void validateTimeRange(Instant from, Instant to) {
    if (from != null && to != null && from.isAfter(to)) {
      throw new BadRequestException("from must be before or equal to to");
    }
  }

  private void validateChannel(String channel) {
    if (channel == null || channel.isBlank()) {
      return;
    }
    EndpointType endpointType = resolveChannel(channel);
    if (endpointType != EndpointType.EMAIL && endpointType != EndpointType.WEBHOOK) {
      throw new BadRequestException("Only EMAIL and WEBHOOK channels are supported");
    }
  }

  private EndpointType resolveChannel(String channel) {
    try {
      return EndpointType.valueOf(channel.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException("Invalid channel: " + channel);
    }
  }
}
