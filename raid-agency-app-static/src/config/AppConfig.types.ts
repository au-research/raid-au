/**
 * AppConfig — the full runtime configuration for the RAiD static site.
 *
 * Loaded from public/app-config.json at build time.
 * Non-secret values only; secrets are always environment variables.
 *
 * The header/footer branding section is also served as /app-config.json
 * so nginx can proxy it from an external source (APP_CONFIG_URL) without
 * a rebuild — useful for Docker / Kubernetes / S3 deployments.
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

export interface AnalyticsConfig {
  gaMeasurementId?: string;
  gaMeasurementIdDemo?: string;
}

export interface CachingConfig {
  enabled: boolean;
  ttlMs: number;
}

export interface AppConfig {
  /** RAiD API base URL, e.g. https://app.demo.raid.org.au */
  apiEndpoint?: string;
  /** Keycloak IAM base URL */
  iamEndpoint?: string;
  /** Keycloak client ID for public data fetching */
  iamClientId?: string;
  /** Keycloak client ID for the raid-dumper (embargoed data) */
  raidDumperClientId?: string;
  /** Environment name: prod | demo | test | dev */
  raidEnv?: string;
  /** Public URL this site is served from (used for the sitemap) */
  siteUrl?: string;
  /** Base URL for resolving RAiD handles, e.g. https://raid.org/ */
  raidUrl?: string;
  /** Google Analytics measurement IDs */
  analytics?: AnalyticsConfig;
  /** Citation-cache settings for the data-fetch scripts */
  caching?: CachingConfig;
  /** Header branding */
  header: {
    topBar: TopBarConfig;
  };
  /** Footer branding */
  footer: FooterConfig;
}

/** Backwards-compat alias used by existing components */
export type SiteConfig = AppConfig;
