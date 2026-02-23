// src/config/defaultConfig.ts

import { AppConfig } from "./Appconfig";

export const defaultConfig: AppConfig = {
  header: {
    logo: {
      src: "/logo.png",
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
        main: "rgb(65, 131, 206)",
        contrastText: "#ffffff",
      },
      secondary: {
        main: "#9c27b0",
        contrastText: "#ffffff",
      },
      background: {
        default: "#c6d9f0",
        paper: "#ffffff",
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
