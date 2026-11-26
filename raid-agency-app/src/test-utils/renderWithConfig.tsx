import { ReactElement } from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { RuntimeConfig, RuntimeConfigProvider } from "@/config";

export const baseRuntimeConfig: RuntimeConfig = {
  keycloak: { url: "https://iam.example.org", realm: "raid", clientId: "raid-api" },
  apiBaseUrl: "https://api.example.org",
  environment: "demo",
  supportEmail: "helpdesk@example.org",
  googleAnalytics: {},
  organisation: {},
  services: {},
  app: { orcid: {} },
};

/** Renders a page inside a router and a RuntimeConfigProvider. */
export const renderWithConfig = (
  ui: ReactElement,
  overrides: Partial<RuntimeConfig> = {}
) =>
  render(
    <RuntimeConfigProvider config={{ ...baseRuntimeConfig, ...overrides }}>
      <MemoryRouter>{ui}</MemoryRouter>
    </RuntimeConfigProvider>
  );
