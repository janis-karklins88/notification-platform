package lv.janis.notification_platform.ingest.domain;

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
@Table(
    name = "event",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_event_tenant_idempotency_key",
        columnNames = {"tenant_id", "idempotency_key"}
    ),
    indexes = {
        @Index(name = "idx_event_tenant_received_at", columnList = "tenant_id,received_at"),
        @Index(name = "idx_event_tenant_status", columnList = "tenant_id,status"),
        @Index(name = "idx_event_tenant_event_type", columnList = "tenant_id,event_type")
    }
)
@EntityListeners(AuditingEntityListener.class)
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Column(name = "event_type", nullable = false, length = 150)
  private String eventType;

  @Column(name = "idempotency_key", length = 128)
  private String idempotencyKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private JsonNode payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private EventStatus status;

  @Version
  @Column(nullable = false)
  private Long version;

  @CreatedDate
  @Column(name = "received_at", nullable = false, updatable = false)
  private Instant receivedAt;

  @Column(length = 100)
  private String source;

  @Column(name = "trace_id", length = 100)
  private String traceId;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Event() {
  }

  public Event(
      Tenant tenant,
      String eventType,
      String idempotencyKey,
      JsonNode payload,
      String source,
      String traceId
  ) {
    this.tenant = Objects.requireNonNull(tenant, "tenant must not be null");
    this.eventType = normalizeEventType(eventType);
    this.idempotencyKey = normalizeIdempotencyKey(idempotencyKey);
    this.payload = Objects.requireNonNull(payload, "payload must not be null");
    this.source = source;
    this.traceId = traceId;
    this.status = EventStatus.RECEIVED;
  }

  public UUID getId() {
    return id;
  }

  public Tenant getTenant() {
    return tenant;
  }

  public String getEventType() {
    return eventType;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public JsonNode getPayload() {
    return payload;
  }

  public EventStatus getStatus() {
    return status;
  }

  public Long getVersion() {
    return version;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public String getSource() {
    return source;
  }

  public String getTraceId() {
    return traceId;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void markRouted() {
    this.status = EventStatus.ROUTED;
  }

  public void markFailed() {
    this.status = EventStatus.FAILED;
  }

  private static String normalizeEventType(String eventType) {
    String normalized = Objects.requireNonNull(eventType, "eventType must not be null").trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("eventType must not be blank");
    }
    return normalized;
  }

  private static String normalizeIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null) {
      return null;
    }
    String normalized = idempotencyKey.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
