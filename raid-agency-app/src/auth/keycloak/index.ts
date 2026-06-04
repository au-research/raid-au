import Keycloak from "keycloak-js";

export { useAuthHelper } from "./hooks/useAuthHelper";

let _instance: Keycloak | null = null;

export function initKeycloakInstance(config: {
  url: string;
  realm: string;
  clientId: string;
}): void {
  _instance = new Keycloak(config);
}

export function getKeycloakInstance(): Keycloak {
  if (!_instance) {
    throw new Error(
      "[Keycloak] Not initialized. Call initKeycloakInstance() before rendering."
    );
  }
  return _instance;
}
