import { Organisation } from "./Organisation";

// keycloak, apiBaseUrl, environment and supportEmail are guaranteed by
// loadConfig() (the app does not start without them). The optional fields may
// be missing from a stale app-config.json; the app shows a warning banner when
// they are (see ConfigValidation.ts).
export interface RuntimeConfig {
  keycloak: {
    url: string;
    realm: string;
    clientId: string;
  };
  apiBaseUrl: string;
  environment: string;
  supportEmail: string;
  contactEmail?: string;
  googleAnalytics: {
    measurementId?: string;
    measurementIdDemo?: string;
  };
  organisation: Organisation;
  services: {
    orcid?: string;
    invite?: string;
    staticProd?: string;
    staticBase?: string;
  };
  app: {
    orcid: {
      placeholder?: string;
      helpText?: string;
    };
  };
}
