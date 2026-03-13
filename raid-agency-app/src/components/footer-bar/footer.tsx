import { useContext } from 'react';
import { useTheme } from '@mui/material/styles';
import { AppConfigContext } from '../../config/Appconfigcontext';
import Container from '@mui/material/Container';
import { Paper, Stack, Box, Typography } from '@mui/material';


export const Footer = () => {
    const config = useContext(AppConfigContext);
    const theme = useTheme();

    if (!config || config.default) {
        return null;
    }

    return (
        <footer className="footer-bar" >
            <div className="footer-content">
                <Container
                    disableGutters
                    maxWidth={false}
                >
                    <Stack direction="column" justifyContent="center" alignItems="center" spacing={0} >
                        <Paper variant="outlined" sx={{ padding: '8px 16px', width: '100%' }}>
                            <Stack
                                direction={{ xs: 'column', sm: 'row' }}
                                spacing={{ xs: 1, md: 3 }}
                                justifyContent={'center'}
                                alignItems="center"
                                useFlexGap
                                flexWrap="wrap"
                                pt={4}
                            >
                                {config.footer.main.logos.map((logo, index) => (
                                    <Box key={index} sx={{ width: 'fit-content' }}>
                                        <a
                                            href={logo.link}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                            style={{ display: 'inline-flex' }}
                                        >
                                            <img
                                                src={logo.src}
                                                alt={logo.alt}
                                                style={{ height: '80px', width: 'auto' }}
                                            />
                                        </a>
                                    </Box>
                                ))}
                                </Stack>
                            <Stack direction="column" justifyContent="center" alignItems="center" spacing={1} sx={{ marginTop: 2 }}>
                                <Box width={"60%"} textAlign="center" sx={{ '@media (max-width: 600px)': { width: '100%' }, padding: { xs: 2, sm: 4 } }}>
                                    <Typography variant="body1" sx={{textWrap: 'pretty'}}>{config.footer.main.text}</Typography>
                                </Box>
                            </Stack>
                        </Paper>
                        <Paper variant="outlined" sx={{ padding: '8px 16px', backgroundColor: theme.palette.background.default, width: '100%'  }}>
                            <Stack
                                direction={{ xs: 'column', sm: 'row' }}
                                spacing={2}
                                justifyContent="center"
                                alignItems="center"
                                useFlexGap
                                flexWrap="wrap"
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
                            <Typography variant="body1" align="center" sx={{textWrap: 'pretty'}}>
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
