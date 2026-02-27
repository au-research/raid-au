// src/config/defaultConfig.ts

import { AppConfig } from "./Appconfig";

export const defaultConfig: AppConfig = {
  default: true,
  header: {
    logo: {
      src: "/raid-logo-light.svg",
      alt: "App Logo",
      height: 40,
    },
    title: "RAiD",
    subtitle: "Research Activity Identifier",
    navLinks: [
      { label: "Home", path: "/" },
      { label: "Dashboard", path: "/dashboard" },
    ],
    showSearch: false,
  },
  footer: {
    copyright: `© ${new Date().getFullYear()} ARDC. All rights reserved.`,
    links: [
      { label: "Privacy Policy", path: "/privacy" },
      { label: "Terms of Service", path: "/terms" },
    ],
    showSocialLinks: false,
    socialLinks: [],
    main: []
  },
  content: {
    landingPage: {
      heroTitle: "Welcome to RAiD",
      heroSubtitle: "Manage your Research Activity Identifiers",
      showHero: true,
    },
  },
  theme: {
    palette: {
      primary: {
        main: "#4183CE",
      },
      secondary: {
        main: "#DC8333",
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
    },
    typography: {
      fontFamily: "Figtree, sans-serif",
      fontSize: 14,
    },
    shape: {
      borderRadius: 8,
    },
    mode: "light",
  },
};
