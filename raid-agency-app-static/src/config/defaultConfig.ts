/**
 * Default config used when public/app-config.json is absent.
 * Branding is hidden; no endpoints are set.
 */

import type { AppConfig } from "./AppConfig.types";

const defaultConfig: AppConfig = {
  header: {
    topBar: {
      show: false,
      label: "",
      href: "",
      height: "",
    },
  },
  footer: {
    show: false,
    main: {
      logos: [],
      text: [],
    },
    links: [],
    copyright: "",
    showSocialLinks: false,
    socialLinks: [],
  },
};

export default defaultConfig;
