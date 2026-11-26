// RAID-1066: the app's pages and shared components carry no ARDC wording or
// ardc.edu.au address. An agency's own details come from app-config.json.
//
// ARDC addresses that remain are listed below, each with its reason. A new
// file that mentions ARDC fails this test until it is fixed or listed here.

import { describe, expect, it } from "vitest";
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative, sep } from "node:path";

const SRC = join(__dirname);

const ALLOWED: Record<string, string> = {
  "components/footer-bar/footer.tsx": "the ARDC footer variant, chosen by footer.variant",
  "components/mega-menu/mega-menu.tsx": "the ARDC mega-menu, chosen by header.menu",
  "components/app-nav-bar/AppNavBar.tsx": "names the ARDC menu option",
  "constants/external-links.ts": "links used only by the ARDC footer variant",
  "containers/orcid-lookup/ORCID.tsx": "the ORCID lookup service, a working dependency",
  "entities/subject/data-generator/subject-data-generator.ts": "ARDC vocabulary service",
  "entities/subject/views/subject-item-view/SubjectItemView.tsx": "ARDC vocabulary service",
  "hooks/useExternalScript.ts": "comment describing the ARDC components",
  "services/isni.ts": "comment crediting a review",
  "types/ardc-web-components.d.ts": "types for the ARDC components",
  "config/Appconfig.ts": "names the ARDC variants",
  "config/DefaultConfig.ts": "comment",
};

const walk = (dir: string): string[] =>
  readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) return name === "generated" ? [] : walk(path);
    return /\.(ts|tsx)$/.test(name) && !/\.(test|spec)\.tsx?$/.test(name) ? [path] : [];
  });

// Word-bounded, so "CardContent" does not count.
const ARDC_WORDING = /\bARDC\b|Australian Research Data Commons|ardc\.edu\.au|Oceania/i;

describe("neutral source (RAID-1066)", () => {
  const offenders = walk(SRC)
    .map((path) => relative(SRC, path).split(sep).join("/"))
    .filter((file) => ARDC_WORDING.test(readFileSync(join(SRC, file), "utf-8")))
    .filter((file) => !(file in ALLOWED));

  it("has no ARDC wording or address outside the listed files", () => {
    expect(offenders).toEqual([]);
  });

  it("lists only files that still mention ARDC, so the list cannot go stale", () => {
    const stale = Object.keys(ALLOWED).filter(
      (file) => !ARDC_WORDING.test(readFileSync(join(SRC, file), "utf-8"))
    );
    expect(stale).toEqual([]);
  });
});
