import { describe, expect, it } from "vitest";
import { getSubjectDisplay } from "./subject-label";

const FOR = "https://linked.data.gov.au/def/anzsrc-for/2020/";
const SEO = "https://linked.data.gov.au/def/anzsrc-seo/2020/";

describe("getSubjectDisplay", () => {
  // RAID-1009: division 42 rendered as "undefined (undefined)"
  it("shows the label and code for a FoR division", () => {
    expect(getSubjectDisplay(`${FOR}42`)).toEqual({
      label: "Health Sciences",
      code: "42",
      text: "Health Sciences (42)",
    });
  });

  it("shows the label and code for a FoR group", () => {
    expect(getSubjectDisplay(`${FOR}3702`).text).toBe("Climate change science (3702)");
  });

  it("shows the label and code for a FoR field", () => {
    expect(getSubjectDisplay(`${FOR}300101`).text).toBe(
      "Agricultural biotechnology diagnostics (incl. biosensors) (300101)"
    );
  });

  it("shows the label and code for an SEO objective", () => {
    expect(getSubjectDisplay(`${SEO}269903`).text).toBe(
      "Plant product traceability and quality assurance (excl. forest products) (269903)"
    );
  });

  it("title-cases all-capital division labels but keeps small words lowercase", () => {
    expect(getSubjectDisplay(`${FOR}30`).label).toBe("Agricultural, Veterinary and Food Sciences");
    expect(getSubjectDisplay(`${SEO}25`).label).toBe("Mineral Resources (Excl. Energy Resources)");
  });

  it("leaves mixed-case labels unchanged", () => {
    expect(getSubjectDisplay(`${FOR}4206`).label).toBe("Public health");
  });

  it("falls back to the code, never undefined, for an id outside the mapping", () => {
    const result = getSubjectDisplay(`${FOR}999999`);
    expect(result).toEqual({ label: undefined, code: "999999", text: "999999" });
    expect(result.text).not.toContain("undefined");
  });

  it("does not render undefined for a missing id", () => {
    expect(getSubjectDisplay(undefined).text).toBe("---");
  });

  it("never produces 'undefined' for any mapped entry", async () => {
    const { default: mapping } = await import("@/mapping/data/subject-mapping.json");
    for (const entry of mapping) {
      expect(getSubjectDisplay(entry.definition).text, entry.definition).not.toContain("undefined");
    }
  });
});
