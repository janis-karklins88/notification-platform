package lv.janis.notification_platform.adminapi.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.CreateSubscriptionCommand;
import lv.janis.notification_platform.adminapi.application.port.in.ListSubscriptionsQuery;
import lv.janis.notification_platform.adminapi.application.port.in.SubscriptionUseCase;
import lv.janis.notification_platform.delivery.application.port.out.EndpointRepositoryPort;
import lv.janis.notification_platform.routing.application.port.out.SubscriptionFilter;
import lv.janis.notification_platform.routing.application.port.out.SubscriptionRepositoryPort;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;

@Service
public class SubscriptionService implements SubscriptionUseCase {
  private final SubscriptionRepositoryPort subscriptionRepository;
  private final EndpointRepositoryPort endpointRepositoryPort;
  private final TenantRepositoryPort tenantRepositoryPort;
  private static final int MAX_PAGE_SIZE = 100;

  public SubscriptionService(SubscriptionRepositoryPort subscriptionRepository,
      EndpointRepositoryPort endpointRepositoryPort,
      TenantRepositoryPort tenantRepositoryPort) {
    this.subscriptionRepository = subscriptionRepository;
    this.endpointRepositoryPort = endpointRepositoryPort;
    this.tenantRepositoryPort = tenantRepositoryPort;
  }

  @Override
  public Subscription createSubscription(CreateSubscriptionCommand command) {
    var tenant = tenantRepositoryPort.findById(command.tenantId())
        .orElseThrow(() -> new NotFoundException("Tenant with id " + command.tenantId() + " not found"));

    var endpoint = endpointRepositoryPort.findById(command.endpointId())
        .orElseThrow(() -> new NotFoundException("Endpoint with id " + command.endpointId() + " not found"));

    if (!endpoint.getTenant().getId().equals(command.tenantId())) {
      throw new BadRequestException("Endpoint does not belong to the specified tenant");
    }

    Subscription subscription = new Subscription(
        tenant,
        command.eventType(),
        endpoint);
    return subscriptionRepository.save(subscription);
  }

  @Override
  public Page<Subscription> listSubscriptions(ListSubscriptionsQuery query) {
    int safePage = Math.max(query.page(), 0);
    int safeSize = Math.min(Math.max(query.size(), 1), MAX_PAGE_SIZE);
    if (query.createdFrom() != null && query.createdTo() != null && query.createdFrom().isAfter(query.createdTo())) {
      throw new BadRequestException("createdFrom must be before or equal to createdTo");
    }

    Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

    var filter = new SubscriptionFilter(
        query.tenantId(),
        query.eventType(),
        query.endpointId(),
        query.status(),
        query.createdFrom(),
        query.createdTo());
    return subscriptionRepository.findAll(filter, pageable);

  }

  @Override
  public void deleteSubscription(UUID subscriptionId) {
    var subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> new NotFoundException("Subscription with id " + subscriptionId + " not found"));
    subscription.delete();
    subscriptionRepository.save(subscription);
  }

  @Override
  public void activateSubscription(UUID subscriptionId) {
    var subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> new NotFoundException("Subscription with id " + subscriptionId + " not found"));
    subscription.activate();
    subscriptionRepository.save(subscription);
  }

  @Override
  public void deactivateSubscription(UUID subscriptionId) {
    var subscription = subscriptionRepository.findById(subscriptionId)
        .orElseThrow(() -> new NotFoundException("Subscription with id " + subscriptionId + " not found"));
    subscription.pause();
    subscriptionRepository.save(subscription);
  }

}
