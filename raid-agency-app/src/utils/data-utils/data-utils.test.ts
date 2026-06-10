import { describe, expect, it } from "vitest";
import { raidCreateRequest, raidRequest } from "@/utils/data-utils/data-utils";
import { Id, RaidDto } from "@/generated/raid";

describe("raidRequest", () => {
  it("defaults the identifier to an empty object when missing", () => {
    const result = raidRequest({} as RaidDto);
    expect(result.identifier).toEqual({});
  });

  it("preserves an existing identifier", () => {
    const identifier = { id: "https://raid.org/10.1/abc" } as Id;
    const result = raidRequest({ identifier } as RaidDto);
    expect(result.identifier).toBe(identifier);
  });
});

describe("raidCreateRequest", () => {
  it("omits the identifier so the API can generate it", () => {
    const result = raidCreateRequest({} as RaidDto);
    expect(result).not.toHaveProperty("identifier");
  });

  it("omits the identifier even when the form data carries one", () => {
    const identifier = { id: "https://raid.org/10.1/abc" } as Id;
    const result = raidCreateRequest({ identifier } as RaidDto);
    expect(result).not.toHaveProperty("identifier");
  });

  it("keeps the remaining raid data", () => {
    const data = {
      title: [{ text: "A title" }],
      date: { startDate: "2024-01-15" },
    } as RaidDto;
    const result = raidCreateRequest(data);
    expect(result.title).toEqual([{ text: "A title" }]);
    expect(result.date).toEqual({ startDate: "2024-01-15" });
    expect(result.contributor).toEqual([]);
  });
});
