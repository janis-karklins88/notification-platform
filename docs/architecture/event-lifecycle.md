# Event Lifecycle

This document describes what happens to an event from ingest to delivery.

## 1. Ingest

An external client sends `POST /ingest` with:

- `X-API-Key`
- `eventType`
- `payload`
- optional `idempotencyKey`
- optional `source`
- optional `traceId`

The ingest flow:

1. Authenticates the tenant by API key
2. Checks tenant-scoped idempotency
3. Stores the event
4. Creates an outbox event of type `EVENT_ACCEPTED`

If the same tenant sends the same `idempotencyKey` again:

- the event is treated as already accepted
- the existing event is returned
- a new routing flow is not started

## 2. Event Accepted Outbox Publication

The accepted event is not routed immediately through a direct synchronous call.

Instead:

- an outbox row is stored in the same persistence flow
- the outbox dispatcher publishes it to RabbitMQ
- the routing listener consumes it asynchronously

This creates a more reliable handoff between persistence and messaging.

## 3. Routing

When the `EVENT_ACCEPTED` message is consumed:

1. The stored event is loaded
2. Active subscriptions are searched by:
   - tenant
   - event type
3. Matching subscriptions create delivery records
4. Each created delivery gets its own outbox event
5. The original event is marked as routed

Relevant queues and routing keys:

- queue `np.routing.event.accepted`
- routing key `event.accepted`

## 4. No Matching Active Subscription

If no active subscription matches:

- no delivery record is created
- the event is still marked as routed

This is why an accepted event can exist without anything appearing in the deliveries view.

Common case:

- subscription is paused
- event arrives while paused
- routing sees no active match
- no delivery is created

## 5. Subscription Reactivation

Reactivating a subscription does not retroactively process already routed events.

That means:

- pausing a subscription suppresses future delivery creation while paused
- reactivating it only affects newly ingested events

To process the missed event, it must be explicitly resent as a new event.

This is an intentional and safer behavior because it avoids stale or unexpected backfill when a subscription is reactivated.

## 6. Delivery Creation

For each matching subscription, the platform creates:

- a `Delivery` row
- an outbox event for channel-specific delivery handling

Current delivery outbox event types:

- `DELIVERY_CREATED_EMAIL`
- `DELIVERY_CREATED_WEBHOOK`

## 7. Channel Delivery

After the delivery-created outbox event is published and consumed:

- email deliveries go to the email listener
- webhook deliveries go to the webhook listener

At this point the platform performs the actual send operation.

Examples:

- SMTP send for email
- HTTP POST for webhook

## 8. Retry Behavior

Channel delivery failures follow retry behavior described in:

- `docs/delivery/delivery-retry-dlq-flow.md`

At a high level:

- retryable failures can be retried
- non-retryable failures are rejected without requeue
- DLQ handling exists for outbox messaging flows

## 9. Monitoring Visibility

What you can currently observe:

- accepted events indirectly through metrics and outbox
- deliveries through delivery monitoring
- outbox publication state through outbox monitoring
- broker/system activity through Prometheus and Grafana

What you cannot currently observe clearly in the UI:

- accepted-but-not-routed events as a first-class screen
- skipped routing decisions caused by paused subscriptions

## Summary

The current lifecycle is:

```text
Ingest accepted
-> Event stored
-> Outbox EVENT_ACCEPTED
-> RabbitMQ
-> Routing
-> Delivery records created for active subscriptions only
-> Outbox DELIVERY_CREATED_*
-> RabbitMQ
-> Channel sender
```

And the most important rule is:

- subscription state is evaluated at routing time
- routing is not retroactive
