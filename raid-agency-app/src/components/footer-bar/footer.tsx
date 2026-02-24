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
        <footer className="footer-bar" >
            <div className="footer-content">
                <Container disableGutters maxWidth={false} sx={{
                        position: 'fixed',
                        bottom: 0,
                        left: 0,
                        width: '100%',
                        zIndex: 1000,
                    }}>
                    <Stack direction="column" justifyContent="center" alignItems="center" spacing={0} >
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
                        <Paper elevation={0} sx={{ padding: '8px 16px', backgroundColor: 'transparent', width: '100%'  }}>
                            <Stack
                                direction={{ xs: 'column', sm: 'row' }}
                                spacing={2}
                                justifyContent="space-evenly"
                                alignItems="center"
                                useFlexGap
                                flexWrap="wrap"
                                width={"80%"}
                                >
                                {config.footer.links.map((link, index) => (
                                    <Box key={index} sx={{ display: 'inline-flex' }}>
                                        <a key={index} href={link.path} target="_blank" rel="noopener noreferrer" style={{ margin: '0 8px', textDecoration: 'none', color: 'inherit'}}>
                                        <Typography variant="h6">{link.label}</Typography>
                                        </a>
                                    </Box>
                                ))}
                            </Stack>
                        </Paper>
                        <Paper elevation={1} sx={{ padding: ' 20px', width: '100%' }}>
                            <Typography variant="body1" align="center">
                                {config.footer.copyright}
                            </Typography>
                        </Paper>
                    </Stack>
                </Container>
            </div>
        </footer>
    );
};

export default Footer;