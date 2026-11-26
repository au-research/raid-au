import { describe, it, expect, vi, beforeEach } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { createTheme, ThemeProvider } from "@mui/material/styles";
import { MemoryRouter } from "react-router-dom";
import {
  Branding,
  BrandingProvider,
  neutralBranding,
  ConfigIssues,
  ConfigIssuesProvider,
  NO_CONFIG_ISSUES,
  RuntimeConfig,
  RuntimeConfigProvider,
  ThemeLookupFailedProvider,
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
  supportEmail: "support@example.org",
  googleAnalytics: {},
  organisation: {},
  services: {},
  app: { orcid: {} },
};

vi.mock("../mega-menu/mega-menu", () => ({
  MegaMenu: () => <div data-testid="mega-menu" />,
}));

const renderNavBar = (
  issues: ConfigIssues,
  themeLookupFailed = false,
  branding: Branding = neutralBranding,
  mode: "light" | "dark" = "light"
) =>
  render(
    <ThemeProvider theme={createTheme({ palette: { mode } })}>
      <RuntimeConfigProvider config={runtime}>
        <ConfigIssuesProvider issues={issues}>
          <ThemeLookupFailedProvider failed={themeLookupFailed}>
            <BrandingProvider branding={branding}>
              <MemoryRouter>
                <AppNavBar />
              </MemoryRouter>
            </BrandingProvider>
          </ThemeLookupFailedProvider>
        </ConfigIssuesProvider>
      </RuntimeConfigProvider>
    </ThemeProvider>
  );

const customBranding: Branding = {
  ...neutralBranding,
  header: {
    title: "Agency Research Activity Identifier",
    logo: { light: "/agency-light.svg", dark: "/agency-dark.svg", alt: "Agency logo", height: 30 },
    navLinks: [
      { label: "About", path: "https://example.org/about" },
      { label: "Help desk", path: "mailto:help@example.org" },
    ],
    menu: "ardc",
  },
};

describe("AppNavBar branding", () => {
  beforeEach(() => {
    mockKeycloak.authenticated = false;
  });

  it("shows the neutral title, logo and no mega menu by default", () => {
    renderNavBar(NO_CONFIG_ISSUES);

    expect(screen.getByText("RAiD")).toBeInTheDocument();
    expect(screen.queryByText(/ARDC Research Activity Identifier/)).not.toBeInTheDocument();
    expect(screen.getByRole("img", { name: "RAiD" })).toHaveAttribute("src", "/raid-logo-light.svg");
    expect(screen.queryByTestId("mega-menu")).not.toBeInTheDocument();
  });

  it("shows the configured title and logo", () => {
    renderNavBar(NO_CONFIG_ISSUES, false, customBranding);

    expect(screen.getByText("Agency Research Activity Identifier")).toBeInTheDocument();
    const logo = screen.getByRole("img", { name: "Agency logo" });
    expect(logo).toHaveAttribute("src", "/agency-light.svg");
    expect(logo).toHaveAttribute("height", "30");
  });

  it("uses the dark logo in dark mode", () => {
    renderNavBar(NO_CONFIG_ISSUES, false, customBranding, "dark");

    expect(screen.getByRole("img", { name: "Agency logo" })).toHaveAttribute("src", "/agency-dark.svg");
  });

  it("mounts the ARDC mega menu only when header.menu is ardc", () => {
    renderNavBar(NO_CONFIG_ISSUES, false, customBranding);
    expect(screen.getByTestId("mega-menu")).toBeInTheDocument();
  });

  it("does not mount the mega menu when header.menu is none", () => {
    renderNavBar(NO_CONFIG_ISSUES, false, {
      ...customBranding,
      header: { ...customBranding.header, menu: "none" },
    });
    expect(screen.queryByTestId("mega-menu")).not.toBeInTheDocument();
  });

  it("shows only Home and Your RAiDs when no links are configured", () => {
    mockKeycloak.authenticated = true;
    renderNavBar(NO_CONFIG_ISSUES);

    const links = screen.getAllByRole("link").map((a) => a.textContent);
    expect(links).toContain("Home");
    expect(links).toContain("Your RAiDs");
    expect(links).not.toContain("About");
    expect(links).not.toContain("Documentation");
    expect(links.some((text) => text?.includes("raid.org"))).toBe(false);
  });

  it("shows configured links after Home and Your RAiDs", () => {
    mockKeycloak.authenticated = true;
    renderNavBar(NO_CONFIG_ISSUES, false, customBranding);

    const about = screen.getByRole("link", { name: /About/ });
    expect(about).toHaveAttribute("href", "https://example.org/about");
    expect(about).toHaveAttribute("target", "_blank");
    expect(about).toHaveAttribute("rel", "noopener noreferrer");

    // mailto links open in the same tab
    const help = screen.getByRole("link", { name: /Help desk/ });
    expect(help).toHaveAttribute("href", "mailto:help@example.org");
    expect(help).not.toHaveAttribute("target");
  });

  it("lists configured links in the mobile menu", async () => {
    mockKeycloak.authenticated = true;
    renderNavBar(NO_CONFIG_ISSUES, false, customBranding);

    fireEvent.click(screen.getByLabelText("navigation menu"));
    const menu = await screen.findByRole("menu");
    expect(within(menu).getByText("Help desk")).toBeInTheDocument();
    expect(within(menu).getByText("About")).toBeInTheDocument();
  });
});

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
    expect(screen.queryByText(/could not load this site's branding/)).not.toBeInTheDocument();
  });

  it("shows the branding banner under the header when the theme lookup failed", () => {
    renderNavBar(NO_CONFIG_ISSUES, true);

    const banner = screen.getByRole("status");
    const stack = screen.getByTestId("app-nav-bar").nextElementSibling;
    expect(banner).toHaveTextContent("We could not load this site's branding");
    expect(banner).toHaveTextContent("support@example.org");
    expect(stack?.firstElementChild).toBe(banner);
  });

  it("shows the branding banner before the config warning when both apply", () => {
    renderNavBar({ missing: ["services.orcid"], invalid: [] }, true);

    const stack = screen.getByTestId("app-nav-bar").nextElementSibling;
    expect(stack?.children).toHaveLength(2);
    expect(stack?.children[0]).toHaveTextContent("We could not load this site's branding");
    expect(stack?.children[1]).toHaveTextContent("Some configuration is missing");
  });
});
