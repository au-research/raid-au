import { describe, it, expect } from "vitest";
import { sanitiseBranding } from "./SanitiseBranding";
import { resolveBranding } from "./ResolveBranding";
import { neutralBranding } from "./DefaultConfig";

const validBranding = {
  theme: { palette: { primary: { main: "#8E489B" } }, mode: "dark" },
  header: {
    title: "SURF RAiD",
    logo: { light: "/surf.svg", dark: "/surf-dark.svg", alt: "SURF" },
    menu: "ardc",
    navLinks: [
      { label: "About", path: "https://example.org/about" },
      { label: "Help", path: "mailto:help@example.org" },
    ],
  },
  footer: {
    variant: "ardc",
    links: [{ label: "Privacy", path: "https://example.org/privacy" }],
  },
};

describe("sanitiseBranding", () => {
  it("returns valid branding unchanged", () => {
    expect(sanitiseBranding(validBranding)).toEqual(validBranding);
  });

  it("returns empty sections when the config has no branding", () => {
    expect(sanitiseBranding({})).toEqual({ theme: {}, header: {}, footer: {} });
  });

  it("ignores everything outside theme, header and footer", () => {
    const result = sanitiseBranding({
      ...validBranding,
      keycloak: { url: "https://iam.example.org" },
      supportEmail: "help@example.org",
      branding: { header: { title: "Old layout" } },
    });

    expect(Object.keys(result).sort()).toEqual(["footer", "header", "theme"]);
  });

  it("does not change the object it is given", () => {
    const input = structuredClone(validBranding);
    input.header.menu = "bogus";
    const before = structuredClone(input);

    sanitiseBranding(input);

    expect(input).toEqual(before);
  });

  describe("sections that are not objects", () => {
    it.each(["oops", 7, null, []])("treats %j as an empty section", (value) => {
      const result = sanitiseBranding({ theme: value, header: value, footer: value });

      expect(result).toEqual({ theme: {}, header: {}, footer: {} });
    });
  });

  describe("single values", () => {
    it.each([
      ["an unknown header.menu", { header: { menu: "mega" } }, "menu"],
      ["a non-text header.menu", { header: { menu: 5 } }, "menu"],
      ["an unknown footer.variant", { footer: { variant: "fancy" } }, "variant"],
      ["a non-text header.title", { header: { title: 42 } }, "title"],
      ["a blank header.title", { header: { title: "   " } }, "title"],
      ["a null header.title", { header: { title: null } }, "title"],
    ])("drops %s so the neutral value applies", (_label, branding, key) => {
      const result = sanitiseBranding(branding);
      const section = "header" in branding ? result.header : result.footer;

      expect(section).not.toHaveProperty(key);
    });

    it("drops only the bad field and keeps the good ones", () => {
      const result = sanitiseBranding({ header: { title: "Keep me", menu: "mega", logo: { alt: "Keep" } } });

      expect(result.header).toEqual({ title: "Keep me", logo: { alt: "Keep" } });
    });

    it("drops a non-text logo field but keeps the rest of the logo", () => {
      const result = sanitiseBranding({ header: { logo: { light: 1, dark: "/d.svg" } } });

      expect(result.header).toEqual({ logo: { dark: "/d.svg" } });
    });

    it("drops header.logo when it is not an object", () => {
      const result = sanitiseBranding({ header: { title: "T", logo: "/a.svg" } });

      expect(result.header).toEqual({ title: "T" });
    });
  });

  describe.each([
    ["header", "navLinks"],
    ["footer", "links"],
  ])("%s.%s", (section, key) => {
    const run = (links: unknown) =>
      (sanitiseBranding({ [section]: { [key]: links } }) as Record<string, Record<string, unknown>>)[section][key];

    it("keeps safe links in their original order", () => {
      const links = [
        { label: "A", path: "https://example.org/a" },
        { label: "B", path: "mailto:b@example.org" },
      ];

      expect(run(links)).toEqual(links);
    });

    it("removes only the unsafe links and keeps the safe ones", () => {
      const result = run([
        { label: "Bad", path: "javascript:alert(1)" },
        { label: "Good", path: "https://example.org" },
        { label: "Insecure", path: "http://example.org" },
        { label: "Relative", path: "/privacy" },
        { label: "Mail", path: "mailto:x@example.org" },
      ]);

      expect(result).toEqual([
        { label: "Good", path: "https://example.org" },
        { label: "Mail", path: "mailto:x@example.org" },
      ]);
    });

    it("keeps an empty list when every link is unsafe, so the neutral list is not used", () => {
      expect(run([{ label: "Bad", path: "javascript:alert(1)" }])).toEqual([]);
    });

    it("drops the list when it is not an array", () => {
      expect(run("https://example.org")).toBeUndefined();
    });

    it("drops entries that are not links", () => {
      expect(run(["https://example.org", null, 5, { label: "Good", path: "https://example.org" }])).toEqual([
        { label: "Good", path: "https://example.org" },
      ]);
    });
  });

  it("passes the theme through untouched", () => {
    const theme = { palette: { primary: { main: "#123456" } }, mode: "dark", anything: ["goes"] };

    expect(sanitiseBranding({ theme }).theme).toEqual(theme);
  });
});

describe("sanitiseBranding feeding resolveBranding", () => {
  it("never lets a bad value reach the rendered branding", () => {
    const sanitised = sanitiseBranding({
      header: {
        title: 42,
        menu: "mega",
        logo: "/oops.svg",
        navLinks: [{ label: "Bad", path: "javascript:alert(1)" }],
      },
      footer: { variant: "fancy", links: "nope" },
    });

    const result = resolveBranding(sanitised, "surf-branding");

    expect(result.header.title).toBe(neutralBranding.header.title);
    expect(result.header.menu).toBe("none");
    expect(result.header.logo).toEqual(neutralBranding.header.logo);
    expect(result.header.navLinks).toEqual([]);
    expect(result.footer.variant).toBe("generic");
    expect(result.footer.links).toEqual([]);
  });

  it("keeps good values from a partly bad section", () => {
    const sanitised = sanitiseBranding({
      header: { title: "SURF RAiD", menu: "mega" },
    });

    const result = resolveBranding(sanitised, "surf-branding");

    expect(result.header.title).toBe("SURF RAiD");
    expect(result.header.menu).toBe("none");
  });
});
