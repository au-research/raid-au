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
    // `style` attribute wholesale at some point after first mount, which
    // silently wipes out anything we'd set via el.style, including an
    // !important inline override. An external stylesheet rule doesn't
    // live on the element's style attribute at all, so it survives that
    // replacement - hide via a stylesheet instead.
    //
    // Reveal as soon as the position has held for a few consecutive
    // checks, rather than always waiting a flat safety window: a style
    // replacement resets `right` back to the component's own value, so
    // seeing it stay at our override across several ticks is itself the
    // signal that the component has stopped overwriting it. The flat
    // window stays only as an upper-bound fallback in case the button
    // never stabilises (or never appears at all).
    useEffect(() => {
        const host = document.getElementById("ardc-menu");
        if (!host) return;

        const hideStyle = document.createElement("style");
        hideStyle.textContent = "#ardc-menu { opacity: 0 !important; }";
        document.head.appendChild(hideStyle);

        const tickMs = 50;
        const maxWaitMs = 800;
        const stableTicksRequired = 3;
        let elapsedMs = 0;
        let stableTicks = 0;

        const interval = setInterval(() => {
            const btn = host.shadowRoot?.querySelector<HTMLElement>(".ardc-header__explore-btn");
            const alreadyCorrect = btn?.style.getPropertyValue("right") === "24px";
            // The component's own shadow stylesheet sets `right` with
            // !important, so a plain inline-style assignment loses -
            // setProperty with "important" priority is required to win.
            btn?.style.setProperty("right", "24px", "important");
            stableTicks = alreadyCorrect ? stableTicks + 1 : 0;

            elapsedMs += tickMs;
            if (stableTicks >= stableTicksRequired || elapsedMs >= maxWaitMs) {
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
