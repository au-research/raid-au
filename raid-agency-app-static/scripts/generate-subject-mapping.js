/**
 * Regenerates src/mapping/data/subject-mapping.json from the ARDC Research
 * Vocabularies Australia API (ANZSRC 2020 FoR and SEO).
 *
 * The mapping covers every level of each scheme (divisions, groups and
 * fields for FoR; divisions, groups and objectives for SEO), because a RAiD
 * subject id can point at any level.
 *
 * Existing entries are kept as they are. Only missing definitions are added,
 * so the output is stable between runs.
 *
 * Usage: node ./scripts/generate-subject-mapping.js
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const MAPPING_PATH = path.join(__dirname, "../src/mapping/data/subject-mapping.json");

const API_BASE = "https://vocabs.ardc.edu.au/repository/api/lda";
const PAGE_SIZE = 200;

const VOCABULARIES = ["anzsrc-2020-for", "anzsrc-2020-seo"];

async function fetchAllConcepts(vocabulary) {
  const concepts = [];
  for (let page = 0; ; page++) {
    const url = `${API_BASE}/${vocabulary}/concept.json?_pageSize=${PAGE_SIZE}&_page=${page}`;
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`${url} returned HTTP ${response.status}`);
    }
    const { result } = await response.json();
    concepts.push(...result.items);
    if (!result.next) break;
  }
  return concepts;
}

function toEntry(concept) {
  const label = concept.prefLabel?._value ?? concept.prefLabel;
  if (!concept._about || !concept.notation || typeof label !== "string") {
    throw new Error(`Concept is missing a uri, notation or label: ${JSON.stringify(concept)}`);
  }
  return {
    field: "subject.type.id",
    key: String(concept.notation),
    value: label,
    definition: concept._about,
  };
}

const existingText = fs.readFileSync(MAPPING_PATH, "utf8");
const eol = existingText.includes("\r\n") ? "\r\n" : "\n";
const existing = JSON.parse(existingText);
const known = new Set(existing.map((entry) => entry.definition));

const added = [];
for (const vocabulary of VOCABULARIES) {
  const concepts = await fetchAllConcepts(vocabulary);
  console.log(`${vocabulary}: ${concepts.length} concepts`);
  for (const concept of concepts) {
    const entry = toEntry(concept);
    if (!known.has(entry.definition)) {
      known.add(entry.definition);
      added.push(entry);
    }
  }
}

added.sort((a, b) => a.definition.localeCompare(b.definition));
const merged = [...existing, ...added];

fs.writeFileSync(MAPPING_PATH, JSON.stringify(merged, null, 2).replace(/\n/g, eol) + eol);
console.log(`Added ${added.length} entries, ${merged.length} total`);
