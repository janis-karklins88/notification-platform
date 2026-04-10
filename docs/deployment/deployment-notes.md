# Deployment Notes

This document describes how the project should be thought about outside local development.

## Local vs Docker vs Production

The repository currently supports:

- local development
  - backend run locally with `local` profile
  - infrastructure in Docker
- full Docker Compose
  - backend, database, broker, Keycloak, and monitoring in Docker

For real production use, deployment should be treated differently from the local developer setup.

## Recommended Production Shape

Deploy these as separate services:

- frontend
- backend
- PostgreSQL
- RabbitMQ
- Keycloak or another identity provider
- Prometheus
- Grafana
- reverse proxy or ingress

## Reverse Proxy and TLS

A reverse proxy should sit in front of the application.

Typical responsibilities:

- terminate TLS
- expose public domains
- route traffic to frontend and backend
- hide internal services from the public internet
- apply security headers or rate limiting if needed

Examples:

- Nginx
- Traefik
- HAProxy
- cloud load balancer / ingress

TLS means using HTTPS with certificates so browser and API traffic is encrypted in transit.

## Production Concerns

For live use, the following should be addressed explicitly:

- HTTPS everywhere
- secret management outside the repository
- persistent storage and backups
- environment separation
- monitoring and alerts
- log aggregation
- rollback and migration strategy

## Secrets

Do not store production secrets in repository config.

Move these to a secret store or deployment environment:

- database password
- RabbitMQ credentials
- SMTP credentials
- Keycloak client and realm-related secrets if using confidential clients
- notification sender config if environment-specific

## Data Services

PostgreSQL:

- prefer managed PostgreSQL if available
- enable backups and restore testing
- monitor storage, CPU, connections, and slow queries

RabbitMQ:

- use durable queues and persistent messages
- monitor queue depth and consumer lag
- set operational alerts for broker health

## Email

MailHog is for local development only.

Production should use a real SMTP provider or managed mail service such as:

- Amazon SES
- SendGrid
- Mailgun

Also review:

- sender domain setup
- SPF, DKIM, DMARC
- SMTP timeout and retry behavior

## Authentication

Current local setup uses Keycloak.

For production:

- either run Keycloak properly with persistent storage and backup strategy
- or use a managed identity provider

For local/dev convenience, this repo bootstraps Keycloak with a realm import file.


Admin auth expectations remain the same:

- backend validates JWT issuer
- admin users need `PLATFORM_ADMIN`

## Monitoring

Current monitoring stack:

- Prometheus
- Grafana
- application metrics through Actuator and Micrometer

Production additions should include:

- alerting rules
- centralized logs
- dashboards for throughput, failures, retries, and broker health

## CI/CD Expectations

Before calling the system production-ready, deployment should be automated.

Minimum pipeline:

1. Build backend and frontend
2. Run backend tests
3. Run frontend build
4. Build container images
5. Push images to registry
6. Deploy to target environment
7. Run smoke checks

## Migration Strategy

Flyway is already used for schema migrations.

Production deployment should make migration order explicit:

1. deploy new backend version
2. run Flyway migration
3. verify app health
4. shift traffic fully

The exact order depends on whether schema changes are backward compatible, but migration must be part of the deployment plan.

## Current Repo-Specific Notes

Relevant operational details for this project:

- Docker backend now exposes `health`, `info`, `metrics`, and `prometheus`
- Prometheus is configured to scrape `host.docker.internal:8080/actuator/prometheus`
- admin UI relies on Keycloak login from the browser
- ingest uses tenant API keys, not Keycloak
- paused subscriptions do not create deliveries and are not retroactive after reactivation

## Good Production Default Answer

If asked how this project should be deployed live, the short answer is:

- frontend and backend as versioned containers
- Postgres and RabbitMQ as persistent services
- Keycloak or managed OIDC provider for admin auth
- reverse proxy with TLS in front
- Prometheus and Grafana for monitoring
- Flyway migrations in the deployment pipeline
- secrets, backups, alerts, and logs handled outside local-style config
