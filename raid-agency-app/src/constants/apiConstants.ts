// Description: This file contains constants for API endpoints used in the application.
import { getApiEndpoint, getRootDomain } from "@/utils/api-utils/api-utils";
// Base domain name for the API
const domain = window.location.hostname.includes("localhost") ? import.meta.env.VITE_RAID_DOMAIN : `${window.location.origin}`;

export const BASE_URL = getApiEndpoint();

// Reusable endpoint path builder

export const API_CONSTANTS = {
  SERVICE_POINT: {
    ALL: `${BASE_URL}/service-point/`,
    BY_ID: (id: number) => `${BASE_URL}/service-point/${id}`,
  },
  RAID: {
    ALL: `${BASE_URL}/raid/`,
    BY_HANDLE: (handle: string) => `${BASE_URL}/raid/${handle}`,
    HISTORY: (handle: string) => `${BASE_URL}/raid/${handle}/history`,
    GET_ENV_FOR_HANDLE: `https://static.prod.${getRootDomain(domain)}/api/all-handles.json`,
    RELATED_RAID_TITLE:(handle: string, environment: string) =>
      `https://static.${environment}.${getRootDomain(domain)}/raids/${handle}.json`,
    HISTORY_DETAIL: (handle: string, version: string) =>
      `${BASE_URL}/raid/${handle}/${version}`,
  },
  ORCID: {
    CONTRIBUTORS: (subDomain: string, environment: string) =>
      `https://${subDomain}.${environment}.${getRootDomain(domain)}/contributors`,
  },
  INVITE: {
    SEND: (subDomain: string, environment: string) =>
      `https://${subDomain}.${environment}.${getRootDomain(domain)}/invite`,
    FETCH: (subDomain: string, environment: string) =>
      `https://${subDomain}.${environment}.${getRootDomain(domain)}/invite/fetch`,
    ACCEPT: (subDomain: string, environment: string) =>
      `https://${subDomain}.${environment}.${getRootDomain(domain)}/invite/accept`,
    REJECT: (subDomain: string, environment: string) =>
      `https://${subDomain}.${environment}.${getRootDomain(domain)}/invite/reject`,
  },
  DOI:{
    REGISTRATION: (handle: string) =>
      `https://doi.org/doiRA/${handle}`,
    CROSS_REF: (handle: string) =>
      `https://api.crossref.org/works/${handle}`,
    DATA_CITE: (handle: string) =>
      `https://api.datacite.org/dois/${handle}`,
    BY_HANDLE_URL: (handle: string) =>
      `https://doi.org/api/handles/${handle}?type=url`
  }
};
// Add more API constants as needed