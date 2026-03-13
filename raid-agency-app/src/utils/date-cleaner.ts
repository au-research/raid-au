/**
 * Date Cleaner Module
 *
 * This module provides a utility function for cleaning date objects
 * in the RAID application, specifically handling undefined or empty date fields.
 */
import { ModelDate } from "@/generated/raid";

/**
 * Cleans a ModelDate object by properly handling undefined or empty endDate values
 * 
 * This function ensures that:
 * - If the input date is undefined, it returns undefined
 * - If the input date has an endDate that is null or empty, it explicitly sets it to undefined
 * - Otherwise, it returns the date object with its original values
 * 
 * This is necessary because the API expects undefined for missing dates rather than
 * null or empty strings.
 * 
 * @param date - The ModelDate object to clean, which may be undefined
 * @returns A clean ModelDate object with properly formatted fields, or undefined if input is undefined
 */
export function dateCleaner(date?: ModelDate): ModelDate | undefined {
  return (
    date && {
      startDate: date.startDate,
      endDate: date.endDate ?? undefined,
    }
  );
}
