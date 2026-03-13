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

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.port.in.ListTenantsQuery;
import lv.janis.notification_platform.tenant.application.port.out.TenantFilter;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class ListTenantsServiceTest {
  private final TenantRepositoryPort repository = mock(TenantRepositoryPort.class);
  private final ListTenantsService service = new ListTenantsService(repository);

  @Test
  void listTenantsClampsPageAndSizeAndDelegatesFilter() {
    Instant from = Instant.parse("2026-01-01T00:00:00Z");
    Instant to = Instant.parse("2026-01-02T00:00:00Z");
    ListTenantsQuery query = new ListTenantsQuery(-2, 500, TenantStatus.ACTIVE, "acme", from, to);
    Page<Tenant> expected = new PageImpl<>(List.of());
    when(repository.findAll(any(), any())).thenReturn(expected);

    Page<Tenant> result = service.listTenants(query);

    assertSame(expected, result);

    ArgumentCaptor<TenantFilter> filterCaptor = ArgumentCaptor.forClass(TenantFilter.class);
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(repository).findAll(filterCaptor.capture(), pageableCaptor.capture());

    TenantFilter filter = filterCaptor.getValue();
    Pageable pageable = pageableCaptor.getValue();
    assertEquals(TenantStatus.ACTIVE, filter.status());
    assertEquals("acme", filter.nameContains());
    assertEquals(from, filter.createdFrom());
    assertEquals(to, filter.createdTo());
    assertEquals(0, pageable.getPageNumber());
    assertEquals(100, pageable.getPageSize());
    assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
  }

  @Test
  void listTenantsRejectsInvalidDateRange() {
    ListTenantsQuery query = new ListTenantsQuery(
        0,
        10,
        null,
        null,
        Instant.parse("2026-01-02T00:00:00Z"),
        Instant.parse("2026-01-01T00:00:00Z"));

    BadRequestException ex = assertThrows(BadRequestException.class, () -> service.listTenants(query));

    assertEquals("createdFrom must be before or equal to createdTo", ex.getMessage());
  }
}
