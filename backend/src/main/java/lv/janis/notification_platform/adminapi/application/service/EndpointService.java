package lv.janis.notification_platform.adminapi.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.CreateEndpointCommand;
import lv.janis.notification_platform.adminapi.application.port.in.EndpointUseCase;
import lv.janis.notification_platform.adminapi.application.port.in.ListEndpointQuery;
import lv.janis.notification_platform.adminapi.application.port.in.UpdateEndpointCommand;
import lv.janis.notification_platform.adminapi.application.validation.endpoint.EndpointConfigValidatorRegistry;
import lv.janis.notification_platform.delivery.application.port.out.EndpointFilter;
import lv.janis.notification_platform.delivery.application.port.out.EndpointRepositoryPort;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;

@Service
public class EndpointService implements EndpointUseCase {
  private static final int MAX_PAGE_SIZE = 100;

  private final EndpointRepositoryPort endpointRepositoryPort;
  private final TenantRepositoryPort tenantRepositoryPort;
  private final EndpointConfigValidatorRegistry endpointConfigValidatorRegistry;

  public EndpointService(
      EndpointRepositoryPort endpointRepositoryPort,
      TenantRepositoryPort tenantRepositoryPort,
      EndpointConfigValidatorRegistry endpointConfigValidatorRegistry) {
    this.endpointRepositoryPort = endpointRepositoryPort;
    this.tenantRepositoryPort = tenantRepositoryPort;
    this.endpointConfigValidatorRegistry = endpointConfigValidatorRegistry;
  }

  @Override
  @Transactional(readOnly = true)
  public Endpoint getEndpointById(UUID id) {
    return endpointRepositoryPort.findById(id)
        .orElseThrow(() -> new NotFoundException("Endpoint with " + id + " not found"));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Endpoint> listEndpoints(ListEndpointQuery query) {
    int safePage = Math.max(query.page(), 0);
    int safeSize = Math.min(Math.max(query.size(), 1), MAX_PAGE_SIZE);
    if (query.createdFrom() != null && query.createdTo() != null && query.createdFrom().isAfter(query.createdTo())) {
      throw new BadRequestException("createdFrom must be before or equal to createdTo");
    }

    Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    EndpointFilter filter = new EndpointFilter(
        query.tenantId(),
        query.status(),
        query.type(),
        query.createdFrom(),
        query.createdTo());
    return endpointRepositoryPort.findAll(filter, pageable);
  }

  @Override
  @Transactional
  public Endpoint createEndpoint(CreateEndpointCommand command) {
    endpointConfigValidatorRegistry.get(command.type()).validate(command.config());
    var tenant = tenantRepositoryPort.findById(command.tenantId())
        .orElseThrow(() -> new NotFoundException("Tenant with " + command.tenantId() + " not found"));

    Endpoint endpoint = new Endpoint(tenant, command.type(), command.config());
    return endpointRepositoryPort.save(endpoint);
  }

  @Override
  @Transactional
  public Endpoint updateEndpoint(UpdateEndpointCommand command) {
    var endpoint = endpointRepositoryPort.findById(command.endpointId())
        .orElseThrow(() -> new NotFoundException("Endpoint with " + command.endpointId() + " not found"));
    endpointConfigValidatorRegistry.get(endpoint.getType()).validate(command.config());
    endpoint.setConfig(command.config());
    return endpointRepositoryPort.save(endpoint);
  }

  @Override
  @Transactional
  public void deactivateEndpoint(UUID endpointId) {
    var endpoint = endpointRepositoryPort.findById(endpointId)
        .orElseThrow(() -> new NotFoundException("Endpoint with " + endpointId + " not found"));
    endpoint.deactivate();
    endpointRepositoryPort.save(endpoint);
  }

  @Override
  @Transactional
  public void reactivateEndpoint(UUID endpointId) {
    var endpoint = endpointRepositoryPort.findById(endpointId)
        .orElseThrow(() -> new NotFoundException("Endpoint with " + endpointId + " not found"));
    endpoint.activate();
    endpointRepositoryPort.save(endpoint);
  }

  @Override
  @Transactional
  public void deleteEndpoint(UUID endpointId) {
    var endpoint = endpointRepositoryPort.findById(endpointId)
        .orElseThrow(() -> new NotFoundException("Endpoint with " + endpointId + " not found"));
    endpoint.delete();
    endpointRepositoryPort.save(endpoint);
  }
}
