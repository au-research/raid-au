import { getRuntimeConfig } from "@/config";

const api = () => getRuntimeConfig().apiBaseUrl;
const svc = () => getRuntimeConfig().services;

export const API_CONSTANTS = {
  SERVICE_POINT: {
    get ALL() { return `${api()}/service-point/`; },
    BY_ID: (id: number) => `${api()}/service-point/${id}`,
  },
  RAID: {
    get ALL() { return `${api()}/raid/`; },
    BY_HANDLE: (handle: string) => `${api()}/raid/${handle}`,
    HISTORY: (handle: string) => `${api()}/raid/${handle}/history`,
    get GET_ENV_FOR_HANDLE() { return `${svc().staticProd}/api/all-handles.json`; },
    RELATED_RAID_TITLE: (handle: string, environment: string) =>
      `${svc().staticBase.replace("{env}", environment)}/raids/${handle}.json`,
    HISTORY_DETAIL: (handle: string, version: string) =>
      `${api()}/raid/${handle}/${version}`,
  },
  ORCID: {
    get CONTRIBUTORS() { return `${svc().orcid}/contributors`; },
  },
  INVITE: {
    get SEND() { return svc().invite ? `${svc().invite}/invite` : undefined; },
    get FETCH() { return svc().invite ? `${svc().invite}/invite/fetch` : undefined; },
    get ACCEPT() { return svc().invite ? `${svc().invite}/invite/accept` : undefined; },
    get REJECT() { return svc().invite ? `${svc().invite}/invite/reject` : undefined; },
  },
  DOI: {
    REGISTRATION: (handle: string) => `https://doi.org/doiRA/${handle}`,
    CROSS_REF: (handle: string) => `https://api.crossref.org/works/${handle}`,
    DATA_CITE: (handle: string) => `https://api.datacite.org/dois/${handle}`,
    BY_HANDLE_URL: (handle: string) => `https://doi.org/api/handles/${handle}?type=url`,
  },
};
