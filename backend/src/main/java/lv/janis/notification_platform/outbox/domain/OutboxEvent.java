package lv.janis.notification_platform.outbox.domain;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lv.janis.notification_platform.tenant.domain.Tenant;

@Entity
@Table(name = "outbox_event", uniqueConstraints = @UniqueConstraint(name = "uk_outbox_tenant_aggregate_event", columnNames = {
    "tenant_id", "aggregate_type", "aggregate_id", "event_type" }), indexes = {
        @Index(name = "idx_outbox_status_available_at", columnList = "status,available_at"),
        @Index(name = "idx_outbox_tenant_status", columnList = "tenant_id,status"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type,aggregate_id")
    })
@EntityListeners(AuditingEntityListener.class)
public class OutboxEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Column(name = "tenant_id", nullable = false, insertable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "aggregate_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private OutboxEventAggregateType aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 100)
  private OutboxEventType eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private JsonNode payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private OutboxStatus status;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "available_at", nullable = false)
  private Instant availableAt;

  @Column(name = "last_attempt_at")
  private Instant lastAttemptAt;

  @Column(name = "published_at")
  private Instant publishedAt;

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

  protected OutboxEvent() {
  }

  public OutboxEvent(
      Tenant tenant,
      OutboxEventAggregateType aggregateType,
      UUID aggregateId,
      OutboxEventType eventType,
      JsonNode payload,
      Instant availableAt) {
    this.tenant = Objects.requireNonNull(tenant, "tenant must not be null");
    this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType must not be null");
    this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
    this.payload = Objects.requireNonNull(payload, "payload must not be null");
    this.availableAt = availableAt;
    this.status = OutboxStatus.PENDING;
    this.attemptCount = 0;
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

  public OutboxEventAggregateType getAggregateType() {
    return aggregateType;
  }

  public UUID getAggregateId() {
    return aggregateId;
  }

  public OutboxEventType getEventType() {
    return eventType;
  }

  public JsonNode getPayload() {
    return payload;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public Instant getAvailableAt() {
    return availableAt;
  }

  public Instant getLastAttemptAt() {
    return lastAttemptAt;
  }

  public Instant getPublishedAt() {
    return publishedAt;
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
    this.status = OutboxStatus.IN_PROGRESS;
    this.lastAttemptAt = Objects.requireNonNull(attemptedAt, "attemptedAt must not be null");
    this.attemptCount++;
  }

  public void markPublished(Instant publishedAt) {
    this.status = OutboxStatus.PUBLISHED;
    this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");
    this.lastError = null;
  }

  public void markFailed(String lastError, Instant nextAvailableAt) {
    this.status = OutboxStatus.FAILED;
    this.lastError = normalizeOptional(lastError);
    this.availableAt = Objects.requireNonNull(nextAvailableAt, "nextAvailableAt must not be null");
  }

  public void reschedule(Instant nextAvailableAt, String lastError) {
    this.status = OutboxStatus.PENDING;
    this.availableAt = Objects.requireNonNull(nextAvailableAt, "nextAvailableAt must not be null");
    this.lastError = normalizeOptional(lastError);
  }

  private static String normalizeOptional(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
