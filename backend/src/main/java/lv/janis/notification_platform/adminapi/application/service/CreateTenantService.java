package lv.janis.notification_platform.adminapi.application.service;

import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lv.janis.notification_platform.adminapi.application.exception.BadRequestException;
import lv.janis.notification_platform.adminapi.application.exception.ConflictException;
import lv.janis.notification_platform.adminapi.application.port.in.CreateTenantCommand;
import lv.janis.notification_platform.adminapi.application.port.in.CreateTenantUseCase;
import lv.janis.notification_platform.tenant.application.port.out.TenantRepositoryPort;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

@Service
public class CreateTenantService implements CreateTenantUseCase {
  private final TenantRepositoryPort tenantRepositoryPort;

  public CreateTenantService(TenantRepositoryPort tenantRepositoryPort) {
    this.tenantRepositoryPort = tenantRepositoryPort;
  }

  @Override
  @Transactional
  public Tenant createTenant(CreateTenantCommand command) {
    String slug = normalizeSlug(command.slug());
    String name = normalizeName(command.name());
    TenantStatus status = command.status() == null ? TenantStatus.ACTIVE : command.status();

    if (tenantRepositoryPort.existsBySlug(slug)) {
      throw ConflictException.of("Tenant slug already exists: " + slug);
    }

    Tenant tenant = new Tenant(slug, name, status);
    return tenantRepositoryPort.save(tenant);
  }

  private static String normalizeSlug(String slug) {
    String normalized = Objects.requireNonNull(slug, "slug must not be null").trim().toLowerCase(Locale.ROOT);
    if (normalized.isEmpty()) {
      throw new BadRequestException("slug must not be blank");
    }
    return normalized;
  }

  private static String normalizeName(String name) {
    String normalized = Objects.requireNonNull(name, "name must not be null").trim();
    if (normalized.isEmpty()) {
      throw new BadRequestException("name must not be blank");
    }
    return normalized;
  }
}
