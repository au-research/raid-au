import Banner from "@/components/alert-notifications/banner/Banner";

/**
 * Shown under the header when the realm's login theme could not be looked up
 * (RAID-1065). The app then shows the neutral RAiD look. People can retry by
 * reloading. It is not part of the app-config.json issues because the file is
 * fine; the lookup is what failed.
 */
export const ThemeLookupBanner = ({ supportEmail }: { supportEmail: string }) => (
  <Banner
    variant="warning"
    role="status"
    align="start"
    dismissible={false}
    title="We could not load this site's branding, so the default RAiD look is shown."
    message={
      <>
        Try reloading the page. If the problem continues, contact{" "}
        <a href={`mailto:${supportEmail}`}>{supportEmail}</a>.
      </>
    }
    action={() => window.location.reload()}
    actionLabel="Reload"
  />
);
