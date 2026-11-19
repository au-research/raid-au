import { describe, it, expect } from "vitest";
import {
  DEFAULT_LOGIN_THEME,
  isCustomLoginTheme,
  resolveBranding,
} from "./ResolveBranding";
import { neutralBranding } from "./DefaultConfig";

// What an agency with a custom realm theme would put in app-config.json.
const configuredBranding = {
  theme: { palette: { primary: { main: "#8E489B" } }, mode: "dark" },
  header: {
    title: "SURF RAiD",
    logo: { light: "/surf.svg", dark: "/surf-dark.svg", alt: "SURF" },
    navLinks: [{ label: "About", path: "https://example.org/about" }],
    menu: "ardc",
  },
  footer: {
    variant: "ardc",
    links: [{ label: "Privacy", path: "https://example.org/privacy" }],
  },
};

describe("isCustomLoginTheme", () => {
  it.each([
    [DEFAULT_LOGIN_THEME, false],
    [null, false],
    [undefined, false],
    ["", false],
    ["   ", false],
    ["ardc-branding", true],
    ["surf-branding", true],
  ])("%j is custom: %s", (theme, expected) => {
    expect(isCustomLoginTheme(theme)).toBe(expected);
  });

  it("treats the default theme name as exact, so a different case is custom", () => {
    expect(isCustomLoginTheme("RAID-CUSTOM")).toBe(true);
  });
});

describe("resolveBranding: neutral branding", () => {
  it("returns the built-in neutral branding for the default realm theme", () => {
    expect(resolveBranding({}, DEFAULT_LOGIN_THEME)).toEqual(neutralBranding);
  });

  it("ignores the branding sections of the config for the default theme", () => {
    const result = resolveBranding(configuredBranding, DEFAULT_LOGIN_THEME);

    expect(result).toEqual(neutralBranding);
    expect(result.header.title).not.toBe("SURF RAiD");
  });

  it.each([null, undefined, "", "   "])(
    "returns neutral branding when the theme is %j (no theme or a failed lookup)",
    (theme) => {
      expect(resolveBranding(configuredBranding, theme)).toEqual(neutralBranding);
    }
  );

  it("uses no ARDC components in neutral branding", () => {
    const result = resolveBranding(configuredBranding, null);

    expect(result.header.menu).toBe("none");
    expect(result.footer.variant).toBe("generic");
  });

  it("has no header or footer links in neutral branding", () => {
    expect(neutralBranding.header.navLinks).toEqual([]);
    expect(neutralBranding.footer.links).toEqual([]);
  });
});

describe("resolveBranding: custom realm theme", () => {
  it("keeps the neutral values when the config has no branding sections", () => {
    expect(resolveBranding({}, "ardc-branding")).toEqual(neutralBranding);
  });

  it("applies every configured section", () => {
    const result = resolveBranding(configuredBranding, "surf-branding");

    expect(result.header.title).toBe("SURF RAiD");
    expect(result.header.logo.light).toBe("/surf.svg");
    expect(result.header.logo.dark).toBe("/surf-dark.svg");
    expect(result.header.logo.alt).toBe("SURF");
    // The logo height was not configured, so it keeps its neutral value.
    expect(result.header.logo.height).toBe(neutralBranding.header.logo.height);
    expect(result.header.navLinks).toEqual([{ label: "About", path: "https://example.org/about" }]);
    expect(result.header.menu).toBe("ardc");
    expect(result.footer.variant).toBe("ardc");
    expect(result.theme.mode).toBe("dark");
    expect(result.theme.palette.primary.main).toBe("#8E489B");
  });

  it("merges field by field, so a field left out keeps its neutral value", () => {
    const result = resolveBranding({ header: { title: "Only a title" } }, "surf-branding");

    expect(result.header.title).toBe("Only a title");
    expect(result.header.logo).toEqual(neutralBranding.header.logo);
    expect(result.header.menu).toBe("none");
    expect(result.footer).toEqual(neutralBranding.footer);
  });

  it("merges nested theme values without losing the other palette colours", () => {
    const result = resolveBranding({ theme: { palette: { primary: { main: "#123456" } } } }, "surf-branding");

    expect(result.theme.palette.primary.main).toBe("#123456");
    expect(result.theme.palette.secondary).toEqual(neutralBranding.theme.palette.secondary);
    expect(result.theme.typography).toEqual(neutralBranding.theme.typography);
  });

  it("replaces link lists instead of merging them", () => {
    const result = resolveBranding(
      { footer: { links: [{ label: "Terms", path: "https://example.org/terms" }] } },
      "surf-branding"
    );

    expect(result.footer.links).toEqual([{ label: "Terms", path: "https://example.org/terms" }]);
  });

  it("applies the same rule to any non-default theme name", () => {
    const first = resolveBranding(configuredBranding, "ardc-branding");
    const second = resolveBranding(configuredBranding, "some-other-theme");

    expect(first).toEqual(second);
  });

  it.each([
    ["a string", "oops"],
    ["a number", 7],
    ["null", null],
    ["an array", []],
  ])("ignores a branding section that is %s", (_label, value) => {
    const result = resolveBranding({ header: value, footer: value, theme: value }, "surf-branding");

    expect(result).toEqual(neutralBranding);
  });
});

describe("resolveBranding: shared state", () => {
  it("does not change the built-in neutral branding when a custom theme is resolved", () => {
    const before = JSON.parse(JSON.stringify(neutralBranding));

    resolveBranding(configuredBranding, "surf-branding");

    expect(neutralBranding).toEqual(before);
    expect(resolveBranding({}, DEFAULT_LOGIN_THEME)).toEqual(before);
  });

  it("returns a copy, so changing the result does not change the neutral branding", () => {
    const result = resolveBranding({}, DEFAULT_LOGIN_THEME);

    result.header.title = "Changed";
    result.footer.links.push({ label: "x", path: "https://example.org" });

    expect(neutralBranding.header.title).toBe("RAiD");
    expect(neutralBranding.footer.links).toEqual([]);
  });
});
