package lv.janis.notification_platform.routing.domain;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.tenant.domain.Tenant;

@Entity
@Table(name = "subscription", uniqueConstraints = @UniqueConstraint(name = "uk_subscription_tenant_event_endpoint", columnNames = {
    "tenant_id", "event_type", "endpoint_id" }), indexes = {
        @Index(name = "idx_subscription_tenant_event_status", columnList = "tenant_id,event_type,status"),
        @Index(name = "idx_subscription_endpoint_id", columnList = "endpoint_id"),
        @Index(name = "idx_subscription_tenant_status", columnList = "tenant_id,status")
    })
@EntityListeners(AuditingEntityListener.class)
public class Subscription {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Column(name = "tenant_id", nullable = false, insertable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "event_type", nullable = false, length = 150)
  private String eventType;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "endpoint_id", nullable = false)
  private Endpoint endpoint;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private SubscriptionStatus status;

  @Version
  @Column(nullable = false)
  private Long version;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Subscription() {
  }

  public Subscription(Tenant tenant, String eventType, Endpoint endpoint) {
    this.tenant = Objects.requireNonNull(tenant, "tenant must not be null");
    this.eventType = normalizeEventType(eventType);
    this.endpoint = Objects.requireNonNull(endpoint, "endpoint must not be null");
    validateSameTenant(this.tenant, this.endpoint);
    this.status = SubscriptionStatus.ACTIVE;
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

  public String getEventType() {
    return eventType;
  }

  public Endpoint getEndpoint() {
    return endpoint;
  }

  public SubscriptionStatus getStatus() {
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

  public void pause() {
    this.status = SubscriptionStatus.PAUSED;
  }

  public void activate() {
    this.status = SubscriptionStatus.ACTIVE;
  }

  public void delete() {
    this.status = SubscriptionStatus.DELETED;
  }

  private static String normalizeEventType(String eventType) {
    String normalized = Objects.requireNonNull(eventType, "eventType must not be null").trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("eventType must not be blank");
    }
    return normalized;
  }

  private static void validateSameTenant(Tenant tenant, Endpoint endpoint) {
    Objects.requireNonNull(tenant, "tenant must not be null");
    Objects.requireNonNull(endpoint, "endpoint must not be null");

    var tenantId = Objects.requireNonNull(tenant.getId(), "tenant.id must not be null");
    var endpointTenant = Objects.requireNonNull(endpoint.getTenant(), "endpoint.tenant must not be null");
    var endpointTenantId = Objects.requireNonNull(endpointTenant.getId(), "endpoint.tenant.id must not be null");

    if (!tenantId.equals(endpointTenantId)) {
      throw new IllegalArgumentException("endpoint must belong to the same tenant as subscription");
    }
  }
}
