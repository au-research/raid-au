export interface RuntimeConfig {
  keycloak: {
    url: string;
    realm: string;
    clientId: string;
  };
  apiBaseUrl: string;
  environment: string;
  supportEmail: string;
  googleAnalytics: {
    measurementId?: string;
    measurementIdDemo?: string;
  };
  services: {
    orcid: string;
    invite?: string;
    staticProd: string;
    staticBase: string;
  };
  app: {
    orcid: {
      placeholder: string;
      helpText: string;
    };
  };
}
