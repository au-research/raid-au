import fs from "fs";
import path from "path";
import { describe, expect, it } from "vitest";
import subjectMapping from "@/mapping/data/subject-mapping.json";

const FOR_PREFIX = "https://linked.data.gov.au/def/anzsrc-for/2020/";
const SEO_PREFIX = "https://linked.data.gov.au/def/anzsrc-seo/2020/";

const RAIDS_PATH = path.resolve(__dirname, "../raw-data/raids.json");

function lookup(definition: string) {
  return subjectMapping.find((entry) => entry.definition === definition);
}

describe("subject-mapping.json", () => {
  it("has a non-empty key, value and definition on every entry", () => {
    for (const entry of subjectMapping) {
      expect(entry.key, JSON.stringify(entry)).toBeTruthy();
      expect(entry.value, JSON.stringify(entry)).toBeTruthy();
      expect(entry.definition, JSON.stringify(entry)).toBeTruthy();
    }
  });

  it("has no duplicate definitions", () => {
    const definitions = subjectMapping.map((entry) => entry.definition);
    expect(new Set(definitions).size).toBe(definitions.length);
  });

  it("ends every definition URI with its key", () => {
    for (const entry of subjectMapping) {
      expect(entry.definition.endsWith(`/${entry.key}`), entry.definition).toBe(true);
    }
  });

  it("only holds ANZSRC 2020 FoR and SEO URIs", () => {
    for (const entry of subjectMapping) {
      expect(
        entry.definition.startsWith(FOR_PREFIX) || entry.definition.startsWith(SEO_PREFIX),
        entry.definition
      ).toBe(true);
    }
  });

  // A RAiD subject id can point at any level of the scheme, so every level
  // must be mapped or the page renders "undefined (undefined)".
  it.each([
    ["FoR division (2 digits)", FOR_PREFIX, 2],
    ["FoR group (4 digits)", FOR_PREFIX, 4],
    ["FoR field (6 digits)", FOR_PREFIX, 6],
    ["SEO division (2 digits)", SEO_PREFIX, 2],
    ["SEO group (4 digits)", SEO_PREFIX, 4],
    ["SEO objective (6 digits)", SEO_PREFIX, 6],
  ])("maps %s codes", (_level, prefix, length) => {
    const matches = subjectMapping.filter(
      (entry) => entry.definition.startsWith(prefix) && entry.key.length === length
    );
    expect(matches.length).toBeGreaterThan(0);
  });

  it("resolves known codes at each level", () => {
    expect(lookup(`${FOR_PREFIX}36`)).toMatchObject({ key: "36", value: "CREATIVE ARTS AND WRITING" });
    expect(lookup(`${FOR_PREFIX}3702`)).toMatchObject({ key: "3702", value: "Climate change science" });
    expect(lookup(`${FOR_PREFIX}300101`)).toMatchObject({
      key: "300101",
      value: "Agricultural biotechnology diagnostics (incl. biosensors)",
    });
    expect(lookup(`${SEO_PREFIX}269903`)).toMatchObject({
      key: "269903",
      value: "Plant product traceability and quality assurance (excl. forest products)",
    });
  });

  it("returns undefined for a code outside the scheme", () => {
    expect(lookup(`${FOR_PREFIX}999999`)).toBeUndefined();
  });
});

// raids.json is fetched at build time and gitignored, so this only runs where
// the data has been fetched (for example after `npm run dev`).
describe.skipIf(!fs.existsSync(RAIDS_PATH))("subject-mapping.json against fetched RAiD data", () => {
  it("maps every subject id in raids.json", () => {
    const raids = JSON.parse(fs.readFileSync(RAIDS_PATH, "utf8"));
    const list: { subject?: { id: string }[] }[] = Array.isArray(raids) ? raids : Object.values(raids);

    const unmapped = new Set<string>();
    for (const raid of list) {
      for (const subject of raid.subject ?? []) {
        if (!lookup(subject.id)) unmapped.add(subject.id);
      }
    }

    expect([...unmapped]).toEqual([]);
  });
});
