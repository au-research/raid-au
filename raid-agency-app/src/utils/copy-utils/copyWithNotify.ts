import { SnackbarContextInterface } from "@/components/snackbar";

/**
 * Copy to Clipboard with Notification Module
 *
 * This module provides utility function to copy to Clipboard and show a notification
 * message using a snackbar. It handles the clipboard operation and displays 
 * a success(✅) popup/snackbar or Failure(❌) popup/snackbar in case
 * of error. 
 */
export const copyToClipboardWithNotification = async (
    data: string,
    message: string,
    snackbar: { openSnackbar: (msg: string, duration?: number, severity?: string) => void } | SnackbarContextInterface
) => {
    try {
      await navigator.clipboard.writeText(data);
     snackbar.openSnackbar(message, 3000, "success");
    } catch (error) {
      console.error("Failed to copy to clipboard:", error);
      snackbar.openSnackbar("Failed to copy data to clipboard", 3000, "error");
    }
};
