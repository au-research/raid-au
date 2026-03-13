/**
 * File Utility Module
 *
 * This module provides helper functions for file operations such as
 * downloading data as files, file format conversions, and other file-related utilities.
 */
import { SnackbarContextInterface } from "@/components/snackbar";

/**
 * Downloads the provided data as a JSON file
 *
 * This function creates a temporary download link and triggers a download
 * of the provided data as a formatted JSON file. It also shows a success
 * notification using the provided snackbar context.
 *
 * @param data - The data object to be downloaded as JSON
 * @param filename - Optional custom filename (defaults to "data.json")
 * @param snackbar - Snackbar context used to display success notification
 * @param prefix - Optional prefix to add to the filename
 * @param suffix - Optional suffix to add to the filename
 * @param labelPlural - Optional label describing the data type in plural form
 * @returns void
 */
export const downloadJson = ({
  data,
  filename = "data.json",
  snackbar,
  prefix,
  suffix,
  labelPlural,
}: {
  data: any; // The data to be converted to JSON and downloaded
  filename?: string;
  snackbar: SnackbarContextInterface;
  prefix?: string;
  suffix?: string;
  labelPlural?: string;
}) => {
  // Create a blob with the JSON data
  const blob = new Blob([JSON.stringify(data, null, 2)], {
    type: "application/json",
  });

  // Create a URL for the blob
  const url = window.URL.createObjectURL(blob);

  // Create a temporary anchor element
  const link = document.createElement("a");
  link.href = url;
  link.download = `${prefix}-${suffix}-${labelPlural?.toLowerCase()}-${filename}`;

  // Programmatically click the link to trigger download
  document.body.appendChild(link);
  link.click();

  // Clean up
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);

  snackbar.openSnackbar(`Downloaded JSON file`, 3000, 'success');
};
