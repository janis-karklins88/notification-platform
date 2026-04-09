package lv.janis.notification_platform.delivery.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lv.janis.notification_platform.tenant.domain.Tenant;

@Entity
@Table(name = "email_templates", uniqueConstraints = @UniqueConstraint(name = "uk_template_name_tenant", columnNames = {
    "tenant_id", "name" }))
@EntityListeners(AuditingEntityListener.class)
public class EmailTemplate {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(nullable = false, length = 100)
  private String name;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Column(name = "tenant_id", nullable = false, insertable = false, updatable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 255)
  private String subject;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String body;

  @Column(nullable = false)
  private boolean isHtml;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(length = 500)
  private String description;

  @Column(nullable = false)
  private boolean isActive = true;

  public EmailTemplate(Tenant tenant, String name, String subject, String body, boolean isHtml, String description) {
    this.tenant = Objects.requireNonNull(tenant, "tenant must not be null");
    this.name = normalizeRequired(name, "name");
    this.subject = normalizeRequired(subject, "subject");
    this.body = normalizeRequired(body, "body");
    this.isHtml = isHtml;
    this.description = normalizeOptional(description);
  }

  protected EmailTemplate() {

  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Tenant getTenant() {
    return tenant;
  }

  public UUID getTenantId() {
    if (tenantId != null) {
      return tenantId;
    }
    return tenant != null ? tenant.getId() : null;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public boolean isHtml() {
    return isHtml;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getDescription() {
    return description;
  }

  public void setTenant(Tenant tenant) {
    this.tenant = Objects.requireNonNull(tenant, "tenant must not be null");
  }

  public void setName(String name) {
    this.name = normalizeRequired(name, "name");
  }

  public void setSubject(String subject) {
    this.subject = normalizeRequired(subject, "subject");
  }

  public void setBody(String body) {
    this.body = normalizeRequired(body, "body");
  }

  public void setHtml(boolean html) {
    isHtml = html;
  }

  public void setDescription(String description) {
    this.description = normalizeOptional(description);
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public void edit(String name, String subject, String body, boolean html, String description) {
    this.name = normalizeRequired(name, "name");
    this.subject = normalizeRequired(subject, "subject");
    this.body = normalizeRequired(body, "body");
    this.isHtml = html;
    this.description = normalizeOptional(description);
  }

  public void delete() {
    this.isActive = false;
  }

  private static String normalizeRequired(String value, String fieldName) {
    String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }

  private static String normalizeOptional(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

}
