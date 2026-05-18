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
  raidDomain: string;
}
