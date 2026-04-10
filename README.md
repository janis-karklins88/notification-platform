# Notification Platform

Multi-tenant event-driven notification platform built with Spring Boot, RabbitMQ, PostgreSQL, React, and Keycloak.

It accepts events through an ingest API, routes them through tenant subscriptions, and delivers them through tenant-owned endpoints such as email and webhooks.

## What It Does

- Ingests events with tenant API keys
- Routes events to subscriptions by event type
- Delivers notifications through:
  - email endpoints
  - webhook endpoints
- Supports tenant-scoped admin management for:
  - tenants
  - endpoints
  - subscriptions
  - API keys
  - email templates
  - deliveries
- Uses an outbox pattern for reliable broker publication
- Exposes Prometheus metrics and supports Grafana dashboards

## Stack

- Backend: Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Flyway
- Frontend: React 19, TypeScript, Vite, React Query, React Router
- Infrastructure: PostgreSQL, RabbitMQ, Keycloak, MailHog, Prometheus, Grafana

## Repository Layout

```text
backend/    Spring Boot application
frontend/   React admin UI
infra/      Prometheus and Grafana provisioning
docs/       Supporting docs
```

## Core Flow

1. A tenant gets an API key.
2. A client sends an event to `POST /ingest` with `X-API-Key`.
3. The platform stores and routes the event.
4. Matching active subscriptions create deliveries.
5. Deliveries are sent through the configured endpoint.
6. Delivery and outbox activity can be monitored through admin APIs and metrics.

Important current behavior:

- Subscription state is evaluated at ingest/routing time.
- If a subscription is paused, no delivery is created.
- Reactivating a subscription is not retroactive.
- To process a missed event after reactivation, resend it as a new event.

## Prerequisites

- Java 21
- Node.js
- Docker with Compose

## Local Development

Recommended local setup:

- Run infrastructure with Docker
- Run backend locally with the `local` profile
- Run frontend locally with Vite

This is still the easiest setup for development, but both `local` and `docker` backend profiles now expose Prometheus metrics.

### 1. Start Infrastructure

From the repository root:

```powershell
docker compose up -d postgres rabbitmq mailhog keycloak prometheus grafana
```

Useful URLs:

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- Keycloak: `http://localhost:8081`
- MailHog: `http://localhost:8025`
- RabbitMQ UI: `http://localhost:15672`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

Default infrastructure credentials:

- PostgreSQL: `notif / notif`
- RabbitMQ: `notif / notif`
- Keycloak admin: `admin / admin`
- Seeded Keycloak app user: `platform-admin / platform-admin`
- Grafana: `admin / admin`

### 2. Start Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The local profile uses:

- PostgreSQL on `localhost:5432`
- RabbitMQ on `localhost:5672`
- MailHog SMTP on `localhost:1025`
- Keycloak issuer `http://localhost:8081/realms/NotificationPlatforAdmins`

### 3. Start Frontend

The frontend already has a local env file at `frontend/.env.local`.

