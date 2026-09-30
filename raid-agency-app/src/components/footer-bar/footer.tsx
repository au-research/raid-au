import { useContext } from 'react';
import { AppConfigContext } from '../../config/Appconfigcontext';
import { AppConfig } from '../../config/Appconfig';
import { useExternalScript } from '@/hooks/useExternalScript';

const FOOTER_SCRIPT_SRC =
    'https://ardc.edu.au/wp-content/plugins/ardc-custom-web-components/dist/loaders/footer.min.js';

const ARDC_ACN_URL =
    'https://www.acnc.gov.au/charity/charities/eca273f3-f5be-e911-a98a-000d3ad02a61/profile';

const ArdcFooter = ({ config }: { config: AppConfig }) => {
    useExternalScript(FOOTER_SCRIPT_SRC, () => {
        const el = document.querySelector<HTMLElement>('.ardc-footer');
        if (el) el.style.display = 'none';
    });

    const quickLinks = config.footer.links.filter((link) => link.contact);
    const legalLinks = config.footer.links.filter((link) => !link.contact);

    return (
        <ardc-footer
            className="ardc-footer ardc-custom-component"
            data-content-width="normal"
            data-show-ardc-logo={true}
            style={{ opacity: 0 }}
        >
            <ul id="quick-links">
                {quickLinks.map((link, index) => (
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
