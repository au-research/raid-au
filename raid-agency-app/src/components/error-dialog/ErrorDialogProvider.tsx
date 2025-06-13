import { ErrorDialogContext } from "@/components/error-dialog";
import { AlertTitle, Button, Dialog } from "@mui/material";
import React, { PropsWithChildren, useState } from "react";
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import IconButton from '@mui/material/IconButton';
import CloseIcon from '@mui/icons-material/Close';
import { styled } from '@mui/material/styles';

export interface ErrorDialogStateInterface {
  open: boolean;
  content: {
    title: string;
    failures: string[];
  };
  duration?: number;
}

/**
 * Context provider for displaying application-wide error dialogs
 * 
 * Provides methods to show error messages in a standardized dialog format
 * throughout the application using React Context.
 * 
 * @param {React.ReactNode} children - Child components that will have access to error dialog context
 * @returns {JSX.Element} Provider component with dialog UI and context
 */
export const ErrorDialogProvider: React.FC<PropsWithChildren<unknown>> = ({
  children,
}) => {
  const [errorDialogState, setErrorDialogState] =
    useState<ErrorDialogStateInterface>({
      open: false,
      content: {
        failures: [],
        title: "An error occurred",
      },
      duration: 0,
    });

  const openErrorDialog = React.useCallback(
    (
      content: {
        failures: string[];
        title: string;
      },
      duration: number = 3000
    ) => {
      setErrorDialogState({ open: true, content: { title: content.title, failures: content.failures }, duration });
    },
    []
  );

  const closeErrorDialog = React.useCallback(() => {
    setErrorDialogState((prev) => ({ ...prev, open: false }));
  }, []);

  const contextValue = React.useMemo(
    () => ({ openErrorDialog, closeErrorDialog }),
    [openErrorDialog, closeErrorDialog]
  );
  const BootstrapDialog = styled(Dialog)(({ theme }) => ({
  '& .MuiDialogContent-root': {
    padding: theme.spacing(2),
  },
  '& .MuiDialogActions-root': {
    padding: theme.spacing(1),
  },
}));
  return (
    <ErrorDialogContext.Provider value={contextValue}>
      {/* <Dialog onClose={closeErrorDialog} open={errorDialogState.open}>
        <Alert variant="outlined" severity="error">
          <AlertTitle>{errorDialogState.content.title}</AlertTitle>
          <ul>
            {errorDialogState.content.failures.map((message, index) => (
              <li key={index}>{message}</li>
            ))}
          </ul>
        </Alert>
      </Dialog> */}
      <BootstrapDialog
        onClose={closeErrorDialog}
        aria-labelledby="customized-dialog-title"
        open={errorDialogState.open}
      >
        <DialogTitle sx={{ m: 0, p: 2 }} id="customized-dialog-title">
          <AlertTitle>{errorDialogState.content.title}</AlertTitle>
        </DialogTitle>
        <IconButton
          aria-label="close"
          onClick={closeErrorDialog}
          sx={(theme) => ({
            position: 'absolute',
            right: 8,
            top: 8,
            color: theme.palette.grey[500],
          })}
        >
          <CloseIcon />
        </IconButton>
        <DialogContent dividers>
          <ul>
            {errorDialogState.content.failures.map((message, index) => (
              <li key={index}>{message}</li>
            ))}
          </ul>
        </DialogContent>
        <DialogActions>
          <Button autoFocus onClick={closeErrorDialog}>
            Ok
          </Button>
        </DialogActions>
      </BootstrapDialog>
      {children}
    </ErrorDialogContext.Provider>
  );
};
