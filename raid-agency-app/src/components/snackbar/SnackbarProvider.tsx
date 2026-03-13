import { SnackbarContext } from "@/components/snackbar";
import { Snackbar } from "@mui/material";
import Alert from '@mui/material/Alert';
import React, { PropsWithChildren, useState } from "react";

interface SnackbarStateInterface {
  open: boolean;
  content: string;
  duration?: number;
  severity?: "success" | "error" | "info";
}

/**
 * Context provider for displaying toast notifications
 * 
 * Provides methods to show temporary notifications with customizable duration
 * throughout the application using React Context.
 * 
 * @param {React.ReactNode} children - Child components that will have access to snackbar context
 * @returns {JSX.Element} Provider component with snackbar UI and context
 */
export const SnackbarProvider: React.FC<PropsWithChildren<unknown>> = ({
  children,
}) => {
  const [snackbarState, setSnackbarState] = useState<SnackbarStateInterface>({
    open: false,
    content: "",
    duration: 3000,
    severity: "info",
  });

  const openSnackbar = React.useCallback(
    (content: string, duration: number = 3000, severity: "success" | "error" | "info" = "info") => {
      setSnackbarState({ open: true, content, duration, severity });
    },
    []
  );

  const closeSnackbar = React.useCallback(() => {
    setSnackbarState((prev) => ({ ...prev, open: false }));
  }, []);

  const contextValue = React.useMemo(
    () => ({ openSnackbar, closeSnackbar }),
    [openSnackbar, closeSnackbar]
  );

  return (
    <SnackbarContext.Provider value={contextValue}>
      <Snackbar
        open={snackbarState.open}
        autoHideDuration={snackbarState.duration}
        onClose={closeSnackbar}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert
          onClose={closeSnackbar}
          severity={snackbarState.severity || "info"}
          variant="filled"
          sx={{ width: 'auto' }}
        >
          {snackbarState.content}
        </Alert>
      </Snackbar>
      {children}
    </SnackbarContext.Provider>
  );
};
