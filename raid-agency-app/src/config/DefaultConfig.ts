import { Branding, ThemeConfig } from "./Appconfig";

/** The look of the neutral branding: the MUI theme used when no custom branding applies. */
export const defaultTheme: ThemeConfig = {
  palette: {
    primary: {
      main: "#4183CE",
    },
    secondary: {
      main: "#DC8333",
    },
    background: {
      default: "#c6d9f0",
      dark: "#1b3e60",
      paper: "#ffffff"
    },
    error: {
      main: "#d32f2f",
    },
    warning: {
      main: "#f57c00",
    },
    info: {
      main: "#1976d2",
    },
    success: {
      main: "#2e7d32",
    },
    text: {
      primary: "#000000",
      secondary: "#555555"
    }
  },
  typography: {
    fontFamily: "Figtree, sans-serif",
    fontSize: 14,
  },
  shape: {
    borderRadius: 8,
  },
  mode: "light",
};

/**
 * The branding built into the app (RAID-1065). It names no agency and loads no
 * ARDC script. It applies when the realm uses the default login theme, when no
 * theme is returned, or when the theme lookup fails.
 */
export const neutralBranding: Branding = {
  theme: defaultTheme,
  header: {
    title: "RAiD",
    logo: {
      light: "/raid-logo-light.svg",
      dark: "/raid-logo-dark.svg",
      alt: "RAiD",
      height: 40,
    },
    navLinks: [],
    menu: "none",
  },
  footer: {
    variant: "generic",
    links: [],
  },
};
