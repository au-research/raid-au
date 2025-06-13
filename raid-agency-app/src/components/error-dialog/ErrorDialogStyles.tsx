import { styled } from '@mui/material/styles';
import Dialog from '@mui/material/Dialog';

export const BootstrapDialog = styled(Dialog)(({ theme }) => ({
  '& .MuiDialogContent-root': {
    padding: theme.spacing(2),
  },
  '& .MuiDialogActions-root': {
    padding: theme.spacing(1),
    justifyContent: 'center',
  },
  '& .MuiDialogTitle-root': {
    padding: theme.spacing(0),
    margin: 0,
  },
  '& .MuiAlert-message': {
    fontSize: theme.typography.body1.fontSize,
  },
  '& .MuiButton-root': {
    borderRadius: "24px",
    color: theme.palette.grey[100],
    backgroundColor: theme.palette.error.main, // MUI error color
    border: `1px solid ${theme.palette.error.main}`,
    textTransform: "capitalize",
    '&:hover': {
      backgroundColor: "#fff", // Darker shade on hover
      color: theme.palette.error.main, // Lighter text color on hover
      border: `1px solid ${theme.palette.error.main}`, // Lighter border color on hover
    },
  },
}));
