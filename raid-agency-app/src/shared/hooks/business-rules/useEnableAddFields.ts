/**
 * Custom hook to determine whether the "Add Fields" button should be disabled
 * 
 * BUSINESS RULE: End Date Completion Requirement
 * Users must specify a valid end date for the current field before they can add a new field.
 * This ensures data completeness and maintains chronological integrity of field entries.
 * 
 * The button is disabled (returns true) when:
 * - The endDate in the last field is missing or invalid
 * 
 * The button is enabled (returns false) when:
 * - There are no existing subsections with fields
 * - The endDate in the last field is valid and properly formatted (YYYY-MM or YYYY-MM-DD)
 *
 * @param subSection - Array of subsections, each containing an array of fields
 * @param isDirty - Boolean indicating if the form has unsaved changes
 * @returns boolean indicating whether the "Add Fields" button should be disabled
 */

import { useMemo } from 'react';

interface SubSection {
    endDate?: string | null | undefined | Date;
    [key: string]: unknown;
}

export type SubSections = SubSection[][];

export const useEnableAddFields = (subSection: SubSections, isDirty: boolean): boolean | undefined => {
    return useMemo(() => {
        const lastField = subSection[subSection.length - 1];
         // Enable if no fields in the last section
        if (!lastField?.length) return false;

        const lastSubSections = lastField[lastField.length - 1];

        // If endDate is missing, disable the button
        if (!isDirty && !lastSubSections?.endDate) return true;

        if (!isDirty || !subSection.length) return false;
        const regex = /^\d{4}-(0[1-9]|1[0-2])(-([0-2]\d|3[01]))?$/; // YYYY-MM-DD format
        // If endDate is not a string, disable the button
        const endDateStr = lastSubSections.endDate as string;

        // If format is invalid, disable the button
        if (!regex.test(endDateStr)) return true;

        // Check if it's actually a valid date
        const [year, month] = endDateStr.split('-').map(Number);
        const date = new Date(year, month - 1, 1);

        // If date is invalid, disable the button
        if (date.getFullYear() !== year ||
            date.getMonth() !== month - 1) {
            return true;
        }

        // All validations passed, enable the button
        return false;
    }, [subSection, isDirty]);
}
