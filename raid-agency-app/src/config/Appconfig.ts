// src/config/Appconfig.ts

export interface LogoConfig {
  src: string;
  alt: string;
  height?: number;
}

export interface NavLink {
  contact?: boolean;
  label: string;
  path: string;
  external?: boolean;
}

export interface LogosConfig {
  src: string;
  alt: string;
  height?: number;
  link: string;
}

export interface HeaderConfig {
  logo: LogoConfig;
  title: string;
  subtitle?: string;
  navLinks: NavLink[];
  showSearch?: boolean;
  toolBar?: boolean;
}

export interface SocialLink {
  platform: string;
  url: string;
}

export interface FooterConfig {
  copyright: string;
  links: NavLink[];
  showSocialLinks?: boolean;
  socialLinks?: SocialLink[];
  main: { logos: LogosConfig[], text: string[] };
}

export interface ContentConfig {
  landingPage?: {
    heroTitle?: string;
    heroSubtitle?: string;
    showHero?: boolean;
  };
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

// Branding as chosen by the realm's login theme (RAID-1065). These sit beside
// the existing AppConfig types until each consumer has moved across.

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

export interface AppConfig {
  header: HeaderConfig;
  footer: FooterConfig;
  content: ContentConfig;
  theme: ThemeConfig;
  default?: boolean; // Indicates if this is the default config
}