Expected values:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_KEYCLOAK_URL=http://localhost:8081
VITE_KEYCLOAK_REALM=NotificationPlatforAdmins
VITE_KEYCLOAK_CLIENT_ID=notification-platform-frontend
```

Run it with:

```powershell
cd frontend
npm install
npm run dev
```

## Docker Compose

`docker-compose.yml` includes:

- `postgres`
- `rabbitmq`
- `mailhog`
- `keycloak`
- `prometheus`
- `grafana`
- `backend`

You can run the full stack with:

```powershell
docker compose up -d
```

Note:

- The backend `docker` profile now exposes `health`, `info`, `metrics`, and `prometheus`.
- Prometheus in Docker is configured to scrape `host.docker.internal:8080/actuator/prometheus`.
- If you run the backend in Docker on port `8080`, Prometheus and Grafana can monitor it without the backend running locally.

## Authentication

Admin endpoints require a Keycloak JWT with the realm role `PLATFORM_ADMIN`.

Current frontend auth config expects:

- Keycloak realm: `NotificationPlatforAdmins`
- Keycloak client: `notification-platform-frontend`

For Docker-based local setup, the repo now includes an automatic Keycloak realm import.

By default, Docker Compose imports:

1. Realm `NotificationPlatforAdmins`
2. Client `notification-platform-frontend`
3. Realm role `PLATFORM_ADMIN`
4. Dev user `platform-admin`

Detailed setup steps are documented in `docs/auth/keycloak-setup.md`.

Important:

- Keycloak imports realms from `infra/keycloak/import`
- import happens on startup only when the realm does not already exist
- if you already have an existing Keycloak database volume, the import will be skipped
- to re-seed from scratch locally, remove the Keycloak database volume and start the stack again

## Main Admin Areas

The admin UI is tenant-first.

Main navigation:

- `Tenants`
- `Deliveries`

Inside a tenant:

- `Overview`
- `Endpoints`
- `Subscriptions`
- `API Keys`
- `Email Templates`
- `Deliveries`

There are also cross-tenant list views under the tenants area for:

- endpoints
- subscriptions
- API keys
- email templates
- deliveries

## Email Templates

Email templates are stored in the database and are tenant-owned.

Endpoints support two modes:

- reusable DB template via `templateName`
- endpoint-specific custom template via inline `subjectTemplate` and `bodyTemplate`

Template placeholders use the inline syntax:

```text
{{payload.orderId}}
{{payload.customerName}}
```

Soft delete behavior:

- deleting an email template sets it inactive
- inactive templates are hidden from the UI

## Webhook Endpoint Config

Example webhook endpoint config:

```json
{
  "url": "https://your-service.example.com/webhooks/notifications",
  "headers": {
    "X-API-Key": "your-secret"
  },
  "connectTimeoutMs": 2500,
  "responseTimeoutMs": 5000,
  "connectionRequestTimeoutMs": 2000
}
```

For testing, `webhook.site` works well with:

```json
{
  "url": "https://webhook.site/your-id-here"
}
```

## Ingest API

`POST /ingest`

Authentication:

- header `X-API-Key: <tenant plaintext key>`

Example request:

```json
{
  "eventType": "order.created",
  "idempotencyKey": "order-100045",
  "source": "shopify",
  "traceId": "trace-order-100045",
  "payload": {
    "orderId": "100045",
    "customerName": "Janis Berzins",
    "customerEmail": "janis@example.com",
    "createdAt": "2026-04-10T12:00:00Z",
    "status": "PAID",
    "currency": "EUR",
    "subtotal": "79.98",
    "shipping": "5.00",
    "tax": "16.80",
    "total": "101.78",
    "productsSummary": "2 x Espresso Beans 1kg - EUR 39.99\n1 x Milk Frother - EUR 39.99"
  }
}
```

## Monitoring

### Delivery Monitoring

Current admin delivery monitoring shows actual delivery records.

If a subscription is paused at ingest time:

- the event may still be accepted
- no delivery is created
- nothing appears in the deliveries list for that skipped routing decision

### Outbox Monitoring

Current outbox monitoring exists as backend APIs:

- `GET /admin/outbox-events`
- `GET /admin/outbox-events/{id}`

It shows outbox publication state, including:

- `status`
- `attemptCount`
- `availableAt`
- `lastAttemptAt`
- `publishedAt`
- `lastError`
- aggregate and event metadata

Outbox statuses:

- `PENDING`
- `IN_PROGRESS`
- `PUBLISHED`
- `FAILED`

### Prometheus

With the backend running on port `8080` in either `local` or `docker` profile:

- metrics endpoint: `http://localhost:8080/actuator/prometheus`
- Prometheus UI: `http://localhost:9090`

Useful queries:

```text
up
notification_event_accepted_total
notification_deliveries_created_total
notification_delivery_success_total
notification_delivery_failure_total
notification_outbox_published_total
notification_outbox_publish_failed_total
```

Rate examples:

```text
rate(notification_event_accepted_total[5m])
rate(notification_delivery_success_total[5m])
rate(notification_delivery_failure_total[5m])
```

### Grafana

- URL: `http://localhost:3000`
- login: `admin / admin`

If no dashboard is configured yet, use `Explore` with the Prometheus datasource and run the same queries as above.

## Testing

Backend:

```powershell
cd backend
.\mvnw.cmd test
```

Frontend:

```powershell
cd frontend
npm run build
```

## Notes

- Mail delivery can be inspected in MailHog at `http://localhost:8025`
- Delivery retry and DLQ notes are documented in `docs/delivery/delivery-retry-dlq-flow.md`
- The frontend sub-navigation supports both global resource views and tenant-scoped views

## Further Docs

- `docs/auth/keycloak-setup.md`
- `docs/architecture/architecture-overview.md`
- `docs/architecture/event-lifecycle.md`
- `docs/deployment/deployment-notes.md`
- `docs/delivery/delivery-retry-dlq-flow.md`
