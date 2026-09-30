import { useEffect } from "react";
import { useExternalScript } from "@/hooks/useExternalScript";

const MEGAMENU_SCRIPT_SRC =
  "https://ardc.edu.au/wp-content/plugins/ardc-custom-web-components/dist/loaders/megamenu.min.js";

export const MegaMenu = () => {
    useExternalScript(MEGAMENU_SCRIPT_SRC, () => {
        const el = document.getElementById("ardc-menu");
        if (el) el.style.display = "none";
    });

    // The trigger bar's own inner wrapper (inside its shadow root) needs
    // extra right padding so the button isn't flush against the true
    // viewport edge; that's inside the ARDC-hosted component's shadow
    // DOM, so it can't be reached with a normal stylesheet selector. Its
    // content renders asynchronously, so this polls briefly rather than
    // assuming it exists on mount.
    useEffect(() => {
        let attempts = 0;
        const applyPadding = () => {
            const host = document.getElementById("ardc-menu");
            const inner = host?.shadowRoot?.querySelector<HTMLElement>(".ardc-header__top-bar-inner");
            if (inner) {
                inner.style.paddingRight = "24px";
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
        <ardc-megamenu
            id="ardc-menu"
            data-host="RAiD App"
            data-content-width="full"
            style={{ display: "block", opacity: 0, minHeight: "20px" }}
        />
    );
}
