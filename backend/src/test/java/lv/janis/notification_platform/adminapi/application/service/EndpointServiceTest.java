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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.CreateEndpointCommand;
import lv.janis.notification_platform.adminapi.application.port.in.ListEndpointQuery;
import lv.janis.notification_platform.adminapi.application.port.in.UpdateEndpointCommand;
import lv.janis.notification_platform.adminapi.application.validation.endpoint.EndpointConfigValidator;
import lv.janis.notification_platform.adminapi.application.validation.endpoint.EndpointConfigValidatorRegistry;
import lv.janis.notification_platform.delivery.application.port.out.EndpointFilter;
import lv.janis.notification_platform.delivery.application.port.out.EndpointRepositoryPort;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static lv.janis.notification_platform.support.EntityTestData.endpoint;
import static lv.janis.notification_platform.support.EntityTestData.tenant;

class EndpointServiceTest {
  private final EndpointRepositoryPort endpointRepository = mock(EndpointRepositoryPort.class);
  private final TenantRepositoryPort tenantRepository = mock(TenantRepositoryPort.class);
  private final EndpointConfigValidatorRegistry validatorRegistry = mock(EndpointConfigValidatorRegistry.class);
  private final EndpointConfigValidator validator = mock(EndpointConfigValidator.class);
  private final EndpointService service = new EndpointService(endpointRepository, tenantRepository, validatorRegistry);

  @Test
  void getEndpointByIdReturnsEndpointWhenPresent() {
    UUID endpointId = UUID.randomUUID();
    Endpoint endpoint = mock(Endpoint.class);
    when(endpointRepository.findById(endpointId)).thenReturn(Optional.of(endpoint));

    Endpoint result = service.getEndpointById(endpointId);

    assertSame(endpoint, result);
  }

  @Test
  void listEndpointsClampsPageAndSizeAndDelegatesFilter() {
    UUID tenantId = UUID.randomUUID();
    Instant from = Instant.parse("2026-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-01-02T00:00:00Z");
    ListEndpointQuery query = new ListEndpointQuery(-1, 500, tenantId, EndpointStatus.ACTIVE, EndpointType.WEBHOOK,
        from, to);
    Page<Endpoint> expected = new PageImpl<>(List.of());
    when(endpointRepository.findAll(any(), any())).thenReturn(expected);

    Page<Endpoint> result = service.listEndpoints(query);

    assertSame(expected, result);

    ArgumentCaptor<EndpointFilter> filterCaptor = ArgumentCaptor.forClass(EndpointFilter.class);
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(endpointRepository).findAll(filterCaptor.capture(), pageableCaptor.capture());
    assertEquals(tenantId, filterCaptor.getValue().tenantId());
    assertEquals(EndpointStatus.ACTIVE, filterCaptor.getValue().status());
    assertEquals(EndpointType.WEBHOOK, filterCaptor.getValue().type());
    assertEquals(from, filterCaptor.getValue().createdFrom());
    assertEquals(to, filterCaptor.getValue().createdTo());
    assertEquals(0, pageableCaptor.getValue().getPageNumber());
    assertEquals(100, pageableCaptor.getValue().getPageSize());
    assertEquals(Sort.Direction.DESC, pageableCaptor.getValue().getSort().getOrderFor("createdAt").getDirection());
  }

  @Test
  void listEndpointsRejectsInvalidDateRange() {
    ListEndpointQuery query = new ListEndpointQuery(
        0,
        10,
        null,
        null,
        null,
        Instant.parse("2026-01-02T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"));

    BadRequestException ex = assertThrows(BadRequestException.class, () -> service.listEndpoints(query));

    assertEquals("createdFrom must be before or equal to createdTo", ex.getMessage());
  }

  @Test
  void createEndpointValidatesTenantAndAddsWebhookTimeoutDefaults() {
    UUID tenantId = UUID.randomUUID();
    Tenant tenant = tenant(tenantId);
    ObjectNode config = JsonNodeFactory.instance.objectNode()
        .put("url", "https://example.com")
        .put("connectTimeoutMs", 1500)
        .put("responseTimeoutMs", 20000);

    when(validatorRegistry.get(EndpointType.WEBHOOK)).thenReturn(validator);
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
    when(endpointRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Endpoint result = service.createEndpoint(new CreateEndpointCommand(tenantId, EndpointType.WEBHOOK, config));

    assertSame(tenant, result.getTenant());
    assertEquals(EndpointType.WEBHOOK, result.getType());
    assertEquals(2000, result.getConfig().get("connectTimeoutMs").asInt());
    assertEquals(10000, result.getConfig().get("responseTimeoutMs").asInt());
    assertEquals(2000, result.getConfig().get("connectionRequestTimeoutMs").asInt());
    verify(validator).validate(config);
  }

  @Test
  void updateEndpointValidatesAndClampsWebhookTimeouts() {
    UUID tenantId = UUID.randomUUID();
    UUID endpointId = UUID.randomUUID();
    Endpoint endpoint = endpoint(endpointId, tenant(tenantId), EndpointType.WEBHOOK);
    ObjectNode config = JsonNodeFactory.instance.objectNode()
        .put("url", "https://example.com")
        .put("connectTimeoutMs", 2800)
        .put("connectionRequestTimeoutMs", 500);

    when(endpointRepository.findById(endpointId)).thenReturn(Optional.of(endpoint));
    when(validatorRegistry.get(EndpointType.WEBHOOK)).thenReturn(validator);
    when(endpointRepository.save(endpoint)).thenReturn(endpoint);

    Endpoint result = service.updateEndpoint(new UpdateEndpointCommand(endpointId, config));

    assertSame(endpoint, result);
    assertEquals(2800, result.getConfig().get("connectTimeoutMs").asInt());
    assertEquals(5000, result.getConfig().get("responseTimeoutMs").asInt());
    assertEquals(1000, result.getConfig().get("connectionRequestTimeoutMs").asInt());
    verify(validator).validate(config);
  }

  @Test
  void deactivateReactivateAndDeleteEndpointPersistStateChanges() {
    UUID endpointId = UUID.randomUUID();
    Endpoint endpoint = endpoint(endpointId, tenant(UUID.randomUUID()), EndpointType.EMAIL);
    when(endpointRepository.findById(endpointId)).thenReturn(Optional.of(endpoint));
    when(endpointRepository.save(endpoint)).thenReturn(endpoint);

    service.deactivateEndpoint(endpointId);
    assertEquals(EndpointStatus.INACTIVE, endpoint.getStatus());

    service.reactivateEndpoint(endpointId);
    assertEquals(EndpointStatus.ACTIVE, endpoint.getStatus());

    service.deleteEndpoint(endpointId);
    assertEquals(EndpointStatus.DISABLED, endpoint.getStatus());
  }

  @Test
  void createEndpointThrowsNotFoundWhenTenantMissing() {
    UUID tenantId = UUID.randomUUID();
    when(validatorRegistry.get(EndpointType.EMAIL)).thenReturn(validator);
    when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(
        NotFoundException.class,
        () -> service.createEndpoint(
            new CreateEndpointCommand(tenantId, EndpointType.EMAIL, JsonNodeFactory.instance.objectNode())));

    assertEquals("Tenant with " + tenantId + " not found", ex.getMessage());
  }
}
