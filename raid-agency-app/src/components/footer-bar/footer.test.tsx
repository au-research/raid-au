import { afterEach, describe, expect, it } from "vitest";
import { render, screen, within } from "@testing-library/react";
import { Branding, BrandingProvider, neutralBranding } from "@/config";
import { Footer } from "./footer";

const FOOTER_SCRIPT = "footer.min.js";

const links = [
  { label: "services@example.org", path: "mailto:services@example.org", contact: true },
  { label: "Terms of use", path: "https://example.org/terms" },
  { label: "Privacy policy", path: "https://example.org/privacy" },
];

const withFooter = (footer: Branding["footer"]): Branding => ({ ...neutralBranding, footer });

const renderFooter = (branding?: Branding) =>
  render(
    branding ? (
      <BrandingProvider branding={branding}>
        <Footer />
      </BrandingProvider>
    ) : (
      <Footer />
    )
  );

const footerScripts = () =>
  Array.from(document.querySelectorAll("script")).filter((s) => s.src.includes(FOOTER_SCRIPT));

afterEach(() => {
  // useExternalScript never removes what it adds, so clear between tests.
  document.querySelectorAll("script").forEach((s) => s.remove());
});

describe("Footer", () => {
  it("renders nothing for the neutral branding", () => {
    const { container } = renderFooter();

    expect(container).toBeEmptyDOMElement();
    expect(footerScripts()).toHaveLength(0);
  });

  describe("generic variant", () => {
    it("renders nothing when there are no links", () => {
      const { container } = renderFooter(withFooter({ variant: "generic", links: [] }));
      expect(container).toBeEmptyDOMElement();
    });

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

    it("opens web links in a new tab safely", () => {
      renderFooter(withFooter({ variant: "generic", links }));

      const terms = screen.getByRole("link", { name: "Terms of use" });
      expect(terms).toHaveAttribute("target", "_blank");
      expect(terms).toHaveAttribute("rel", "noopener noreferrer");
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
