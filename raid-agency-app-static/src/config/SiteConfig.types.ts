/**
 * SiteConfig types for the Astro static site.
 *
 * These match the React app's app-config.json schema for the
 * header and footer sections, so teams can reuse the same
 * config structure across both apps.
 */

export interface LogoConfig {
  src: string;
  alt: string;
  link?: string;
  height?: number;
}

export interface FooterLink {
  label: string;
  path: string;
}

export interface SocialLink {
  platform: string;
  url: string;
}

export interface TopBarConfig {
  show: boolean;
  label: string;
  href: string;
  height: string;
}

export interface FooterMainConfig {
  logos: LogoConfig[];
  text: string[];
}

export interface FooterConfig {
  show: boolean;
  main: FooterMainConfig;
  links: FooterLink[];
  copyright: string;
  showSocialLinks: boolean;
  socialLinks: SocialLink[];
}

export interface SiteConfig {
  header: {
    topBar: TopBarConfig;
  };
  footer: FooterConfig;
}
