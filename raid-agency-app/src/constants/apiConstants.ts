import { getRootDomain } from "@/utils/api-utils/api-utils";
import { getRuntimeConfig } from "@/config";

const rootDomain = () => getRootDomain(getRuntimeConfig().raidDomain);
const api = () => getRuntimeConfig().apiBaseUrl;

export const API_CONSTANTS = {
  SERVICE_POINT: {
    get ALL() { return `${api()}/service-point/`; },
    BY_ID: (id: number) => `${api()}/service-point/${id}`,
  },
  RAID: {
    get ALL() { return `${api()}/raid/`; },
    BY_HANDLE: (handle: string) => `${api()}/raid/${handle}`,
    HISTORY: (handle: string) => `${api()}/raid/${handle}/history`,
    get GET_ENV_FOR_HANDLE() { return `https://static.prod.${rootDomain()}/api/all-handles.json`; },
    RELATED_RAID_TITLE: (handle: string, environment: string) =>
      `https://static.${environment}.${rootDomain()}/raids/${handle}.json`,
    HISTORY_DETAIL: (handle: string, version: string) =>
      `${api()}/raid/${handle}/${version}`,
  },
  ORCID: {
    CONTRIBUTORS: (subDomain: string, environment: string) =>
      `https://${subDomain}.${environment}.${rootDomain()}/contributors`,
  },
  INVITE: {
    SEND: (subDomain: string, environment: string) =>
      `https://${subDomain}.${environment}.${rootDomain()}/invite`,
    FETCH: (subDomain: string, environment: string) =>
      `https://${subDomain}.${environment}.${rootDomain()}/invite/fetch`,
    ACCEPT: (subDomain: string, environment: string) =>
      `https://${subDomain}.${environment}.${rootDomain()}/invite/accept`,
    REJECT: (subDomain: string, environment: string) =>
      `https://${subDomain}.${environment}.${rootDomain()}/invite/reject`,
  },
  DOI: {
    REGISTRATION: (handle: string) =>
      `https://doi.org/doiRA/${handle}`,
    CROSS_REF: (handle: string) =>
      `https://api.crossref.org/works/${handle}`,
    DATA_CITE: (handle: string) =>
      `https://api.datacite.org/dois/${handle}`,
    BY_HANDLE_URL: (handle: string) =>
      `https://doi.org/api/handles/${handle}?type=url`,
  },
};
