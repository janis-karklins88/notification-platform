package lv.janis.notification_platform.adminapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.NotFoundException;
import lv.janis.notification_platform.adminapi.application.port.in.EditTenantCommand;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;
import org.junit.jupiter.api.Test;

import static lv.janis.notification_platform.support.EntityTestData.tenant;

class EditTenantByIdServiceTest {
  private final TenantRepositoryPort repository = mock(TenantRepositoryPort.class);
  private final EditTenantByIdService service = new EditTenantByIdService(repository);

  @Test
  void editTenantByIdUpdatesProvidedFieldsAndSaves() {
    UUID tenantId = UUID.randomUUID();
    Tenant existing = tenant(tenantId, TenantStatus.ACTIVE);
    when(repository.findById(tenantId)).thenReturn(Optional.of(existing));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Tenant result = service.editTenantById(new EditTenantCommand(tenantId, "Updated Name", TenantStatus.SUSPENDED));

    assertSame(existing, result);
    assertEquals("Updated Name", existing.getName());
    assertEquals(TenantStatus.SUSPENDED, existing.getStatus());
    verify(repository).save(existing);
  }

  @Test
  void editTenantByIdRejectsMissingUpdates() {
    UUID tenantId = UUID.randomUUID();
    when(repository.findById(tenantId)).thenReturn(Optional.of(tenant(tenantId)));

    BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.editTenantById(new EditTenantCommand(tenantId, null, null)));

    assertEquals("Name or Status must be provided!", ex.getMessage());
    verify(repository, never()).save(any());
  }

  @Test
  void editTenantByIdThrowsNotFoundWhenMissing() {
    UUID tenantId = UUID.randomUUID();
    when(repository.findById(tenantId)).thenReturn(Optional.empty());

    NotFoundException ex = assertThrows(
        NotFoundException.class,
        () -> service.editTenantById(new EditTenantCommand(tenantId, "Name", null)));

    assertEquals("Tenant not found: " + tenantId, ex.getMessage());
  }
}
