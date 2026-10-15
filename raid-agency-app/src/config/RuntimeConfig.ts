// keycloak, apiBaseUrl and environment are guaranteed by loadConfig() (the
// app does not start without them). The optional fields may be missing from
// a stale app-config.json; the app shows a warning banner when they are
// (see ConfigValidation.ts).
export interface RuntimeConfig {
  keycloak: {
    url: string;
    realm: string;
    clientId: string;
  };
  apiBaseUrl: string;
  environment: string;
  supportEmail?: string;
  googleAnalytics: {
    measurementId?: string;
    measurementIdDemo?: string;
  };
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
