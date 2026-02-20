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

export interface HeaderConfig {
  logo: LogoConfig;
  title: string;
  subtitle?: string;
  navLinks: NavLink[];
  showSearch?: boolean;
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
}
