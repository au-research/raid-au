import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import {
  AppConfigProvider,
  ConfigIssues,
  ConfigIssuesProvider,
  defaultConfig,
  NO_CONFIG_ISSUES,
  RuntimeConfig,
  RuntimeConfigProvider,
} from "@/config";
import { AppNavBar } from "./AppNavBar";

const mockKeycloak = { authenticated: false, tokenParsed: undefined, logout: vi.fn() };

vi.mock("@/contexts/keycloak-context", () => ({
  useKeycloak: () => mockKeycloak,
}));

vi.mock("@/auth/keycloak", () => ({
  useAuthHelper: () => ({ isOperator: false, isGroupAdmin: false }),
}));

vi.mock("@/shared/service-point/service-point-pending-request", () => ({
  useServicePointPendingRequest: () => undefined,
}));

vi.mock("@/containers/header/UserDropdown", () => ({
  UserDropdown: () => <div data-testid="user-dropdown" />,
}));

vi.mock("@/components/alert-notifications/Notifications", () => ({
  NotificationBell: () => null,
}));

const runtime: RuntimeConfig = {
  keycloak: { url: "https://iam.example.org", realm: "raid", clientId: "raid-api" },
  apiBaseUrl: "https://api.example.org",
  environment: "demo",
  googleAnalytics: {},
  services: {},
  app: { orcid: {} },
};

const renderNavBar = (issues: ConfigIssues) =>
  render(
    <RuntimeConfigProvider config={runtime}>
      <ConfigIssuesProvider issues={issues}>
        <AppConfigProvider config={defaultConfig}>
          <MemoryRouter>
            <AppNavBar />
          </MemoryRouter>
        </AppConfigProvider>
      </ConfigIssuesProvider>
    </RuntimeConfigProvider>
  );

describe("AppNavBar config warnings", () => {
  beforeEach(() => {
    mockKeycloak.authenticated = false;
  });

  it("shows the warning banner directly under the header when signed out", () => {
    renderNavBar({ missing: ["services.orcid"], invalid: [] });

    const banner = screen.getByRole("status");
    const stack = screen.getByTestId("app-nav-bar").nextElementSibling;
    expect(banner).toHaveTextContent("Some configuration is missing: services.orcid.");
    expect(stack).toHaveClass("notification-banner-stack");
    expect(stack?.children).toHaveLength(1);
    expect(stack?.firstElementChild).toBe(banner);
  });

  it("stacks the warning banner after the non-production banner when signed in", () => {
    mockKeycloak.authenticated = true;

    renderNavBar({ missing: ["services.orcid"], invalid: [] });

    const stack = screen.getByTestId("app-nav-bar").nextElementSibling;
    const nonProduction = screen.getByText(/This is not a production system/).closest(".notification-banner");
    expect(stack?.children).toHaveLength(2);
    expect(stack?.children[0]).toBe(nonProduction);
    expect(stack?.children[1]).toBe(screen.getByRole("status"));
  });

  it("shows no banner when the config has no issues", () => {
    renderNavBar(NO_CONFIG_ISSUES);

    expect(screen.queryByText(/Some configuration/)).not.toBeInTheDocument();
  });
});
