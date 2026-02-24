package lv.janis.notification_platform.tenant.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
    name = "tenant",
    uniqueConstraints = @UniqueConstraint(name = "uk_tenant_slug", columnNames = "slug"),
    indexes = @Index(name = "idx_tenant_status", columnList = "status")
)
@EntityListeners(AuditingEntityListener.class)
public class Tenant {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, length = 64)
  private String slug;

  @Column(nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TenantStatus status;

  @Version
  @Column(nullable = false)
  private Long version;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Tenant() {
  }

  public Tenant(String slug, String name, TenantStatus status) {
    this.slug = Objects.requireNonNull(slug, "slug must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");
  }

  public UUID getId() {
    return id;
  }

  public String getSlug() {
    return slug;
  }

  public String getName() {
    return name;
  }

  public TenantStatus getStatus() {
    return status;
  }

  public Long getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void rename(String name) {
    this.name = Objects.requireNonNull(name, "name must not be null");
  }

  public void changeStatus(TenantStatus status) {
    this.status = Objects.requireNonNull(status, "status must not be null");
  }
}
