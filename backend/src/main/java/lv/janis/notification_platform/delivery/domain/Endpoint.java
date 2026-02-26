package lv.janis.notification_platform.delivery.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.databind.JsonNode;

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
@Table(name = "endpoint", indexes = {
    @Index(name = "idx_endpoint_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_endpoint_status", columnList = "status")
})
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

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  private JsonNode config;

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

  public Endpoint(Tenant tenant, EndpointType type, JsonNode config) {
    this.tenant = Objects.requireNonNull(tenant, "tenant must not be null");
    this.type = Objects.requireNonNull(type, "type must not be null");
    this.config = Objects.requireNonNull(config, "config must not be null");
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

  public JsonNode getConfig() {
    return config;
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

  public void setConfig(JsonNode config) {
    this.config = Objects.requireNonNull(config, "config must not be null");
  }

  public void activate() {
    this.status = EndpointStatus.ACTIVE;
  }

  public void deactivate() {
    this.status = EndpointStatus.INACTIVE;
  }

  public void delete() {
    this.status = EndpointStatus.DISABLED;
  }
}
