import { useContext } from 'react';
import { AppConfigContext } from '../../config/Appconfigcontext';
import Container from '@mui/material/Container';
import { Paper, Stack, Box, Typography } from '@mui/material';


export const Footer = () => {
    const config = useContext(AppConfigContext);

    if (!config) {
        return null;
    }

    return (
        <footer className="footer-bar">
            <div className="footer-content">
                <Container disableGutters maxWidth={false}>
                    <Stack direction="column" justifyContent="center" alignItems="center" spacing={1}>
                        <Paper variant="outlined" sx={{ padding: '8px 16px', width: '100%' }}>
                            <Stack
                                direction={{ xs: 'column', sm: 'row' }}
                                spacing={0}
                                justifyContent="center"
                                alignItems="center"
                                useFlexGap
                                flexWrap="wrap"
                                >
                                {config.footer.main.logos.map((logo, index) => (
                                    <Box key={index} sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                                    <a href={logo.link} target="_blank" rel="noopener noreferrer" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                        <img src={logo.src} alt={logo.alt} style={{height: 'auto', width: '50%' }} />
                                    </a>
                                    </Box>
                                ))}
                            </Stack>
                            <Stack direction="column" justifyContent="center" alignItems="center" spacing={1} sx={{ marginTop: 2 }}>
                                <Box width={"60%"} textAlign="center" padding={4}>
                                    <Typography variant="body1">{config.footer.main.text}</Typography>
                                </Box>
                            </Stack>
                        </Paper>
                        <Paper variant="outlined" sx={{ padding: '8px 16px' }}>
                            contact details, copyrights etc
                        </Paper>
                        <Paper variant="outlined" sx={{ padding: '8px 16px' }}>
                            acknowledgement of country
                        </Paper>
                    </Stack>
                </Container>
            </div>
        </footer>
    );
};

export default Footer;