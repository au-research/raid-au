import embargoedRaidData from "../raw-data/embargoed-raids.json";

export interface EmbargoedRaidSummary {
  handle: string;
  embargoedStatusLabel: string;
  embargoExpiry: string | null;
  accessStatement: string | null;
  registrationAgencyRor: string;
  registrationAgencyName: string;
  servicePointName: string;
  ownerRor: string;
  ownerName: string;
}

export const embargoedRaids = embargoedRaidData as EmbargoedRaidSummary[];
