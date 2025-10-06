import { ErrorItem, ErrorMessage } from './types';

/**
 * Transforms raw error data into a standardized ErrorMessage format
 * @param errorData - Object containing field names as keys and ErrorItem(s) as values
 * @returns Standardized ErrorMessage object with title and array of failure messages
 */
export const transformErrorMessage = (errorData: Record<string, ErrorItem | ErrorItem[]>): ErrorMessage => {
  // Initialize the standardized error message structure
  const transformedError: ErrorMessage = {
    title: "There are some validation errors",
    // Default title for the error message
    failures: [],
  };

  // Iterate through each field and its associated error(s)
  Object.entries(errorData).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      // Handle array of errors (like title field)
      // This occurs when a single field has multiple validation errors
      value.forEach((error) => {
        // Extract error message with fallback chain: text.message -> message -> default
        const message = error?.text?.message || error?.message || 'Unknown error';
        // Add formatted error message to failures array
        transformedError.failures.push(`${key} - ${message}`);
      });
    } else {
      // Handle single error object
      // This occurs when a field has only one validation error
      const message = value?.message || 'Unknown error';
      // Add formatted error message to failures array
      transformedError.failures.push(`${key} - ${message}`);
    }
  });

  return transformedError;
};
