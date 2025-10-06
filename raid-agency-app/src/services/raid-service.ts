import {RaidDto} from "@/generated/raid";
import {authService} from "@/services/auth-service.ts";
import {RaidHistoryType} from "@/pages/raid-history";
import {transformBeforeUpdate} from "@/services/raid.ts";
import { API_CONSTANTS } from "@/constants/apiConstants";

export const raidService = {
    fetchAll: async (fields?: string[]) => {
        // the trailing slash is required for the API to work
        const url = new URL(API_CONSTANTS.RAID.ALL);

        if (fields?.length) {
            fields.forEach(field => url.searchParams.append("includeFields", field));
        }

        const response = await authService.fetchWithAuth(url.toString());

        if (!response.ok) {
            throw new Error(`RAiDs could not be fetched`);
        }

        return await response.json() as RaidDto[];
    },
    fetch: async (handle: string) => {
        const url = API_CONSTANTS.RAID.BY_HANDLE(handle);
        const response = await authService.fetchWithAuth(url);

        if (!response.ok) {
            throw new Error(`RAiD could not be fetched`);
        }

        return await response.json() as RaidDto;
    },
    fetchHistory: async (handle: string) => {
        const url = API_CONSTANTS.RAID.HISTORY(handle);
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error(`RAiD history could not be fetched`);
        }

        return await response.json() as RaidHistoryType[];
    },
    create: async (raid: RaidDto) => {
        const url = API_CONSTANTS.RAID.ALL;
        const response = await authService.fetchWithAuth(url, {
            method: "POST",
            body: JSON.stringify(raid),
        });

        if (!response.ok) {
            throw new Error(await response.text());
        }

        return await response.json() as RaidDto;
    },
    update: async (data: RaidDto, handle: string) => {
        const raidToBeUpdated = transformBeforeUpdate(data);
        const url = API_CONSTANTS.RAID.BY_HANDLE(handle);
        const response = await authService.fetchWithAuth(url, {
            method: "PUT",
            body: JSON.stringify(raidToBeUpdated),
        });
        if (!response.ok) {
            throw new Error(await response.text());
        }
        return await response.json() as RaidDto;
    },
};
