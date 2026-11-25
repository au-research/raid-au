// src/config/Appconfig.ts

export interface NavLink {
  contact?: boolean;
  label: string;
  path: string;
  external?: boolean;
}

export interface ThemePaletteColor {
  main: string;
  light?: string;
  dark?: string;
  contrastText?: string;
}

export interface ThemeConfig {
  palette: {
    primary: ThemePaletteColor;
    secondary: ThemePaletteColor;
    background?: {
      default?: string;
      paper?: string;
      dark?: string;
    };
    error: ThemePaletteColor;
    warning: ThemePaletteColor;
    info: ThemePaletteColor;
    success: ThemePaletteColor;
    text: {
      primary: string;
      secondary: string;
    };
  };
  typography?: {
    fontFamily?: string;
    h1FontFamily?: string | null;
    fontSize?: number;
  };
  shape?: {
    borderRadius?: number;
  };
  mode?: "light" | "dark";
}

// Branding as chosen by the realm's login theme (RAID-1065).

/** Which ARDC web component the header mounts. "none" loads no ARDC script. */
export const HEADER_MENUS = ["none", "ardc"] as const;
export type HeaderMenu = (typeof HEADER_MENUS)[number];

/** Which footer component renders. "generic" loads no ARDC script. */
export const FOOTER_VARIANTS = ["generic", "ardc"] as const;
export type FooterVariant = (typeof FOOTER_VARIANTS)[number];

export interface BrandingLogo {
  light: string;
  dark: string;
  alt: string;
  height?: number;
}

export interface BrandingHeader {
  title: string;
  logo: BrandingLogo;
  navLinks: NavLink[];
  menu: HeaderMenu;
}

export interface BrandingFooter {
  variant: FooterVariant;
  links: NavLink[];
}

export interface Branding {
  theme: ThemeConfig;
  header: BrandingHeader;
  footer: BrandingFooter;
}
