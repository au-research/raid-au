// @ts-check
import { defineConfig } from "astro/config";
import { readFileSync } from "fs";
import { dirname, resolve } from "path";
import { fileURLToPath } from "url";

import tailwindcss from "@tailwindcss/vite";
import sitemap from "@astrojs/sitemap";

const __dirname = dirname(fileURLToPath(import.meta.url));

const embargoedRaids = JSON.parse(
  readFileSync(resolve(__dirname, "src/raw-data/embargoed-raids.json"), "utf-8")
);
const embargoedHandles = new Set(embargoedRaids.map((/** @type {{ handle: string }} */ r) => r.handle));

// Read siteUrl from app-config.json; fall back to prod default if absent.
let siteUrl = "https://static.prod.raid.org.au";
try {
  const appConfig = JSON.parse(
    readFileSync(resolve(__dirname, "public/app-config.json"), "utf-8")
  );
  if (appConfig.siteUrl) siteUrl = appConfig.siteUrl;
} catch {
  // file absent — use default
}

// https://astro.build/config
export default defineConfig({
  site: siteUrl,
  integrations: [
    sitemap({
      filter: (page) => {
        if (page.endsWith(".json/") || page.endsWith(".download/")) return false;

        const { pathname } = new URL(page);

        // Exclude the embargoed listing page
        if (pathname.startsWith("/embargoed")) return false;

        // Exclude individual embargoed RAiD pages
        const match = pathname.match(/^\/raids\/(.+?)\/?$/);
        if (match && embargoedHandles.has(match[1])) return false;

        return true;
      },
    }),
  ],
  vite: {
    plugins: [tailwindcss()],
  },
});
