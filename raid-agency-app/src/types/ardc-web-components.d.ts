import "react";

type ArdcContentWidth = "small" | "normal" | "wide" | "full";

declare module "react" {
  namespace JSX {
    interface IntrinsicElements {
      "ardc-megamenu": React.DetailedHTMLProps<
        React.HTMLAttributes<HTMLElement> & {
          id?: string;
          "data-host"?: string;
          "data-content-width"?: ArdcContentWidth;
        },
        HTMLElement
      >;
      "ardc-footer": React.DetailedHTMLProps<
        React.HTMLAttributes<HTMLElement> & {
          "data-content-width"?: ArdcContentWidth;
          "data-show-ardc-logo"?: boolean | "";
        },
        HTMLElement
      >;
    }
  }
}
