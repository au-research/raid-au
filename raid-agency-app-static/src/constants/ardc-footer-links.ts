// The ARDC footer's quick links. Fixed, external destinations per ARDC's RAiD
// domain/service guide, the same list as raid-agency-app's
// src/constants/external-links.ts and the Keycloak login theme's template.ftl
// (kept in sync by hand, since these are three separate deploy targets with no
// shared build).
//
// Request Support goes to ARDC's helpdesk and Contact the ARDC to its general
// contact address (RAID-1068).
export const QUICK_LINKS = [
  { label: "About RAiD", path: "https://ardc.edu.au/services/ardc-identifier-services/raid-research-activity-identifier-service/" },
  { label: "Visit raid.org", path: "https://raid.org" },
  { label: "RAiD User Guides", path: "https://documentation.ardc.edu.au/raid" },
  { label: "RAiD Schema Documentation", path: "https://metadata.raid.org/" },
  { label: "Request Support", path: "mailto:services@ardc.edu.au" },
  { label: "Contact the ARDC", path: "mailto:contact@ardc.edu.au" },
];
