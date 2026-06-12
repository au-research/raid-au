import { describe, expect, it } from "vitest";
import { addMissingEndDate, addMissingEndDateInPlace } from "./TransformResponseData";

describe("addMissingEndDate", () => {
  it("adds endDate to objects with startDate but no endDate", () => {
    const input = { startDate: "2024-01-01" };
    const result = addMissingEndDate(input);
    expect(result).toEqual({ startDate: "2024-01-01", endDate: "" });
  });

  it("does not overwrite existing endDate", () => {
    const input = { startDate: "2024-01-01", endDate: "2024-12-31" };
    const result = addMissingEndDate(input);
    expect(result).toEqual({ startDate: "2024-01-01", endDate: "2024-12-31" });
  });

  it("does not add endDate to objects without startDate", () => {
    const input = { name: "test" };
    const result = addMissingEndDate(input);
    expect(result).toEqual({ name: "test" });
  });

  it("processes nested objects recursively", () => {
    const input = {
      date: { startDate: "2024-01-01" },
      other: "value",
    };
    const result = addMissingEndDate(input) as Record<string, unknown>;
    expect(result.date).toEqual({ startDate: "2024-01-01", endDate: "" });
    expect(result.other).toBe("value");
  });

  it("processes arrays of objects", () => {
    const input = [
      { startDate: "2024-01-01" },
      { startDate: "2024-06-01", endDate: "2024-07-01" },
    ];
    const result = addMissingEndDate(input);
    expect(result).toEqual([
      { startDate: "2024-01-01", endDate: "" },
      { startDate: "2024-06-01", endDate: "2024-07-01" },
    ]);
  });

  it("does not mutate the original object", () => {
    const input = { startDate: "2024-01-01" };
    addMissingEndDate(input);
    expect(input).toEqual({ startDate: "2024-01-01" });
    expect(input).not.toHaveProperty("endDate");
  });

  it("handles null and undefined", () => {
    expect(addMissingEndDate(null)).toBeNull();
    expect(addMissingEndDate(undefined)).toBeUndefined();
  });

  it("returns primitive values unchanged", () => {
    expect(addMissingEndDate("string")).toBe("string");
    expect(addMissingEndDate(42)).toBe(42);
    expect(addMissingEndDate(true)).toBe(true);
  });
});

describe("addMissingEndDateInPlace", () => {
  it("mutates the original object", () => {
    const input = { startDate: "2024-01-01" } as Record<string, unknown>;
    addMissingEndDateInPlace(input);
    expect(input.endDate).toBe("");
  });

  it("processes nested objects in place", () => {
    const nested = { startDate: "2024-01-01" } as Record<string, unknown>;
    const input = { date: nested };
    addMissingEndDateInPlace(input);
    expect(nested.endDate).toBe("");
  });
});
