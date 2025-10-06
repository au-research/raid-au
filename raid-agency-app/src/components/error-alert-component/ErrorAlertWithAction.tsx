import { Alert, IconButton, Tooltip } from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import { memo } from 'react';

export const ErrorAlertWithAction = memo(({
    message,
    onRetry,
    args
}: {
    message: string;
    onRetry: (args: unknown) => void;
    args: unknown;
}) => {
    return (
        <Alert
            severity="error"
            variant="outlined"
            sx={{
                mt: 1,
                alignItems: 'center',
                '& .MuiAlert-icon': {
                fontSize: '1.25rem'
                },
            }}
            action={
                <Tooltip title="Retry" arrow={true} placement="top">
                <IconButton
                    size="small"
                    onClick={() => onRetry(args)}
                    sx={{
                        color: 'error.main',
                        '&:hover': {
                            bgcolor: 'error.light',
                            color: 'error.contrastText'
                        }
                    }}
                >
                    <RefreshIcon fontSize="small" />
                </IconButton>
                </Tooltip>
            }
        >
            {message}
        </Alert>
    );
});
