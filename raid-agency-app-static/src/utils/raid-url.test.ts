import { describe, expect, it } from "vitest";
import { buildRaidUrl } from "./raid-url";

const id = "10.83338/831b5565";

describe("buildRaidUrl", () => {
  it("joins a base URL that has a trailing slash", () => {
    expect(buildRaidUrl("https://raid.org/", id)).toBe(
      "https://raid.org/10.83338/831b5565"
    );
  });

  it("adds the separator when the base URL has no trailing slash", () => {
    expect(buildRaidUrl("https://raid.surf.nl", id)).toBe(
      "https://raid.surf.nl/10.83338/831b5565"
    );
  });

  it("collapses repeated trailing slashes on the base URL", () => {
    expect(buildRaidUrl("https://raid.org//", id)).toBe(
      "https://raid.org/10.83338/831b5565"
    );
  });

  it("ignores a leading slash on the handle", () => {
    expect(buildRaidUrl("https://raid.org/", `/${id}`)).toBe(
      "https://raid.org/10.83338/831b5565"
    );
  });

  it("trims whitespace around the base URL", () => {
    expect(buildRaidUrl("  https://raid.org  ", id)).toBe(
      "https://raid.org/10.83338/831b5565"
    );
  });

  it("falls back to https://raid.org/ when the base URL is undefined", () => {
    expect(buildRaidUrl(undefined, id)).toBe(
      "https://raid.org/10.83338/831b5565"
    );
  });

  it("falls back to https://raid.org/ when the base URL is blank", () => {
    expect(buildRaidUrl("   ", id)).toBe("https://raid.org/10.83338/831b5565");
  });
});
