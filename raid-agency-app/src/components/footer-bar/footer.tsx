import { useContext, useEffect } from 'react';
import { AppConfigContext } from '../../config/Appconfigcontext';
import { AppConfig } from '@/config/Appconfig';
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

const ArdcFooter = ({ config }: { config: AppConfig }) => {
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

    const legalLinks = config.footer.links.filter((link) => !link.contact);

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
                {legalLinks.map((link, index) => (
                    <li key={index}><a href={link.path}>{link.label}</a></li>
                ))}
            </ul>
            <a id="ardc-footer-acn" href={ARDC_ACN_URL}>ACN 633 798 857</a>
        </ardc-footer>
    );
};

export const Footer = () => {
    const config = useContext(AppConfigContext);

    if (!config || config.default) {
        return null;
    }

    return <ArdcFooter config={config} />;
};

export default Footer;
