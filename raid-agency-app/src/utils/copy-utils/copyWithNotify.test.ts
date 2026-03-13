import { describe, it, expect, vi, beforeEach } from "vitest";
import { copyToClipboardWithNotification } from "./copyWithNotify";

// Define a mock snackbar type matching the function's expected interface
interface MockSnackbar {
  openSnackbar: (msg: string, duration?: number, severity?: string) => void;
}

describe("copyToClipboardWithNotification", () => {
  let mockSnackbar: MockSnackbar;

  beforeEach(() => {
    vi.resetAllMocks();

    // Create a mock snackbar
    mockSnackbar = {
      openSnackbar: vi.fn(),
    };

    // Mock navigator.clipboard.writeText
    Object.defineProperty(navigator, "clipboard", {
      value: {
        writeText: vi.fn(),
      },
      writable: true,
    });
  });

  it("SUCCESS: should copy data to clipboard and show success snackbar", async () => {
    const testData = "Test data";
    const successMessage = "✅ Copied!";
    (navigator.clipboard.writeText as ReturnType<typeof vi.fn>).mockResolvedValueOnce(undefined);

    await copyToClipboardWithNotification(testData, successMessage, mockSnackbar);

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith(testData);
    expect(mockSnackbar.openSnackbar).toHaveBeenCalledWith(successMessage, 3000, "success");
  });

  it("FAILURE: should show error snackbar if clipboard write fails", async () => {
    const testError = new Error("Copy to Clipboard failed");
    (navigator.clipboard.writeText as ReturnType<typeof vi.fn>).mockRejectedValueOnce(testError);
    const consoleErrorSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    await copyToClipboardWithNotification("data", "Copying...", mockSnackbar);

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith("data");
    expect(mockSnackbar.openSnackbar).toHaveBeenCalledWith("Failed to copy data to clipboard", 3000, "error");
    expect(consoleErrorSpy).toHaveBeenCalledWith("Failed to copy to clipboard:", testError);

    consoleErrorSpy.mockRestore();
  });
});
