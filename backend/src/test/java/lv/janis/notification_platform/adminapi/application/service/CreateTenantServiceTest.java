package lv.janis.notification_platform.adminapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.ConflictException;
import lv.janis.notification_platform.adminapi.application.port.in.CreateTenantCommand;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateTenantServiceTest {
  private final TenantRepositoryPort repository = mock(TenantRepositoryPort.class);
  private final CreateTenantService service = new CreateTenantService(repository);

  @Test
  void createTenantNormalizesInputAndDefaultsStatus() {
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Tenant tenant = service.createTenant(new CreateTenantCommand("  AcMe  ", "  Acme Inc  ", null));

    assertEquals("acme", tenant.getSlug());
    assertEquals("Acme Inc", tenant.getName());
    assertEquals(TenantStatus.ACTIVE, tenant.getStatus());

    ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
    verify(repository).save(tenantCaptor.capture());
    assertSame(tenant, tenantCaptor.getValue());
  }

  @Test
  void createTenantThrowsConflictWhenSlugAlreadyExists() {
    when(repository.existsBySlug("acme")).thenReturn(true);

    ConflictException ex = assertThrows(
        ConflictException.class,
        () -> service.createTenant(new CreateTenantCommand(" Acme ", "Acme", TenantStatus.ACTIVE)));

    assertEquals("Tenant slug already exists: acme", ex.getMessage());
    verify(repository, never()).save(any());
  }

  @Test
  void createTenantRejectsBlankSlug() {
    BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.createTenant(new CreateTenantCommand("   ", "Acme", TenantStatus.ACTIVE)));

    assertEquals("slug must not be blank", ex.getMessage());
  }

  @Test
  void createTenantRejectsBlankName() {
    BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> service.createTenant(new CreateTenantCommand("acme", "   ", TenantStatus.ACTIVE)));

    assertEquals("name must not be blank", ex.getMessage());
  }
}
