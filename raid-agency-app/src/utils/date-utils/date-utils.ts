/**
 * Date Utilities Module
 *
 * This module provides utility functions for working with dates in various formats,
 * particularly focused on handling different date string formats used in RAiD records.
 */
import dayjs from "dayjs";

/**
 * Parses JWT timestamp format to a JavaScript Date
 * 
 * JWT timestamps are typically in seconds since epoch, while JavaScript
 * uses milliseconds since epoch. This function handles the conversion.
 * 
 * @param date - A JWT date timestamp, either as string or number
 * @returns A properly formatted Date object, or undefined if input is invalid
 */
export function parseJwtDate(
  date: string | number | undefined
): Date | undefined {
  if (!date || isNaN(Number(date))) {
    return undefined;
  }
  return new Date(Number(date) * 1000);
}

/**
 * Adds a specified number of days to a date
 * 
 * @param d - The source date to add days to (uses current date if undefined)
 * @param days - Number of days to add (can be negative to subtract days)
 * @returns A new Date object with the days added
 */
export function addDays(d: Date | undefined, days: number): Date {
  const date = d ? new Date(d) : new Date();
  date.setDate(date.getDate() + days);
  return date;
}

/**
 * Pattern for validating various date formats in one regex
 * 
 * This pattern accepts:
 * - Year only: YYYY
 * - Year and month: YYYY-MM
 * - Full date: YYYY-MM-DD (with proper month/day range validation)
 */
export const combinedPattern =
  /^(?:\d{4}-(?:(?:0[13578]|1[02])-(?:0[1-9]|[12]\d|3[01])|(?:0[469]|11)-(?:0[1-9]|[12]\d|30)|02-(?:0[1-9]|1\d|2[0-8]))|\d{4}-(?:0[1-9]|1[0-2])|\d{4})?$/;

/**
 * Pattern for validating year-only format (YYYY)
 */
export const yearPattern = /^\d{4}$/;

/**
 * Pattern for validating year-month format (YYYY-MM)
 */
export const yearMonthPattern = /^\d{4}-\d{2}$/;

/**
 * Pattern for validating full date format (YYYY-MM-DD)
 * with proper month and day range validation
 */
export const yearMonthDayPattern =
  /^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$/;

/**
 * Checks if a string is in year-only format (YYYY)
 * 
 * @param input - The string to test
 * @returns True if the input matches the year-only pattern
 */
const isYearFormattedString = (input: string): boolean => {
  return yearPattern.test(input);
};

/**
 * Checks if a string is in year-month format (YYYY-MM)
 * 
 * @param input - The string to test
 * @returns True if the input matches the year-month pattern
 */
const isYearMonthFormattedString = (input: string): boolean => {
  return yearMonthPattern.test(input);
};

/**
 * Checks if a string is in full date format (YYYY-MM-DD)
 * 
 * @param input - The string to test
 * @returns True if the input matches the full date pattern
 */
const isYearMonthDayFormattedString = (input: string): boolean => {
  return yearMonthDayPattern.test(input);
};

/**
 * Formats date strings for display based on their format
 * 
 * This function intelligently formats dates based on their detected precision:
 * - Year only (YYYY): Displays as is
 * - Year and month (YYYY-MM): Formats as "MMM-YYYY" (e.g., "Jan-2023")
 * - Full date (YYYY-MM-DD): Formats as "DD-MMM-YYYY" (e.g., "01-Jan-2023")
 * 
 * @param input - A date string in various possible formats
 * @returns A formatted date string for display, or "---" if input is invalid
 */
export const dateDisplayFormatter = (input?: string) => {
  if (!input || input === "" || input === undefined) {
    return "---";
  }

  if (isYearFormattedString(input)) {
    return input;
  }

  if (isYearMonthFormattedString(input)) {
    const [year, month] = input.split("-");
    return dayjs(`${year}-${month}-01`).format("MMM-YYYY");
  }

  if (isYearMonthDayFormattedString(input)) {
    const [year, month, day] = input.split("-");
    return dayjs(`${year}-${month}-${day}`).format("DD-MMM-YYYY");
  }

  return "---";
};
