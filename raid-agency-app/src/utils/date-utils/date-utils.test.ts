import { describe, expect, it, test } from "vitest";
import {
  addDays,
  dateDisplayFormatter,
  parseJwtDate,
} from "@/utils/date-utils/date-utils";

describe("parseJwtDate", () => {
  it("should return a Date object for valid numeric string input", () => {
    const timestamp = "1633072800"; // Example Unix timestamp
    const result = parseJwtDate(timestamp);
    expect(result).toBeInstanceOf(Date);
    expect(result?.getTime()).toBe(1633072800000);
  });

  it("should return a Date object for valid number input", () => {
    const timestamp = 1633072800; // Example Unix timestamp
    const result = parseJwtDate(timestamp);
    expect(result).toBeInstanceOf(Date);
    expect(result?.getTime()).toBe(1633072800000);
  });

  it("should return undefined for invalid string input", () => {
    const invalidDate = "invalid-date";
    const result = parseJwtDate(invalidDate);
    expect(result).toBeUndefined();
  });

  it("should return undefined for undefined input", () => {
    const result = parseJwtDate(undefined);
    expect(result).toBeUndefined();
  });
});

describe("addDays", () => {
  it("should add days to a specific date", () => {
    const baseDate = new Date("2024-01-01");
    const daysToAdd = 10;
    const result = addDays(baseDate, daysToAdd);
    const expectedDate = new Date("2024-01-11");
    expect(result.toISOString()).toBe(expectedDate.toISOString());
  });

  it("should use the current date if date is undefined", () => {
    const daysToAdd = 5;
    const result = addDays(undefined, daysToAdd);
    const expectedDate = new Date();
    expectedDate.setDate(expectedDate.getDate() + daysToAdd);
    expect(result.toISOString().split("T")[0]).toBe(
      expectedDate.toISOString().split("T")[0]
    );
  });

  it("should correctly subtract days when given a negative number", () => {
    const baseDate = new Date("2024-01-10");
    const daysToSubtract = -5;
    const result = addDays(baseDate, daysToSubtract);
    const expectedDate = new Date("2024-01-05");
    expect(result.toISOString()).toBe(expectedDate.toISOString());
  });

  it("should return the same date when adding zero days", () => {
    const baseDate = new Date("2024-01-10");
    const result = addDays(baseDate, 0);
    expect(result.toISOString()).toBe(baseDate.toISOString());
  });
});

test("should return a full year", () => {
  const input = "2023";
  expect(dateDisplayFormatter(input)).toBe("2023");
});

test("should return a year and month in format YYYY-MM", () => {
  const input = "2023-05";
  expect(dateDisplayFormatter(input)).toBe("2023-05");
});

test("should return a year, month and day in format YYYY-MM-DD", () => {
  const input = "2023-05-23";
  expect(dateDisplayFormatter(input)).toBe("2023-05-23");
});

test("should return '---', if the year, month and day are not in format YYYY-MM-DD", () => {
  const input = "23-05-2023";
  expect(dateDisplayFormatter(input)).toBe("---");
});

test("should return '---', if the input is not valid date", () => {
  const input = "abcd";
  expect(dateDisplayFormatter(input)).toBe("---");
});
