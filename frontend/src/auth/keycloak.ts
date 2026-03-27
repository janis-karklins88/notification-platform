import Keycloak from "keycloak-js";
import { env } from "../lib/env"

export const keycloak = new Keycloak ({
  url: env.keycloakUrl,
  realm: env.keycloakRealm,
  clientId: env.keycloakClientId,
})