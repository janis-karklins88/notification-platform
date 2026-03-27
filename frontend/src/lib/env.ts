const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? ''
const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL?.trim() ?? ''
const keycloakRealm = import.meta.env.VITE_KEYCLOAK_REALM?.trim() ?? ''
const keycloakClientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID?.trim() ?? ''

export const env = {
  apiBaseUrl,
  keycloakUrl,
  keycloakRealm,
  keycloakClientId,
} as const
