import { describe, expect, it } from "vitest";
import { renderHook } from "@testing-library/react";
import { ReactNode } from "react";
import {
  Branding,
  BrandingProvider,
  neutralBranding,
  RuntimeConfig,
  RuntimeConfigProvider,
  useContactEmail,
} from "@/config";

const runtime: RuntimeConfig = {
  keycloak: { url: "https://iam.example.org", realm: "raid", clientId: "raid-api" },
  apiBaseUrl: "https://api.example.org",
  environment: "demo",
  supportEmail: "support@example.org",
  googleAnalytics: {},
  services: {},
  app: { orcid: {} },
};

const wrapperFor =
  (branding: Branding) =>
  ({ children }: { children: ReactNode }) => (
    <RuntimeConfigProvider config={runtime}>
      <BrandingProvider branding={branding}>{children}</BrandingProvider>
    </RuntimeConfigProvider>
  );

describe("useContactEmail", () => {
  it("falls back to supportEmail when no footer link is flagged as contact", () => {
    const { result } = renderHook(() => useContactEmail(), {
      wrapper: wrapperFor(neutralBranding),
    });
    expect(result.current).toBe("support@example.org");
  });

  it("uses the footer link flagged as contact", () => {
    const branding: Branding = {
      ...neutralBranding,
      footer: {
        variant: "ardc",
        links: [
          { label: "Terms", path: "https://example.org/terms" },
          { label: "services@ardc.edu.au", path: "mailto:services@ardc.edu.au", contact: true },
        ],
      },
    };
    const { result } = renderHook(() => useContactEmail(), { wrapper: wrapperFor(branding) });
    expect(result.current).toBe("services@ardc.edu.au");
  });
});
