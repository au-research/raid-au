import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { renderHook } from "@testing-library/react";
import { ReactNode } from "react";
import { RuntimeConfig, RuntimeConfigProvider } from "@/config";
import { useGoogleAnalytics } from "./useGoogleAnalytics";

const baseConfig: RuntimeConfig = {
  keycloak: { url: "https://iam.example.org", realm: "raid", clientId: "raid-api" },
  apiBaseUrl: "https://api.example.org",
  environment: "demo",
  supportEmail: "support@example.org",
  googleAnalytics: {},
  services: {},
  app: { orcid: {} },
};

const originalLocation = window.location;

const setHostname = (hostname: string) =>
  Object.defineProperty(window, "location", {
    configurable: true,
    value: { ...originalLocation, hostname },
  });

const renderWith = (googleAnalytics: RuntimeConfig["googleAnalytics"]) =>
  renderHook(() => useGoogleAnalytics(), {
    wrapper: ({ children }: { children: ReactNode }) => (
      <RuntimeConfigProvider config={{ ...baseConfig, googleAnalytics }}>
        {children}
      </RuntimeConfigProvider>
    ),
  });

const analyticsScripts = () =>
  Array.from(document.head.querySelectorAll("script")).filter((s) =>
    s.src.includes("googletagmanager.com")
  );

beforeEach(() => {
  document.head.querySelectorAll("script").forEach((s) => s.remove());
});

afterEach(() => {
  Object.defineProperty(window, "location", { configurable: true, value: originalLocation });
  delete (window as { gtag?: unknown }).gtag;
  delete (window as { dataLayer?: unknown }).dataLayer;
});

describe("useGoogleAnalytics", () => {
  it("loads nothing when googleAnalytics is left out", () => {
    setHostname("app.demo.example.org");
    renderWith({});

    expect(document.head.querySelectorAll("script")).toHaveLength(0);
  });

  it("loads nothing on a production host that has no measurementId", () => {
    setHostname("app.prod.example.org");
    renderWith({ measurementIdDemo: "G-DEMO123" });

    expect(document.head.querySelectorAll("script")).toHaveLength(0);
  });

  it("uses measurementIdDemo on a demo host", () => {
    setHostname("app.demo.example.org");
    renderWith({ measurementId: "G-PROD123", measurementIdDemo: "G-DEMO123" });

    const scripts = analyticsScripts();
    expect(scripts).toHaveLength(1);
    expect(scripts[0].src).toContain("G-DEMO123");
  });

  it("uses measurementId on a production host", () => {
    setHostname("app.prod.example.org");
    renderWith({ measurementId: "G-PROD123", measurementIdDemo: "G-DEMO123" });

    const scripts = analyticsScripts();
    expect(scripts).toHaveLength(1);
    expect(scripts[0].src).toContain("G-PROD123");
  });

  it("never loads on any other host, such as local development", () => {
    setHostname("localhost");
    renderWith({ measurementId: "G-PROD123", measurementIdDemo: "G-DEMO123" });

    expect(document.head.querySelectorAll("script")).toHaveLength(0);
  });
});
