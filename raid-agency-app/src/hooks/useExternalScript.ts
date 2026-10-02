import { useEffect } from "react";

/**
 * Injects a <script defer data-no-optimize> tag once per src, for embedding
 * externally-hosted web components (e.g. ARDC's mega-menu/footer). Skips
 * injection entirely if a script with the same src is already present.
 *
 * The script is intentionally never removed once added: these components
 * call customElements.define() on load, which throws if the browser sees
 * the same tag name registered twice. Removing the tag on unmount only
 * un-links the DOM node — the script has already executed by then — so a
 * later remount (React StrictMode's dev double-invoke, or navigating back
 * to a page that uses the component) would re-run it and crash.
 */
export function useExternalScript(src: string | null | undefined, onError?: () => void) {
  useEffect(() => {
    if (!src) return;

    const existing = document.querySelector<HTMLScriptElement>(`script[src="${src}"]`);
    if (existing) return;

    const script = document.createElement("script");
    script.src = src;
    script.defer = true;
    script.setAttribute("data-no-optimize", "");
    if (onError) {
      script.onerror = onError;
    }
    document.body.appendChild(script);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [src]);
}
