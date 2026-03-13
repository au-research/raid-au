import React from 'react';
import {
  Box,
  Container,
  Typography,
  Paper,
  Card,
  CardContent,
  Chip,
  Button,
  Stack,
} from '@mui/material';
import {
  CheckCircle,
  AccountCircle,
  Security,
  ArrowForward,
} from '@mui/icons-material';
import { green } from '@mui/material/colors';

interface OrcidSuccessPageProps {
  onContinue?: () => void;
  redirectUrl?: string;
}

const OrcidSuccess: React.FC<OrcidSuccessPageProps> = () => {
  return (
    <Box
      sx={{
        minHeight: '52vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        py: 4,
      }}
    >
      <Container maxWidth="md">
        <Card
          elevation={3}
          sx={{
            borderRadius: 2,
            overflow: 'visible',
            position: 'relative',
          }}
        >
          <CardContent sx={{ p: 6, textAlign: 'center' }}>
            {/* Success Icon */}
            <Box sx={{ mb: 3 }}>
              <CheckCircle
                sx={{
                  fontSize: 80,
                  color: green[500],
                  filter: 'drop-shadow(0 2px 4px rgba(76, 175, 80, 0.3))',
                }}
              />
            </Box>

            {/* Success Message */}
            <Typography
              variant="h4"
              component="h1"
              gutterBottom
              sx={{
                fontWeight: 600,
                color: 'text.primary',
                mb: 2,
              }}
            >
              Authentication Successful!
            </Typography>

            <Typography
              variant="h6"
              sx={{
                color: 'text.secondary',
                mb: 4,
                lineHeight: 1.6,
                maxWidth: '600px',
                mx: 'auto',
              }}
            >
              You have successfully authorised RAiD to interact with your ORCID Record as a trusted party
            </Typography>
            <Typography 
                variant="body2" 
                sx={{
                    color: 'text.secondary',
                    mb: 4,
                    lineHeight: 1.6,
                    maxWidth: '600px',
                    mx: 'auto',
                }}
            >
                You can now safely close this tab
            </Typography>
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
};

export default OrcidSuccess;
