package lv.janis.notification_platform.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lv.janis.notification_platform.tenant.domain.Tenant;

@Entity
@Table(
    name = "api_key",
    uniqueConstraints = @UniqueConstraint(name = "uk_api_key_key_hash", columnNames = "key_hash"),
    indexes = {
        @Index(name = "idx_api_key_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_api_key_status", columnList = "status")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class ApiKey {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Column(name = "tenant_id", nullable = false, insertable = false, updatable = false)
  private UUID tenantId;

  @Column(nullable = false, updatable = false, length = 64)
  private String keyPrefix;

  @Column(nullable = false, updatable = false, length = 128)
  private String keyHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ApiKeyStatus status;

  @Version
  @Column(nullable = false)
  private Long version;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  protected ApiKey() {
  }

  public ApiKey(Tenant tenant, String keyPrefix, String keyHash) {
    this.tenant = Objects.requireNonNull(tenant, "tenant must not be null");
    this.keyPrefix = Objects.requireNonNull(keyPrefix, "keyPrefix must not be null");
    this.keyHash = Objects.requireNonNull(keyHash, "keyHash must not be null");
    this.status = ApiKeyStatus.ACTIVE;
  }

  public UUID getId() {
    return id;
  }

  public Tenant getTenant() {
    return tenant;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getKeyPrefix() {
    return keyPrefix;
  }

  public String getKeyHash() {
    return keyHash;
  }

  public ApiKeyStatus getStatus() {
    return status;
  }

  public Long getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public void markUsedAt(Instant usedAt) {
    this.lastUsedAt = Objects.requireNonNull(usedAt, "usedAt must not be null");
  }

  public void revoke(Instant revokedAt) {
    this.status = ApiKeyStatus.REVOKED;
    this.revokedAt = Objects.requireNonNull(revokedAt, "revokedAt must not be null");
  }
}
