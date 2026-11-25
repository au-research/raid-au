import { Fragment, ReactNode, useEffect } from 'react';
import { Box, Link, Typography, useTheme } from '@mui/material';
import { NavLink, useBranding, useRuntimeConfig } from '@/config';
import { useExternalScript } from '@/hooks/useExternalScript';
import { EXTERNAL_LINKS } from '@/constants/external-links';

// Per the mock's full Quick Links list - these are fixed, external
// destinations (same as the header nav links), not config-driven like
// the legal links below.
const QUICK_LINKS = [
    { label: 'About RAiD', path: EXTERNAL_LINKS.ABOUT_RAID },
    { label: 'Visit raid.org', path: EXTERNAL_LINKS.RAID_ORG },
    { label: 'RAiD User Guides', path: EXTERNAL_LINKS.DOCUMENTATION },
    { label: 'RAiD Schema Documentation', path: EXTERNAL_LINKS.SCHEMA_DOCUMENTATION },
    { label: 'Request Support', path: EXTERNAL_LINKS.REQUEST_SUPPORT },
    { label: 'Contact the ARDC', path: EXTERNAL_LINKS.CONTACT_ARDC },
];

const FOOTER_SCRIPT_SRC =
    'https://ardc.edu.au/wp-content/plugins/ardc-custom-web-components/dist/loaders/footer.min.js';

const ARDC_ACN_URL =
    'https://www.acnc.gov.au/charity/charities/eca273f3-f5be-e911-a98a-000d3ad02a61/profile';

const ArdcFooter = ({ links }: { links: NavLink[] }) => {
    useExternalScript(FOOTER_SCRIPT_SRC, () => {
        const el = document.querySelector<HTMLElement>('ardc-footer');
        if (el) el.style.display = 'none';
    });

    // The footer's own inner wrapper (inside its shadow root) needs extra
    // top padding; that's inside the ARDC-hosted component's shadow DOM, so
    // it can't be reached with a normal stylesheet selector. Its content
    // renders asynchronously once footer.min.js finishes its own init, so
    // this polls briefly rather than trying once on mount. Selecting by
    // tag name, not the "ardc-footer" class - the component's own script
    // strips the light-DOM class attribute once it upgrades the element.
    useEffect(() => {
        let attempts = 0;
        const applyPadding = () => {
            const host = document.querySelector<HTMLElement>('ardc-footer');
            const inner = host?.shadowRoot?.querySelector<HTMLElement>('.ardc-footer__footer');
            if (inner) {
                inner.style.paddingTop = '30px';
                return true;
            }
            return false;
        };
        if (applyPadding()) return;
        const interval = setInterval(() => {
            attempts += 1;
            if (applyPadding() || attempts > 40) {
                clearInterval(interval);
            }
        }, 250);
        return () => clearInterval(interval);
    }, []);

    return (
        <ardc-footer
            className="ardc-footer ardc-custom-component"
            data-content-width="normal"
            data-show-ardc-logo={true}
            style={{ opacity: 0 }}
        >
            <ul id="quick-links">
                {QUICK_LINKS.map((link, index) => (
                    <li key={index}><a href={link.path}>{link.label}</a></li>
                ))}
            </ul>
            <ul id="legal-links">
                {links.map((link, index) => (
                    <li key={index}><a href={link.path}>{link.label}</a></li>
                ))}
            </ul>
            <a id="ardc-footer-acn" href={ARDC_ACN_URL}>ACN 633 798 857</a>
        </ardc-footer>
    );
};

// Contact entries (mailto addresses flagged with contact: true) are shown by
// the ARDC footer's own contact area, not in the legal links.
const withoutContactEntries = (links: NavLink[]) => links.filter((link) => !link.contact);

const textColor = { light: 'text.secondary', dark: 'grey.400' } as const;

const Separator = () => (
    <Box
        component="li"
        aria-hidden="true"
        data-testid="footer-separator"
        sx={{ width: '1px', height: 14, bgcolor: 'currentColor', opacity: 0.35, listStyle: 'none' }}
    />
);

// Simple footer for agencies without ARDC branding: the configured links, the
// helpdesk address and, when set, the contact address, on one centred line with
// a separator between each. It has the header's background, names no agency and
// loads no external script.
const GenericFooter = ({ links }: { links: NavLink[] }) => {
    const theme = useTheme();
    const { supportEmail, contactEmail } = useRuntimeConfig();
    const isDark = theme.palette.mode === 'dark';

    const items: ReactNode[] = [
        ...links.map((link) => (
            <Link
                key={`${link.label}|${link.path}`}
                href={link.path}
                color="inherit"
                underline="hover"
                {...(link.path.startsWith('https:')
                    ? { target: '_blank', rel: 'noopener noreferrer' }
                    : {})}
            >
                {link.label}
            </Link>
        )),
        <span key="support">
            Support:{' '}
            <Link href={`mailto:${supportEmail}`} color="inherit" underline="always">
                {supportEmail}
            </Link>
        </span>,
    ];
    if (contactEmail) {
        items.push(
            <span key="contact">
                Contact:{' '}
                <Link href={`mailto:${contactEmail}`} color="inherit" underline="always">
                    {contactEmail}
                </Link>
            </span>
        );
    }

    return (
        <Box
            component="footer"
            sx={{
                // The same background as the header (see AppNavBar).
                bgcolor: isDark ? 'black' : 'white',
                color: isDark ? textColor.dark : textColor.light,
                borderTop: 1,
                borderColor: isDark ? 'grey.800' : 'divider',
                py: 2,
                px: 2,
            }}
        >
            <Typography
                variant="body2"
                component="ul"
                sx={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    justifyContent: 'center',
                    alignItems: 'center',
                    columnGap: 2,
                    rowGap: 1,
                    listStyle: 'none',
                    m: 0,
                    p: 0,
                }}
            >
                {items.map((item, index) => (
                    <Fragment key={index}>
                        {index > 0 && <Separator />}
                        <li>{item}</li>
                    </Fragment>
                ))}
            </Typography>
        </Box>
    );
};

export const Footer = () => {
    const { variant, links } = useBranding().footer;
    const shown = withoutContactEntries(links);

    // The ARDC variant is the only one that loads an ARDC script.
    if (variant === 'ardc') {
        return <ArdcFooter links={shown} />;
    }

    return <GenericFooter links={shown} />;
};

export default Footer;
