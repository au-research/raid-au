import { Failure, ErrorMessage, ParsedErrorMessage } from "./";

/**
 * Utility for processing and displaying form validation errors
 * 
 * This component handles parsing error messages from various sources
 * and displaying them through the application's error dialog system.
 * It supports both API validation errors (with failure objects) and
 * client-side validation errors.
 * 
 * The component attempts to parse the error message as JSON, and then
 * extracts relevant failure information from common error response formats.
 * If parsing fails, it treats the message as a plain string error.
 * 
 * @param {Error} error - The error object containing validation messages
 * @param {Function} openErrorDialog - Function to display errors in UI dialog
 *                   This parameter should ideally be typed with the proper interface
 *                   for the error dialog component
 * @returns {void}
 * @throws {Error} Rethrows the error after displaying it with concatenated messages
 */

export const RaidFormErrorMessage = (
  error: Error,
  openErrorDialog:  (content: { failures: string[]; title: string; }, duration?: number) => void
): ErrorMessage => {
  const message: ErrorMessage = {
    title: "Error",
    failures: [],
  };
  let messageParsed: ParsedErrorMessage | null = null;
  try {
    messageParsed = JSON.parse(error.message);
  } catch (parseError) {
    // If parsing fails, handle the error message as a plain string
    message.failures.push(error.message);
  }

  if (messageParsed) {
    message.title = messageParsed.title || "Error";
    if (messageParsed.failures && Array.isArray(messageParsed.failures)) {
      // Ensure failures is an array before processing
      if (messageParsed.failures.length > 0) {
        messageParsed.failures.forEach((failure: Failure) => {
          // Add validation to ensure failure has required properties
          if (failure.fieldId && failure.message) {
            message.failures.push(`${failure.fieldId}: ${failure.message}`);
          } else {
            message.failures.push('Invalid failure format');
          }
        });
      }
    }
  }
  openErrorDialog(message);
  return message;
};

RaidFormErrorMessage.displayName = "RaidFormErrorMessage";
