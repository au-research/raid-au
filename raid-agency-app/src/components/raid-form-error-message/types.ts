/**
 * Represents a validation failure returned from the API
 * 
 * This type defines the structure of validation error messages
 * that are returned by the backend API when field validation fails.
 * It's used to parse and display specific validation errors to users.
 * 
 * @property {string} fieldId - The identifier of the field that failed validation
 * @property {string} errorType - The type of validation error (e.g., "required", "pattern")
 * @property {string} message - Human-readable error message for display
 */
export type Failure = {
  fieldId: string;
  errorType: string;
  message: string;
};

export interface ErrorMessage {
  title: string;
  failures: string[];
}

export interface ParsedErrorMessage {
  title?: string;
  failures?: Failure[];
}

export interface ErrorItem {
  message?: string;
  text?: { message?: string };
}
