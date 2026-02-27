// src/config/Appconfig.ts

export interface LogoConfig {
  src: string;
  alt: string;
  height?: number;
}

export interface NavLink {
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
    };
    error: ThemePaletteColor;
    warning: ThemePaletteColor;
    info: ThemePaletteColor;
    success: ThemePaletteColor;
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

export interface AppConfig {
  header: HeaderConfig;
  footer: FooterConfig;
  content: ContentConfig;
  theme: ThemeConfig;
  default?: boolean; // Indicates if this is the default config
}
