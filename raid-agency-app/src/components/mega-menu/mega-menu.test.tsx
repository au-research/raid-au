import { afterEach, describe, expect, it } from "vitest";
import { render } from "@testing-library/react";
import { MegaMenu } from "./mega-menu";

const megaMenuScript = () =>
  document.querySelector<HTMLScriptElement>('script[src*="megamenu.min.js"]');

afterEach(() => {
  // useExternalScript never removes what it adds, so clear between tests.
  document.querySelectorAll("script").forEach((s) => s.remove());
});

describe("MegaMenu", () => {
  it("loads the ARDC mega-menu script and renders its element", () => {
    const { container } = render(<MegaMenu />);

    expect(megaMenuScript()).not.toBeNull();
    expect(container.querySelector("#ardc-menu")).not.toBeNull();
  });

  it("hides its element, and does not throw, when the script cannot be loaded", () => {
    const { container } = render(<MegaMenu />);

    expect(() => megaMenuScript()!.dispatchEvent(new Event("error"))).not.toThrow();

    expect(container.querySelector<HTMLElement>("#ardc-menu")!.style.display).toBe("none");
  });
});
