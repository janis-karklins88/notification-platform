package lv.janis.notification_platform.outbox.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import lv.janis.notification_platform.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

  @Test
  void constructorSetsDefaults() {
    UUID tenantId = UUID.randomUUID();
    Tenant tenant = mock(Tenant.class);
    when(tenant.getId()).thenReturn(tenantId);

    JsonNode payload = new TextNode("payload");
    Instant availableAt = Instant.parse("2026-03-01T00:00:00Z");
    OutboxEvent outboxEvent = new OutboxEvent(
        tenant,
        OutboxEventAggregateType.EVENT,
        UUID.randomUUID(),
        OutboxEventType.EVENT_ACCEPTED,
        payload,
        availableAt);

    assertSame(tenant, outboxEvent.getTenant());
    assertEquals(OutboxStatus.PENDING, outboxEvent.getStatus());
    assertEquals(0, outboxEvent.getAttemptCount());
    assertEquals(availableAt, outboxEvent.getAvailableAt());
    assertSame(payload, outboxEvent.getPayload());
  }

  @Test
  void markInProgressIncrementsAttemptsAndSetsLastAttempt() {
    OutboxEvent outboxEvent = eventWithTenantAndPayload();

    Instant firstAttempt = Instant.parse("2026-03-01T00:00:00Z");
    outboxEvent.markInProgress(firstAttempt);

    assertEquals(OutboxStatus.IN_PROGRESS, outboxEvent.getStatus());
    assertEquals(1, outboxEvent.getAttemptCount());
    assertEquals(firstAttempt, outboxEvent.getLastAttemptAt());
  }

  @Test
  void markPublishedClearsErrorAndSetsPublishedAt() {
    OutboxEvent outboxEvent = eventWithTenantAndPayload();

    outboxEvent.markFailed("transient", Instant.parse("2026-03-01T00:00:00Z"));
    outboxEvent.markPublished(Instant.parse("2026-03-01T00:01:00Z"));

    assertEquals(OutboxStatus.PUBLISHED, outboxEvent.getStatus());
    assertNull(outboxEvent.getLastError());
    assertEquals(Instant.parse("2026-03-01T00:01:00Z"), outboxEvent.getPublishedAt());
  }

  @Test
  void markFailedNormalizesErrorAndSetsAvailableAt() {
    OutboxEvent outboxEvent = eventWithTenantAndPayload();
    Instant nextAvailableAt = Instant.parse("2026-03-01T00:10:00Z");

    outboxEvent.markFailed("   temporary failure   ", nextAvailableAt);

    assertEquals(OutboxStatus.FAILED, outboxEvent.getStatus());
    assertEquals("temporary failure", outboxEvent.getLastError());
    assertEquals(nextAvailableAt, outboxEvent.getAvailableAt());
  }

  @Test
  void markFailedClearsBlankError() {
    OutboxEvent outboxEvent = eventWithTenantAndPayload();

    outboxEvent.markFailed("   ", Instant.parse("2026-03-01T00:10:00Z"));

    assertNull(outboxEvent.getLastError());
  }

  @Test
  void rescheduleSetsPendingAndNormalizesError() {
    OutboxEvent outboxEvent = eventWithTenantAndPayload();

    Instant rescheduleAt = Instant.parse("2026-03-01T00:20:00Z");
    outboxEvent.reschedule(rescheduleAt, "  retry ");

    assertEquals(OutboxStatus.PENDING, outboxEvent.getStatus());
    assertEquals("retry", outboxEvent.getLastError());
    assertEquals(rescheduleAt, outboxEvent.getAvailableAt());
  }

  @Test
  void markInProgressRequiresAttemptTime() {
    OutboxEvent outboxEvent = eventWithTenantAndPayload();

    assertThrows(NullPointerException.class, () -> outboxEvent.markInProgress(null));
  }

  @Test
  void markPublishedRequiresPublishedAt() {
    OutboxEvent outboxEvent = eventWithTenantAndPayload();

    assertThrows(NullPointerException.class, () -> outboxEvent.markPublished(null));
  }

  @Test
  void markFailedRequiresNextAvailableTime() {
    OutboxEvent outboxEvent = eventWithTenantAndPayload();

    assertThrows(NullPointerException.class, () -> outboxEvent.markFailed("temp", null));
  }

  @Test
  void rescheduleRequiresNextAvailableTime() {
    OutboxEvent outboxEvent = eventWithTenantAndPayload();

    assertThrows(NullPointerException.class, () -> outboxEvent.reschedule(null, "retry"));
  }

  @Test
  void markInProgressIncrementsAttemptCountPerCall() {
    OutboxEvent outboxEvent = eventWithTenantAndPayload();
    outboxEvent.markInProgress(Instant.parse("2026-03-01T00:00:00Z"));
    outboxEvent.markInProgress(Instant.parse("2026-03-01T00:00:10Z"));

    assertEquals(2, outboxEvent.getAttemptCount());
    assertEquals(OutboxStatus.IN_PROGRESS, outboxEvent.getStatus());
    assertEquals(Instant.parse("2026-03-01T00:00:10Z"), outboxEvent.getLastAttemptAt());
  }

  @Test
  void transitionFromFailedBackToPublishedClearsError() {
    OutboxEvent outboxEvent = eventWithTenantAndPayload();
    outboxEvent.markFailed("error details", Instant.parse("2026-03-01T00:10:00Z"));
    outboxEvent.markPublished(Instant.parse("2026-03-01T00:10:10Z"));

    assertEquals(OutboxStatus.PUBLISHED, outboxEvent.getStatus());
    assertNull(outboxEvent.getLastError());
    assertEquals(Instant.parse("2026-03-01T00:10:10Z"), outboxEvent.getPublishedAt());
  }

  @Test
  void constructorRejectsNullInputs() {
    Tenant tenant = mock(Tenant.class);
    when(tenant.getId()).thenReturn(UUID.randomUUID());
    JsonNode payload = new TextNode("payload");

    assertThrows(NullPointerException.class, () -> new OutboxEvent(
        null,
        OutboxEventAggregateType.EVENT,
        UUID.randomUUID(),
        OutboxEventType.EVENT_ACCEPTED,
        payload,
        Instant.now()));
    assertThrows(NullPointerException.class, () -> new OutboxEvent(
        tenant,
        null,
        UUID.randomUUID(),
        OutboxEventType.EVENT_ACCEPTED,
        payload,
        Instant.now()));
    assertThrows(NullPointerException.class, () -> new OutboxEvent(
        tenant,
        OutboxEventAggregateType.EVENT,
        null,
        OutboxEventType.EVENT_ACCEPTED,
        payload,
        Instant.now()));
    assertThrows(NullPointerException.class, () -> new OutboxEvent(
        tenant,
        OutboxEventAggregateType.EVENT,
        UUID.randomUUID(),
        null,
        payload,
        Instant.now()));
    assertThrows(NullPointerException.class, () -> new OutboxEvent(
        tenant,
        OutboxEventAggregateType.EVENT,
        UUID.randomUUID(),
        OutboxEventType.EVENT_ACCEPTED,
        null,
        Instant.now()));
  }

  private static OutboxEvent eventWithTenantAndPayload() {
    Tenant tenant = mock(Tenant.class);
    when(tenant.getId()).thenReturn(UUID.randomUUID());
    return new OutboxEvent(
        tenant,
        OutboxEventAggregateType.EVENT,
        UUID.randomUUID(),
        OutboxEventType.EVENT_ACCEPTED,
        new TextNode("payload"),
        Instant.now());
  }
}
