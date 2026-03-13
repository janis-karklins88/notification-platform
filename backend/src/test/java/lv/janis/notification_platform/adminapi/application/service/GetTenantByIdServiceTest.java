package lv.janis.notification_platform.adminapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;

class GetTenantByIdServiceTest {
  private final TenantRepositoryPort repository = mock(TenantRepositoryPort.class);
  private final GetTenantByIdService service = new GetTenantByIdService(repository);

  @Test
  void getTenantByIdReturnsTenantWhenPresent() {
    UUID tenantId = UUID.randomUUID();
    Tenant tenant = mock(Tenant.class);
    when(repository.findById(tenantId)).thenReturn(Optional.of(tenant));

    Tenant result = service.getTenantById(tenantId);

    assertSame(tenant, result);
  }

  @Test
  void getTenantByIdThrowsNotFoundWhenMissing() {
    UUID tenantId = UUID.randomUUID();
    when(repository.findById(tenantId)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getTenantById(tenantId));

    assertEquals("Tenant not found: " + tenantId, ex.getMessage());
  }
}
