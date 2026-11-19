import { CONFIG_FIELD_RULES, checkField, isSafeLink } from "./ConfigValidation";

type PlainObject = Record<string, unknown>;

/**
 * The branding sections of app-config.json, with every unusable value removed.
 * A type alias, not an interface, so it can be passed to resolveBranding().
 */
export type RawBranding = {
  theme: PlainObject;
  header: PlainObject;
  footer: PlainObject;
};

const SECTIONS = ["theme", "header", "footer"] as const;
type Section = (typeof SECTIONS)[number];

// The optional rules that sit inside a section, for example header.title or
// footer.links. Parents come before their children, so a bad header.logo is
// removed before header.logo.light is looked at.
const SECTION_FIELD_RULES = CONFIG_FIELD_RULES.filter((rule) => {
  const [first] = rule.path.split(".");
  return rule.optional && rule.path.includes(".") && SECTIONS.includes(first as Section);
});

function isPlainObject(value: unknown): value is PlainObject {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function isBlank(value: unknown): boolean {
  return typeof value === "string" && value.trim() === "";
}

// app-config.json is parsed JSON, so a JSON round trip is a safe deep copy.
function copySection(value: unknown): PlainObject {
  return isPlainObject(value) ? (JSON.parse(JSON.stringify(value)) as PlainObject) : {};
}

function containerOf(section: PlainObject, keys: string[]): PlainObject | undefined {
  let node: unknown = section;
  for (const key of keys) {
    if (!isPlainObject(node)) return undefined;
    node = node[key];
  }
  return isPlainObject(node) ? node : undefined;
}

/**
 * Takes the `theme`, `header` and `footer` sections from the raw app-config.json
 * and removes every value the app must not use, so the resolver falls back to
 * the neutral value for that field (RAID-1065).
 *
 * - A value of the wrong type, an unknown `header.menu` or `footer.variant`, a
 *   blank text, or a null is removed.
 * - In a link list, each link that is not a safe https or mailto link is
 *   removed and the safe ones stay. A list that is not a list is removed.
 * - A section that is not an object becomes empty.
 *
 * validateConfig() reports the same problems as banner text, naming fields only.
 * This function never changes the object it is given.
 */
export function sanitiseBranding(raw: Record<string, unknown>): RawBranding {
  const sections: RawBranding = {
    theme: copySection(raw.theme),
    header: copySection(raw.header),
    footer: copySection(raw.footer),
  };

  for (const rule of SECTION_FIELD_RULES) {
    const [section, ...rest] = rule.path.split(".");
    const key = rest[rest.length - 1];
    const container = containerOf(sections[section as Section], rest.slice(0, -1));
    if (!container) continue;

    const value = container[key];
    if (value === undefined) continue;

    if (rule.check === "links") {
      if (Array.isArray(value)) {
        container[key] = value.filter(isSafeLink);
      } else {
        delete container[key];
      }
      continue;
    }

    if (value === null || isBlank(value) || checkField(value, rule) !== "ok") {
      delete container[key];
    }
  }

  return sections;
}
