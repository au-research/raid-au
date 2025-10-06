import { createContext } from "react";

interface ErrorDialogContextInterface {
  openErrorDialog: (content: { failures: string[]; title: string; }, duration?: number) => void;
  closeErrorDialog: () => void;
}

export const ErrorDialogContext = createContext<
  ErrorDialogContextInterface | undefined
>(undefined);
