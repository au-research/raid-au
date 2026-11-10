import { describe, expect, it } from "vitest";
import { resolveAppUrl } from "./app-url";

describe("resolveAppUrl", () => {
  describe("when appUrl is set", () => {
    it("uses appUrl regardless of raidEnv", () => {
      for (const raidEnv of ["prod", "demo", "stage", "test", "dev", undefined]) {
        expect(
          resolveAppUrl({ appUrl: "https://app.raid.surf.nl", raidEnv })
        ).toBe("https://app.raid.surf.nl");
      }
    });

    it("strips trailing slashes", () => {
      expect(resolveAppUrl({ appUrl: "https://app.raid.surf.nl//" })).toBe(
        "https://app.raid.surf.nl"
      );
    });

    it("trims surrounding whitespace", () => {
      expect(resolveAppUrl({ appUrl: "  https://app.raid.surf.nl/  " })).toBe(
        "https://app.raid.surf.nl"
      );
    });
  });

  describe("when appUrl is unset", () => {
    it.each(["prod", "demo", "stage", "test"])(
      "maps raidEnv %s to the ARDC app URL",
      (raidEnv) => {
        expect(resolveAppUrl({ raidEnv })).toBe(
          `https://app.${raidEnv}.raid.org.au`
        );
      }
    );

    it("treats a blank appUrl as unset", () => {
      expect(resolveAppUrl({ appUrl: "   ", raidEnv: "prod" })).toBe(
        "https://app.prod.raid.org.au"
      );
    });

    it.each(["dev", "unknown", "", undefined])(
      "falls back to localhost for raidEnv %s",
      (raidEnv) => {
        expect(resolveAppUrl({ raidEnv })).toBe("http://localhost:7080");
      }
    );
  });
});
