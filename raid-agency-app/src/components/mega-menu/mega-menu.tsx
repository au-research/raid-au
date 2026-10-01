import { useEffect } from "react";
import { Box } from "@mui/material";
import { useExternalScript } from "@/hooks/useExternalScript";

const MEGAMENU_SCRIPT_SRC =
  "https://ardc.edu.au/wp-content/plugins/ardc-custom-web-components/dist/loaders/megamenu.min.js";

export const MegaMenu = () => {
    useExternalScript(MEGAMENU_SCRIPT_SRC, () => {
        const el = document.getElementById("ardc-menu");
        if (el) el.style.display = "none";
    });

    // The trigger button itself (inside the component's shadow root) is
    // position:absolute; right:0, so it's flush against the true viewport
    // edge regardless of any padding set on its parents - padding only
    // affects normal-flow children, not absolutely-positioned ones. Its
    // own `right` is what needs overriding, and that content renders
    // asynchronously, so this polls briefly rather than assuming it
    // exists on mount.
    useEffect(() => {
        let attempts = 0;
        const applyInset = () => {
            const host = document.getElementById("ardc-menu");
            const btn = host?.shadowRoot?.querySelector<HTMLElement>(".ardc-header__explore-btn");
            if (btn) {
                // The component's own shadow stylesheet sets `right` with
                // !important, so a plain inline-style assignment loses -
                // setProperty with "important" priority is required to win.
                btn.style.setProperty("right", "24px", "important");
                return true;
            }
            return false;
        };
        if (applyInset()) return;
        const interval = setInterval(() => {
            attempts += 1;
            if (applyInset() || attempts > 40) {
                clearInterval(interval);
            }
        }, 250);
        return () => clearInterval(interval);
    }, []);

    return (
        // The real component's rendered height exceeds the 20px FOUC
        // placeholder once it loads; without this reserved min-height the
        // AppBar doesn't leave it enough room, so it overlaps the Toolbar
        // row below instead of pushing it down (same fix already applied
        // to the Keycloak login theme and the static site).
        <Box sx={{ width: "100%", minHeight: "40px", bgcolor: "#ffffff" }}>
            <ardc-megamenu
                id="ardc-menu"
                data-host="RAiD App"
                data-content-width="full"
                style={{ display: "block", opacity: 0, minHeight: "20px" }}
            />
        </Box>
    );
}
