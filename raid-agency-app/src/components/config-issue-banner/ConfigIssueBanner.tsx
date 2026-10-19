import { Fragment } from "react";
import Banner from "@/components/alert-notifications/banner/Banner";
import {
  ConfigErrorReason,
  ConfigIssues,
  isUrlField,
  NO_CONFIG_ISSUES,
} from "@/config";

/**
 * Banner shown directly under the header when app-config.json is missing
 * fields or invalid (RAID-941).
 *
 * - "blocking": the app could not start; shown by ConfigErrorScreen.
 * - "warning": the app runs but some features may not work; shown by
 *   AppNavBar and the login page.
 *
 * Only field paths are shown, never the values in the file.
 */

type ConfigIssueBannerProps =
  | { level: "blocking"; reason: ConfigErrorReason | "startup-failed"; issues?: ConfigIssues }
  | { level: "warning"; issues: ConfigIssues };

const FieldList = ({ paths, describeUrls }: { paths: string[]; describeUrls: boolean }) => (
  <>
    {paths.map((path, index) => (
      <Fragment key={path}>
        {index > 0 && ", "}
        <code className="notification-banner__field">{path}</code>
        {describeUrls && isUrlField(path) && " (not a valid URL)"}
      </Fragment>
    ))}
  </>
);

const IssueSummary = ({ issues }: { issues: ConfigIssues }) => (
  <>
    {issues.missing.length > 0 && (
      <>
        {" "}Missing: <FieldList paths={issues.missing} describeUrls={false} />.
      </>
    )}
    {issues.invalid.length > 0 && (
      <>
        {" "}Invalid: <FieldList paths={issues.invalid} describeUrls />.
      </>
    )}
  </>
);

// First line of the warning banner, e.g.
// "Some configuration is missing: a, b. Invalid: c (not a valid URL)."
const WarningHeadline = ({ issues }: { issues: ConfigIssues }) =>
  issues.missing.length > 0 ? (
    <>
      Some configuration is missing: <FieldList paths={issues.missing} describeUrls={false} />.
      {issues.invalid.length > 0 && (
        <>
          {" "}Invalid: <FieldList paths={issues.invalid} describeUrls />.
        </>
      )}
    </>
  ) : (
    <>
      Some configuration is invalid: <FieldList paths={issues.invalid} describeUrls />.
    </>
  );

export const ConfigIssueBanner = (props: ConfigIssueBannerProps) => {
  if (props.level === "warning") {
    return (
      <Banner
        variant="warning"
        role="status"
        align="start"
        dismissible={false}
        title={<WarningHeadline issues={props.issues} />}
        message="Some features may not work as expected. Site administrators: update app-config.json and redeploy the release."
      />
    );
  }

  const issues = props.issues ?? NO_CONFIG_ISSUES;
  const summary =
    props.reason === "invalid-fields" ? (
      <>
        The app cannot start.
        <IssueSummary issues={issues} />
      </>
    ) : props.reason === "load-failed" ? (
      "The app cannot start because its configuration file could not be loaded."
    ) : (
      "The app could not start."
    );
  const adminAction =
    props.reason === "load-failed"
      ? "Site administrators: check that app-config.json is deployed, then redeploy the release."
      : "Site administrators: check app-config.json and redeploy the release.";

  return (
    <Banner
      variant="error"
      role="alert"
      align="start"
      fullWidth
      dismissible={false}
      title="This site is not configured correctly"
      message={
        <>
          <div>{summary}</div>
          <div className="notification-banner__admin">{adminAction}</div>
        </>
      }
      action={() => window.location.reload()}
      actionLabel="Reload"
    />
  );
};
