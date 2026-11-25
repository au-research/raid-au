import { describe, expect, it } from "vitest";
import { buildMuiTheme } from "./Buildtheme";
import { defaultTheme, neutralBranding } from "./DefaultConfig";
import { resolveBranding } from "./ResolveBranding";
import { ThemeConfig } from "./Appconfig";

describe("buildMuiTheme", () => {
  it("turns the configured palette, typography, shape and mode into the MUI theme", () => {
    const custom: ThemeConfig = {
      ...defaultTheme,
      palette: {
        ...defaultTheme.palette,
        primary: { main: "#8e489b", dark: "#552b5d" },
        secondary: { main: "#808080" },
        background: { default: "#f5f5f5", paper: "#fafafa" },
      },
      typography: { fontFamily: "Figtree, sans-serif", fontSize: 15 },
      shape: { borderRadius: 3 },
      mode: "dark",
    };

    const theme = buildMuiTheme(custom);

    expect(theme.palette.primary.main).toBe("#8e489b");
    expect(theme.palette.primary.dark).toBe("#552b5d");
    expect(theme.palette.secondary.main).toBe("#808080");
    expect(theme.palette.background.default).toBe("#f5f5f5");
    expect(theme.palette.background.paper).toBe("#fafafa");
    expect(theme.typography.fontFamily).toBe("Figtree, sans-serif");
    expect(theme.typography.fontSize).toBe(15);
    expect(theme.shape.borderRadius).toBe(3);
    expect(theme.palette.mode).toBe("dark");
  });

  it("falls back to light mode and an 8px radius when the config sets neither", () => {
    const { mode: _mode, shape: _shape, ...rest } = defaultTheme;

    const theme = buildMuiTheme(rest as ThemeConfig);

    expect(theme.palette.mode).toBe("light");
    expect(theme.shape.borderRadius).toBe(8);
  });

  it("gives a custom login theme's configured colours to the app, and the neutral colours otherwise", () => {
    const raw = {
      theme: { palette: { primary: { main: "#123456" } } },
      header: {},
      footer: {},
    };

    const custom = buildMuiTheme(resolveBranding(raw, "ardc-branding").theme);
    const neutral = buildMuiTheme(resolveBranding(raw, "raid-custom").theme);

    expect(custom.palette.primary.main).toBe("#123456");
    expect(neutral.palette.primary.main).toBe(neutralBranding.theme.palette.primary.main);
  });
});
