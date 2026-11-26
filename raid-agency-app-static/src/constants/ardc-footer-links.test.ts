import { describe, expect, it } from "vitest";
import { QUICK_LINKS } from "./ardc-footer-links";

// RAID-1068: the ARDC footer's two contact links went to the wrong addresses.
describe("ARDC footer quick links", () => {
  const pathOf = (label: string) => QUICK_LINKS.find((link) => link.label === label)?.path;

  it("sends Request Support to the ARDC helpdesk", () => {
    expect(pathOf("Request Support")).toBe("mailto:services@ardc.edu.au");
  });

  it("sends Contact the ARDC to the ARDC contact address", () => {
    expect(pathOf("Contact the ARDC")).toBe("mailto:contact@ardc.edu.au");
  });

  it("has no link to contact@raid.org", () => {
    expect(JSON.stringify(QUICK_LINKS)).not.toContain("contact@raid.org");
  });

  it("keeps the other quick links", () => {
    expect(QUICK_LINKS.map((link) => link.label)).toEqual([
      "About RAiD",
      "Visit raid.org",
      "RAiD User Guides",
      "RAiD Schema Documentation",
      "Request Support",
      "Contact the ARDC",
    ]);
  });
});
