// @ts-check
import { defineConfig } from "astro/config";

import sitemap from "@astrojs/sitemap";

// https://astro.build/config
// todo: replace `prod` with actual value per environment
export default defineConfig({
  site: `https://static.prod.raid.org.au`,
  integrations: [sitemap()],
});
