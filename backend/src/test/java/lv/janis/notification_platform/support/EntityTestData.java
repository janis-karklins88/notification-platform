package lv.janis.notification_platform.support;

import java.time.Instant;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import lv.janis.notification_platform.auth.domain.ApiKey;
import lv.janis.notification_platform.delivery.domain.Delivery;
import lv.janis.notification_platform.delivery.domain.DeliveryStatus;
import lv.janis.notification_platform.delivery.domain.EmailTemplate;
import lv.janis.notification_platform.delivery.domain.Endpoint;
import lv.janis.notification_platform.delivery.domain.EndpointStatus;
import lv.janis.notification_platform.delivery.domain.EndpointType;
import lv.janis.notification_platform.ingest.domain.Event;
import lv.janis.notification_platform.ingest.domain.EventStatus;
import lv.janis.notification_platform.outbox.domain.OutboxEvent;
import lv.janis.notification_platform.outbox.domain.OutboxEventAggregateType;
import lv.janis.notification_platform.outbox.domain.OutboxEventType;
import lv.janis.notification_platform.outbox.domain.OutboxStatus;
import lv.janis.notification_platform.routing.domain.Subscription;
import lv.janis.notification_platform.routing.domain.SubscriptionStatus;
import lv.janis.notification_platform.tenant.domain.Tenant;
import lv.janis.notification_platform.tenant.domain.TenantStatus;

public final class EntityTestData {
  private EntityTestData() {
  }

  public static Tenant tenant(UUID id) {
    return tenant(id, TenantStatus.ACTIVE);
  }

  public static Tenant tenant(UUID id, TenantStatus status) {
    Tenant tenant = new Tenant("tenant-" + id.toString().substring(0, 8), "Tenant " + id.toString().substring(0, 8), status);
    ReflectionTestUtils.setField(tenant, "id", id);
    return tenant;
  }

  public static Endpoint endpoint(UUID id, Tenant tenant, EndpointType type) {
    return endpoint(id, tenant, type, JsonNodeFactory.instance.objectNode(), EndpointStatus.ACTIVE);
  }

  public static Endpoint endpoint(UUID id, Tenant tenant, EndpointType type, JsonNode config, EndpointStatus status) {
    Endpoint endpoint = new Endpoint(tenant, type, config);
    ReflectionTestUtils.setField(endpoint, "id", id);
    ReflectionTestUtils.setField(endpoint, "tenantId", tenant.getId());
    ReflectionTestUtils.setField(endpoint, "status", status);
    return endpoint;
  }

  public static EmailTemplate emailTemplate(
      UUID id,
      Tenant tenant,
      String name,
      String subject,
      String body,
      boolean html,
      String description,
      boolean active) {
    EmailTemplate template = new EmailTemplate(tenant, name, subject, body, html, description);
    ReflectionTestUtils.setField(template, "id", id);
    ReflectionTestUtils.setField(template, "tenantId", tenant.getId());
    ReflectionTestUtils.setField(template, "isActive", active);
    return template;
  }

  public static Event event(UUID id, Tenant tenant, String eventType) {
    return event(id, tenant, eventType, JsonNodeFactory.instance.objectNode(), EventStatus.RECEIVED);
  }

  public static Event event(UUID id, Tenant tenant, String eventType, JsonNode payload, EventStatus status) {
    Event event = new Event(tenant, eventType, null, payload, "source", "trace");
    ReflectionTestUtils.setField(event, "id", id);
    ReflectionTestUtils.setField(event, "tenantId", tenant.getId());
    ReflectionTestUtils.setField(event, "status", status);
    return event;
  }

  public static Subscription subscription(UUID id, Tenant tenant, String eventType, Endpoint endpoint) {
    return subscription(id, tenant, eventType, endpoint, SubscriptionStatus.ACTIVE);
  }

  public static Subscription subscription(UUID id, Tenant tenant, String eventType, Endpoint endpoint, SubscriptionStatus status) {
    Subscription subscription = new Subscription(tenant, eventType, endpoint);
    ReflectionTestUtils.setField(subscription, "id", id);
    ReflectionTestUtils.setField(subscription, "tenantId", tenant.getId());
    ReflectionTestUtils.setField(subscription, "status", status);
    return subscription;
  }

  public static Delivery delivery(UUID id, Tenant tenant, Event event, Subscription subscription, Endpoint endpoint) {
    return delivery(id, tenant, event, subscription, endpoint, DeliveryStatus.PENDING);
  }

  public static Delivery delivery(
      UUID id,
      Tenant tenant,
      Event event,
      Subscription subscription,
      Endpoint endpoint,
      DeliveryStatus status) {
    Delivery delivery = new Delivery(tenant, event, subscription, endpoint);
    ReflectionTestUtils.setField(delivery, "id", id);
    ReflectionTestUtils.setField(delivery, "tenantId", tenant.getId());
    ReflectionTestUtils.setField(delivery, "eventId", event.getId());
    ReflectionTestUtils.setField(delivery, "subscriptionId", subscription.getId());
    ReflectionTestUtils.setField(delivery, "endpointId", endpoint.getId());
    ReflectionTestUtils.setField(delivery, "status", status);
    return delivery;
  }

  public static OutboxEvent outboxEvent(
      UUID id,
      Tenant tenant,
      OutboxEventAggregateType aggregateType,
      UUID aggregateId,
      OutboxEventType eventType,
      JsonNode payload,
      Instant availableAt) {
    return outboxEvent(id, tenant, aggregateType, aggregateId, eventType, payload, availableAt, OutboxStatus.PENDING);
  }

  public static OutboxEvent outboxEvent(
      UUID id,
      Tenant tenant,
      OutboxEventAggregateType aggregateType,
      UUID aggregateId,
      OutboxEventType eventType,
      JsonNode payload,
      Instant availableAt,
      OutboxStatus status) {
    OutboxEvent outboxEvent = new OutboxEvent(tenant, aggregateType, aggregateId, eventType, payload, availableAt);
    ReflectionTestUtils.setField(outboxEvent, "id", id);
    ReflectionTestUtils.setField(outboxEvent, "tenantId", tenant.getId());
    ReflectionTestUtils.setField(outboxEvent, "status", status);
    return outboxEvent;
  }

  public static ApiKey apiKey(UUID id, Tenant tenant, String prefix, String hash, Instant createdAt) {
    ApiKey apiKey = new ApiKey(tenant, prefix, hash);
    ReflectionTestUtils.setField(apiKey, "id", id);
    ReflectionTestUtils.setField(apiKey, "tenantId", tenant.getId());
    ReflectionTestUtils.setField(apiKey, "createdAt", createdAt);
    return apiKey;
  }
}
