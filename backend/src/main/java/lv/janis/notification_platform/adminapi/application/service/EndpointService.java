package lv.janis.notification_platform.adminapi.application.service;

import org.springframework.stereotype.Service;

import lv.janis.notification_platform.adminapi.application.port.in.EndpointUseCase;
import lv.janis.notification_platform.delivery.application.port.out.EndpointRepositoryPort;

@Service
public class EndpointService implements EndpointUseCase {
  private final EndpointRepositoryPort endpointRepositoryPort;

  public EndpointService(EndpointRepositoryPort endpointRepositoryPort) {
    this.endpointRepositoryPort = endpointRepositoryPort;
  }

}
