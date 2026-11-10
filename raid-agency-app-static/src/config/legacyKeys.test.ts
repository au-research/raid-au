import { describe, expect, it } from "vitest";
import { findLegacyKeys } from "./legacyKeys";

describe("findLegacyKeys", () => {
  it("reports the old raidUrl key with its replacement", () => {
    expect(findLegacyKeys({ raidUrl: "https://raid.surf.nl" })).toEqual([
      { oldKey: "raidUrl", newKey: "raidResolverUrl" },
    ]);
  });

  it("reports nothing when only the new key is present", () => {
    expect(findLegacyKeys({ raidResolverUrl: "https://raid.org/" })).toEqual([]);
  });

  it("reports nothing for an empty config", () => {
    expect(findLegacyKeys({})).toEqual([]);
  });

  it("still reports the old key when both keys are present", () => {
    expect(
      findLegacyKeys({
        raidUrl: "https://raid.surf.nl",
        raidResolverUrl: "https://raid.org/",
      })
    ).toEqual([{ oldKey: "raidUrl", newKey: "raidResolverUrl" }]);
  });
});
