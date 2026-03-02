package lv.janis.notification_platform.adminapi.application.port.in;

import java.util.UUID;

import org.springframework.data.domain.Page;

import lv.janis.notification_platform.delivery.domain.Endpoint;

public interface EndpointUseCase {
  Endpoint getEndpointById(UUID id);

  Page<Endpoint> listEndpoints(ListEndpointQuery query);

  Endpoint createEndpoint(CreateEndpointCommand command);

  Endpoint updateEndpoint(UpdateEndpointCommand command);

  void deactivateEndpoint(UUID endpointId);

  void deleteEndpoint(UUID endpointId);

  void reactivateEndpoint(UUID endpointId);
}
