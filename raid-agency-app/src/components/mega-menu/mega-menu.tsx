import { useExternalScript } from "@/hooks/useExternalScript";

const MEGAMENU_SCRIPT_SRC =
  "https://ardc.edu.au/wp-content/plugins/ardc-custom-web-components/dist/loaders/megamenu.min.js";

export const MegaMenu = () => {
    useExternalScript(MEGAMENU_SCRIPT_SRC, () => {
        const el = document.getElementById("ardc-menu");
        if (el) el.style.display = "none";
    });

    return (
        <ardc-megamenu
            id="ardc-menu"
            data-host="RAiD App"
            data-content-width="full"
            style={{ display: "block", opacity: 0, minHeight: "20px" }}
        />
    );
}
