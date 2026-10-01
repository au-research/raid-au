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
    //
    // The component reveals itself (opacity 0 -> 1) as soon as it
    // initialises, which can happen before this poll has run - visibly
    // flashing the button flush-right for a moment before it jumps to
    // its corrected position. An inline opacity:0 override can't reliably
    // prevent that: the component appears to replace its own inline
    // `style` attribute wholesale at some point after first mount (not a
    // fixed delay - likely tied to its data fetch completing), which
    // silently wipes out anything we'd set via el.style, including an
    // !important inline override. An external stylesheet rule doesn't
    // live on the element's style attribute at all, so it survives that
    // replacement - hide via a stylesheet instead, and only remove the
    // rule once a fixed safety window has elapsed with the position fix
    // continuously re-applied throughout.
    useEffect(() => {
        const host = document.getElementById("ardc-menu");
        if (!host) return;

        const hideStyle = document.createElement("style");
        hideStyle.textContent = "#ardc-menu { opacity: 0 !important; }";
        document.head.appendChild(hideStyle);

        const applyFix = () => {
            const btn = host.shadowRoot?.querySelector<HTMLElement>(".ardc-header__explore-btn");
            if (!btn) return false;
            // The component's own shadow stylesheet sets `right` with
            // !important, so a plain inline-style assignment loses -
            // setProperty with "important" priority is required to win.
            btn.style.setProperty("right", "24px", "important");
            return true;
        };

        let elapsedMs = 0;
        const tickMs = 50;
        const safetyWindowMs = 800;
        const interval = setInterval(() => {
            applyFix();
            elapsedMs += tickMs;
            if (elapsedMs >= safetyWindowMs) {
                clearInterval(interval);
                hideStyle.remove();
            }
        }, tickMs);
        return () => {
            clearInterval(interval);
            hideStyle.remove();
        };
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
