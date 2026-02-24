package lv.janis.notification_platform.delivery.domain;

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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lv.janis.notification_platform.tenant.domain.Tenant;

@Entity
@Table(
    name = "endpoint",
    indexes = {
        @Index(name = "idx_endpoint_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_endpoint_status", columnList = "status")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Endpoint {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private EndpointType type;

  @Column(length = 255)
  private String url;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private EndpointStatus status;

  @Version
  @Column(nullable = false)
  private Long version;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Endpoint() {
  }

  public Endpoint(Tenant tenant, EndpointType type, String url) {
    this.tenant = Objects.requireNonNull(tenant, "tenant must not be null");
    this.type = Objects.requireNonNull(type, "type must not be null");
    this.url = url;
    this.status = EndpointStatus.ACTIVE;
  }

  public UUID getId() {
    return id;
  }

  public Tenant getTenant() {
    return tenant;
  }

  public EndpointType getType() {
    return type;
  }

  public String getUrl() {
    return url;
  }

  public EndpointStatus getStatus() {
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

  public void setUrl(String url) {
    this.url = url;
  }

  public void activate() {
    this.status = EndpointStatus.ACTIVE;
  }

  public void deactivate() {
    this.status = EndpointStatus.INACTIVE;
  }

  public void delete() {
    this.status = EndpointStatus.DELETED;
  }
}
