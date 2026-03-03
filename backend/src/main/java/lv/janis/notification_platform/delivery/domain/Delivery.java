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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.tenant.domain.Tenant;

@Entity
@Table(
    name = "delivery",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_delivery_tenant_event_subscription",
        columnNames = {"tenant_id", "event_id", "subscription_id"}),
    indexes = {
        @Index(name = "idx_delivery_tenant_status", columnList = "tenant_id,status"),
        @Index(name = "idx_delivery_status_next_attempt_at", columnList = "status,next_attempt_at"),
        @Index(name = "idx_delivery_event_id", columnList = "event_id")
    })
@EntityListeners(AuditingEntityListener.class)
public class Delivery {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Column(name = "tenant_id", nullable = false, insertable = false, updatable = false)
  private UUID tenantId;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  private Event event;

  @Column(name = "event_id", nullable = false, insertable = false, updatable = false)
  private UUID eventId;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "subscription_id", nullable = false)
  private Subscription subscription;

  @Column(name = "subscription_id", nullable = false, insertable = false, updatable = false)
  private UUID subscriptionId;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "endpoint_id", nullable = false)
  private Endpoint endpoint;

  @Column(name = "endpoint_id", nullable = false, insertable = false, updatable = false)
  private UUID endpointId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private DeliveryStatus status;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt;

  @Column(name = "last_attempt_at")
  private Instant lastAttemptAt;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  @Column(name = "failed_at")
  private Instant failedAt;

  @Column(name = "last_error", length = 4000)
  private String lastError;

  @Version
  @Column(nullable = false)
  private Long version;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Delivery() {
  }

  public Delivery(Tenant tenant, Event event, Subscription subscription, Endpoint endpoint, Instant nextAttemptAt) {
    this.tenant = Objects.requireNonNull(tenant, "tenant must not be null");
    this.event = Objects.requireNonNull(event, "event must not be null");
    this.subscription = Objects.requireNonNull(subscription, "subscription must not be null");
    this.endpoint = Objects.requireNonNull(endpoint, "endpoint must not be null");
    validateSameTenant(this.tenant, this.event, this.subscription, this.endpoint);
    this.status = DeliveryStatus.PENDING;
    this.attemptCount = 0;
    this.nextAttemptAt = nextAttemptAt;
  }

  public Delivery(Tenant tenant, Event event, Subscription subscription, Endpoint endpoint) {
    this(tenant, event, subscription, endpoint, null);
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

  public Event getEvent() {
    return event;
  }

  public UUID getEventId() {
    return eventId;
  }

  public Subscription getSubscription() {
    return subscription;
  }

  public UUID getSubscriptionId() {
    return subscriptionId;
  }

  public Endpoint getEndpoint() {
    return endpoint;
  }

  public UUID getEndpointId() {
    return endpointId;
  }

  public DeliveryStatus getStatus() {
    return status;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public Instant getLastAttemptAt() {
    return lastAttemptAt;
  }

  public Instant getDeliveredAt() {
    return deliveredAt;
  }

  public Instant getFailedAt() {
    return failedAt;
  }

  public String getLastError() {
    return lastError;
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

  public void markInProgress(Instant attemptedAt) {
    this.status = DeliveryStatus.IN_PROGRESS;
    this.lastAttemptAt = Objects.requireNonNull(attemptedAt, "attemptedAt must not be null");
    this.attemptCount++;
    this.nextAttemptAt = null;
  }

  public void scheduleRetry(Instant nextAttemptAt, String lastError) {
    this.status = DeliveryStatus.RETRY_SCHEDULED;
    this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null");
    this.lastError = normalizeOptional(lastError);
  }

  public void markDelivered(Instant deliveredAt) {
    this.status = DeliveryStatus.DELIVERED;
    this.deliveredAt = Objects.requireNonNull(deliveredAt, "deliveredAt must not be null");
    this.failedAt = null;
    this.nextAttemptAt = null;
    this.lastError = null;
  }

  public void markFailed(Instant failedAt, String lastError) {
    this.status = DeliveryStatus.FAILED;
    this.failedAt = Objects.requireNonNull(failedAt, "failedAt must not be null");
    this.nextAttemptAt = null;
    this.lastError = normalizeOptional(lastError);
  }

  private static String normalizeOptional(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private static void validateSameTenant(Tenant tenant, Event event, Subscription subscription, Endpoint endpoint) {
    Objects.requireNonNull(tenant, "tenant must not be null");
    Objects.requireNonNull(event, "event must not be null");
    Objects.requireNonNull(subscription, "subscription must not be null");
    Objects.requireNonNull(endpoint, "endpoint must not be null");

    UUID tenantId = Objects.requireNonNull(tenant.getId(), "tenant.id must not be null");
    UUID eventTenantId = Objects.requireNonNull(event.getTenant().getId(), "event.tenant.id must not be null");
    UUID subscriptionTenantId = Objects.requireNonNull(subscription.getTenant().getId(),
        "subscription.tenant.id must not be null");
    UUID endpointTenantId = Objects.requireNonNull(endpoint.getTenant().getId(), "endpoint.tenant.id must not be null");

    if (!tenantId.equals(eventTenantId) || !tenantId.equals(subscriptionTenantId) || !tenantId.equals(endpointTenantId)) {
      throw new IllegalArgumentException("event, subscription and endpoint must belong to the same tenant");
    }
  }
}
