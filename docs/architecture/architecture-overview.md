# Architecture Overview

This project is a multi-tenant notification platform built around event ingest, routing, outbox publishing, and channel-specific delivery.

## Main Building Blocks

- `ingest`
  - accepts tenant-authenticated inbound events
  - stores events
  - emits an outbox event for asynchronous routing
- `routing`
  - models subscriptions
  - decides which active subscriptions match an event
- `delivery`
  - models endpoints, deliveries, and email templates
  - creates delivery records
  - sends email and webhook notifications
- `outbox`
  - persists integration events in the database
  - publishes them to RabbitMQ reliably
- `adminapi`
  - provides the secured admin surface for tenants, endpoints, subscriptions, API keys, templates, deliveries, and outbox monitoring
- `auth`
  - handles ingest API key authentication
  - integrates admin JWT-based auth through Keycloak
- `tenant`
  - holds tenant ownership and identity
- `shared`
  - common utilities and metrics

## High-Level Flow

```text
Client -> /ingest -> Event persisted -> OutboxEvent(EVENT_ACCEPTED)
       -> RabbitMQ -> EventAcceptedListener -> routeEvent(...)
       -> Delivery persisted -> OutboxEvent(DELIVERY_CREATED_*)
       -> RabbitMQ -> Email/Webhook listener -> channel sender
```

## Inbound Side

Inbound events arrive through `POST /ingest`.

- Authentication is done with tenant API keys through `X-API-Key`
- Events are stored in the database
- Idempotency is supported through tenant-scoped `idempotencyKey`
- Accepted events create an outbox record with type `EVENT_ACCEPTED`

Relevant code:

- `ingest/adapter/in/web/IngestController`
- `ingest/application/service/IngestService`

## Routing Side

Routing happens asynchronously after the accepted-event outbox message is published and consumed.

The router:

- loads the stored event
- checks matching active subscriptions by tenant and event type
- creates delivery records for matching subscriptions
- creates outbox records for delivery creation
- marks the event as routed

Important behavior:

- paused subscriptions are excluded
- if no active subscription matches, no delivery is created
- reactivating a subscription is not retroactive for already routed events

Relevant code:

- `delivery/application/service/DeliveryService`
- `delivery/adapter/in/messaging/EventAcceptedListener`

## Delivery Side

Deliveries are created as records first, then processed asynchronously by channel.

Currently implemented channels:

- `EMAIL`
- `WEBHOOK`

Delivery processing is split by queue and listener:

- `np.delivery.delivery.created.email`
- `np.delivery.delivery.created.web`

Relevant code:

- `delivery/adapter/in/messaging/DeliveryEmailListener`
- `delivery/adapter/in/messaging/DeliveryWebhookListener`
- `delivery/application/service/EmailDeliveryService`
- `delivery/application/service/WebhookDeliveryService`

## Template Model

Email delivery supports two template modes:

- reusable DB-backed tenant template selected by `templateName`
- endpoint-specific custom subject/body stored directly in endpoint config

DB templates are tenant-owned and managed through the admin UI.

## Outbox Pattern

The platform uses a database-backed outbox to decouple transactional writes from RabbitMQ publication.

Why:

- event acceptance and delivery creation can commit locally first
- message publication can be retried separately
- broker publish state is observable through outbox monitoring

Current outbox event types:

- `EVENT_ACCEPTED`
- `DELIVERY_CREATED_EMAIL`
- `DELIVERY_CREATED_WEBHOOK`

Current outbox statuses:

- `PENDING`
- `IN_PROGRESS`
- `PUBLISHED`
- `FAILED`

Relevant code:

- `outbox/application/service/OutboxClaimService`
- `outbox/application/service/OutboxPublishService`
- `outbox/application/service/OutboxFinalizeService`
- `adminapi/adapter/in/web/OutboxMonitoringController`

## Security Model

Two different authentication models are used:

- `/ingest/**`
  - authenticated by tenant API key
- `/admin/**`
  - authenticated by JWT through Keycloak
  - requires `PLATFORM_ADMIN`

Relevant code:

- `config/SecurityConfig`
- `auth/adapter/in/security/ApiKeyAuthenticationFilter`

## Admin UI Model

The frontend follows a tenant-first structure.

Global views:

- tenants
- deliveries
- cross-tenant resource lists

Tenant-scoped views:

- overview
- endpoints
- subscriptions
- API keys
- email templates
- deliveries

## Operational Surfaces

Current operational visibility is split across:

- deliveries monitoring
  - actual delivery records only
- outbox monitoring
  - outbox publication state
- Prometheus and Grafana
  - counters and rates

Current limitation:

- accepted-but-not-routed events are not shown in deliveries
- paused subscription skips do not create delivery records
