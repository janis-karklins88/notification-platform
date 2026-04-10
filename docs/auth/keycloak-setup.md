# Keycloak Setup

This project uses Keycloak for admin authentication in the frontend and JWT validation in the backend.

## Required Values

The current project expects:

- Keycloak base URL: `http://localhost:8081`
- Realm: `NotificationPlatforAdmins`
- Frontend client ID: `notification-platform-frontend`
- Admin realm role: `PLATFORM_ADMIN`

These values are referenced by:

- `backend/src/main/resources/application-local.yml`
- `frontend/.env.local`

## Automatic Local Bootstrap

For Docker-based local development, Keycloak is now auto-seeded from:

- `infra/keycloak/import/NotificationPlatforAdmins-realm.json`

Docker Compose starts Keycloak with:

- `start-dev --import-realm`

and mounts the import directory to:

- `/opt/keycloak/data/import`

This creates the required realm, client, role, and a dev admin user on a fresh Keycloak database.

Seeded local app user:

- username: `platform-admin`
- password: `platform-admin`

This seeded user is for local/dev use only.

## What Must Be Configured In Keycloak

If you are not using the Docker auto-import flow, configure Keycloak manually as follows.

Create the following:

1. Realm `NotificationPlatforAdmins`
2. Client `notification-platform-frontend`
3. Realm role `PLATFORM_ADMIN`
4. A user assigned to `PLATFORM_ADMIN`

## Client Settings

For client `notification-platform-frontend`, use a browser SPA style setup.

Recommended settings:

- Client type: `OpenID Connect`
- Access type / Client authentication: public client
- Standard flow: enabled
- PKCE: enabled or allowed
- Direct access grants: disabled
- Service accounts: disabled

Redirect settings:

- Valid redirect URIs:
  - `http://localhost:5173/*`
- Web origins:
  - `http://localhost:5173`

## User Setup

Create a user for admin access and assign the realm role:

- `PLATFORM_ADMIN`

Without that role, login may succeed but admin API calls under `/admin/**` will be rejected.

## Frontend Configuration

The frontend reads these values from `frontend/.env.local`:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_KEYCLOAK_URL=http://localhost:8081
VITE_KEYCLOAK_REALM=NotificationPlatforAdmins
VITE_KEYCLOAK_CLIENT_ID=notification-platform-frontend
```

What they mean:

- `VITE_API_BASE_URL`: backend base URL
- `VITE_KEYCLOAK_URL`: Keycloak server URL
- `VITE_KEYCLOAK_REALM`: realm name
- `VITE_KEYCLOAK_CLIENT_ID`: SPA client ID

## Backend Configuration

The backend validates JWTs using the issuer URI configured in `application-local.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081/realms/NotificationPlatforAdmins
```

That means the backend expects:

- tokens issued by realm `NotificationPlatforAdmins`
- admin users to have role `PLATFORM_ADMIN`

## How It Works

1. The frontend initializes Keycloak with `login-required`.
2. The user authenticates in Keycloak.
3. Keycloak returns an access token.
4. The frontend sends that token as `Authorization: Bearer <token>`.
5. The backend validates the token issuer and roles.

## Troubleshooting

If authentication is not working, check these first:

- realm name is exactly `NotificationPlatforAdmins`
- client ID is exactly `notification-platform-frontend`
- redirect URI includes `http://localhost:5173/*`
- web origin includes `http://localhost:5173`
- backend issuer URI points to the same realm
- logged-in user has realm role `PLATFORM_ADMIN`

If Docker import does not seem to apply:

- check that `docker compose` is mounting `infra/keycloak/import`
- check Keycloak logs for realm import messages
- remember that `--import-realm` skips import if the realm already exists
- if needed, delete the local Keycloak database volume and start again

Typical symptoms:

- login page loops or redirect failure:
  - redirect URI or web origin is wrong
- frontend loads but admin requests return `401`:
  - token missing, expired, or issuer mismatch
- admin requests return `403`:
  - user authenticated but does not have `PLATFORM_ADMIN`
