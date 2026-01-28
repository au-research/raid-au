import * as React from 'react';
import Button from '@mui/material/Button';
import { styled } from '@mui/material/styles';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import IconButton from '@mui/material/IconButton';
import CloseIcon from '@mui/icons-material/Close';
import Typography from '@mui/material/Typography';

const BootstrapDialog = styled(Dialog)(({ theme }) => ({
  '& .MuiDialogContent-root': {
    padding: theme.spacing(2),
  },
  '& .MuiDialogActions-root': {
    padding: theme.spacing(1),
  },
  '& .Cancel': {
    marginRight: theme.spacing(1),
    ':hover': {
        backgroundColor: "#fff",
        color: theme.palette.primary.main,  // ✅ Use theme.palette
        border: `1px solid ${theme.palette.primary.main}`,
    },
  },
  '& .Yes': {
    marginRight: theme.spacing(1),
    ':hover': {
        backgroundColor: "#fff",
        color: theme.palette.error.main,  // ✅ Use theme.palette
        border: `1px solid ${theme.palette.error.main}`,
    },
  },
}));

export default function CustomizedDialogs({
  modalTitle,
  modalContent,
  alertOpen,
  onClose,
  modalAction,
  modalActions,
}: {
  modalTitle: string;
  modalContent: string;
  alertOpen: boolean;
  onClose?: () => void;
  modalAction?: boolean;
  modalActions?: {
    label: string;
    onClick: () => void;
    icon: React.ElementType;
    bgColor: string;
  }[];
}) {

  return (
    <React.Fragment>
      <BootstrapDialog
        onClose={onClose}
        aria-labelledby="customized-dialog-title"
        open={alertOpen}
      >
        <DialogTitle sx={{ m: 0, p: 2 }} id="customized-dialog-title">
          {modalTitle}
        </DialogTitle>
        <IconButton
          aria-label="close"
          onClick={onClose}
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
          <Typography gutterBottom>
            {modalContent}
          </Typography>
        </DialogContent>
        <DialogActions>
          {modalAction && modalActions?.map((action, index) => (
            <Button
              key={index}
              autoFocus
              onClick={action.onClick}
              variant='outlined'
              sx={{ backgroundColor: action.bgColor, color: '#fff' }}
              className={action.label}
            >
              <action.icon style={{ marginRight: '8px' }} />
              {action.label}
            </Button>))}
          </DialogActions>
      </BootstrapDialog>
    </React.Fragment>
  );
}