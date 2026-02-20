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
        main: "#1976d2",
        contrastText: "#ffffff",
      },
      secondary: {
        main: "#9c27b0",
        contrastText: "#ffffff",
      },
      background: {
        default: "#f5f5f5",
        paper: "#ffffff",
      },
    },
    typography: {
      fontFamily: "'Roboto', 'Helvetica', 'Arial', sans-serif",
      fontSize: 14,
    },
    shape: {
      borderRadius: 8,
    },
    mode: "light",
  },
};
