import { afterEach, describe, expect, it } from "vitest";
import { render, screen, within } from "@testing-library/react";
import {
  Branding,
  BrandingProvider,
  neutralBranding,
  RuntimeConfig,
  RuntimeConfigProvider,
} from "@/config";
import { Footer } from "./footer";

const FOOTER_SCRIPT = "footer.min.js";

const links = [
  { label: "services@example.org", path: "mailto:services@example.org", contact: true },
  { label: "Terms of use", path: "https://example.org/terms" },
  { label: "Privacy policy", path: "https://example.org/privacy" },
];

const withFooter = (footer: Branding["footer"]): Branding => ({ ...neutralBranding, footer });

const runtime: RuntimeConfig = {
  keycloak: { url: "https://iam.example.org", realm: "raid", clientId: "raid-api" },
  apiBaseUrl: "https://api.example.org",
  environment: "demo",
  supportEmail: "help@example.org",
  googleAnalytics: {},
  services: {},
  app: { orcid: {} },
};

const renderFooter = (branding: Branding = neutralBranding, config: RuntimeConfig = runtime) =>
  render(
    <RuntimeConfigProvider config={config}>
      <BrandingProvider branding={branding}>
        <Footer />
      </BrandingProvider>
    </RuntimeConfigProvider>
  );

const footerScripts = () =>
  Array.from(document.querySelectorAll("script")).filter((s) => s.src.includes(FOOTER_SCRIPT));

afterEach(() => {
  // useExternalScript never removes what it adds, so clear between tests.
  document.querySelectorAll("script").forEach((s) => s.remove());
});

describe("Footer", () => {
  describe("neutral branding", () => {
    it("shows the helpdesk address as a support link", () => {
      renderFooter();

      const footer = screen.getByRole("contentinfo");
      expect(within(footer).getByRole("link", { name: "help@example.org" })).toHaveAttribute(
        "href",
        "mailto:help@example.org"
      );
      expect(footer).toHaveTextContent("Support");
    });

    it("shows no contact link when contactEmail is not set", () => {
      renderFooter();

      expect(screen.getByRole("contentinfo")).not.toHaveTextContent("Contact");
      expect(screen.getAllByRole("link")).toHaveLength(1);
    });

    it("shows a contact link when contactEmail is set", () => {
      renderFooter(neutralBranding, { ...runtime, contactEmail: "contact@example.org" });

      const footer = screen.getByRole("contentinfo");
      expect(within(footer).getByRole("link", { name: "contact@example.org" })).toHaveAttribute(
        "href",
        "mailto:contact@example.org"
      );
      expect(footer).toHaveTextContent("Contact");
      expect(within(footer).getByRole("link", { name: "help@example.org" })).toBeInTheDocument();
    });

    it("names no agency and loads no ARDC script or element", () => {
      const { container } = renderFooter();

      expect(footerScripts()).toHaveLength(0);
      expect(container.querySelector("ardc-footer")).toBeNull();
      expect(container).not.toHaveTextContent(/ARDC/i);
    });
  });

  describe("generic variant", () => {
    it("lists the configured links without contact entries", () => {
      renderFooter(withFooter({ variant: "generic", links }));

      const footer = screen.getByRole("contentinfo");
      expect(within(footer).getByRole("link", { name: "Terms of use" })).toHaveAttribute(
        "href",
        "https://example.org/terms"
      );
      expect(within(footer).getByRole("link", { name: "Privacy policy" })).toBeInTheDocument();
      expect(within(footer).queryByText("services@example.org")).not.toBeInTheDocument();
    });

    it("shows the support address as well as the configured links", () => {
      renderFooter(withFooter({ variant: "generic", links }));

      expect(
        within(screen.getByRole("contentinfo")).getByRole("link", { name: "help@example.org" })
      ).toBeInTheDocument();
    });

    it("opens web links in a new tab and mailto links in the same tab", () => {
      renderFooter(withFooter({ variant: "generic", links }));

      const terms = screen.getByRole("link", { name: "Terms of use" });
      expect(terms).toHaveAttribute("target", "_blank");
      expect(terms).toHaveAttribute("rel", "noopener noreferrer");
      expect(screen.getByRole("link", { name: "help@example.org" })).not.toHaveAttribute("target");
    });

    it("never loads the ARDC footer script or element", () => {
      const { container } = renderFooter(withFooter({ variant: "generic", links }));

      expect(footerScripts()).toHaveLength(0);
      expect(container.querySelector("ardc-footer")).toBeNull();
    });
  });

  describe("ardc variant", () => {
    it("renders the ARDC footer with the configured legal links, without contact entries", () => {
      const { container } = renderFooter(withFooter({ variant: "ardc", links }));

      const el = container.querySelector("ardc-footer");
      expect(el).not.toBeNull();
      const legal = el!.querySelector("#legal-links")!;
      expect(within(legal as HTMLElement).getByRole("link", { name: "Terms of use" })).toBeInTheDocument();
      expect(within(legal as HTMLElement).getByRole("link", { name: "Privacy policy" })).toBeInTheDocument();
      expect(within(legal as HTMLElement).queryByText("services@example.org")).toBeNull();
      expect(el!.querySelector("#quick-links")).not.toBeNull();
    });

    it("loads the ARDC footer script", () => {
      renderFooter(withFooter({ variant: "ardc", links }));

      expect(footerScripts()).toHaveLength(1);
    });

    it("renders even when no legal links are configured", () => {
      const { container } = renderFooter(withFooter({ variant: "ardc", links: [] }));

      expect(container.querySelector("ardc-footer")).not.toBeNull();
    });
  });
});
