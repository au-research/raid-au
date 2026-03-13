/**
 * RAID API Service Module
 *
 * This module provides functions for interacting with the RAID (Research Activity Identifier) API.
 * It handles fetching, creating, and updating RAIDs, as well as managing RAID history.
 */
import { RaidDto } from "@/generated/raid";
import { RaidHistoryType } from "@/pages/raid-history";
import { API_CONSTANTS } from "@/constants/apiConstants";

/**
 * Generates default headers for API requests including authentication
 * 
 * @param token - The authentication token
 * @returns Object containing headers with Content-Type and Authorization
 */
const getDefaultHeaders = (token: string) => ({
  "Content-Type": "application/json",
  Authorization: `Bearer ${token}`,
});

/**
 * Fetches all RAIDs with optional field filtering
 * 
 * @param fields - Optional array of field names to include in the response
 * @param token - Authentication token
 * @returns Promise resolving to an array of RAID DTOs
 */
export const fetchAllRaids = async ({
  fields,
  token,
}: {
  fields?: string[];
  token: string;
}): Promise<RaidDto[]> => {
  const url = new URL(API_CONSTANTS.RAID.ALL);
  if (fields?.length) {
    url.searchParams.set("includeFields", fields.join(","));
  }
  const response = await fetch(url, {
    headers: getDefaultHeaders(token),
  });
  if (!response.ok) {
    throw new Error(`RAiDs could not be fetched`);
  }

  await new Promise((resolve) => setTimeout(resolve, 1000));
  return response.json();
};

/**
 * Fetches a single RAID by its handle
 * 
 * @param handle - The RAID handle (identifier)
 * @param token - Authentication token
 * @returns Promise resolving to a RAID DTO
 */
export const fetchOneRaid = async ({
  handle,
  token,
}: {
  handle: string;
  token: string;
}): Promise<RaidDto> => {
  const url = API_CONSTANTS.RAID.BY_HANDLE(handle);
  const response = await fetch(url, {
    method: "GET",
    headers: getDefaultHeaders(token),
  });
  if (!response.ok) {
    throw new Error(`RAiD could not be fetched`);
  }

  return await response.json();
};

/**
 * Fetches the revision history of a RAID
 * 
 * @param handle - The RAID handle (identifier)
 * @param token - Authentication token
 * @returns Promise resolving to an array of RAID history entries
 */
export const fetchOneRaidHistory = async ({
  handle,
  token,
}: {
  handle: string;
  token: string;
}): Promise<RaidHistoryType[]> => {
  const url = API_CONSTANTS.RAID.HISTORY(handle);
  const response = await fetch(url, {
    method: "GET",
    headers: getDefaultHeaders(token),
  });
  if (!response.ok) {
    throw new Error(`RAiD history could not be fetched`);
  }

  return await response.json();
};

/**
 * Creates a new RAID
 * 
 * @param raid - The RAID data to create
 * @param token - Authentication token
 * @returns Promise resolving to the created RAID DTO
 */
export const createOneRaid = async ({
  raid,
  token,
}: {
  raid: RaidDto;
  token: string;
}): Promise<RaidDto> => {
  const url = API_CONSTANTS.RAID.ALL;
  const response = await fetch(url, {
    method: "POST",
    headers: getDefaultHeaders(token),
    body: JSON.stringify(raid),
  });
  if (!response.ok) {
    throw new Error(`RAiD could not be created`);
  }

  return await response.json();
};

/**
 * Updates an existing RAID
 * 
 * @param data - The updated RAID data
 * @param handle - The RAID handle (identifier)
 * @param token - Authentication token
 * @returns Promise resolving to the updated RAID DTO
 */
export const updateOneRaid = async ({
  data,
  handle,
  token,
}: {
  data: RaidDto;
  handle: string;
  token: string;
}): Promise<RaidDto> => {
  const raidToBeUpdated = transformBeforeUpdate(data);
  const url = API_CONSTANTS.RAID.BY_HANDLE(handle);
  const response = await fetch(url, {
    method: "PUT",
    headers: getDefaultHeaders(token),
    body: JSON.stringify(raidToBeUpdated),
  });
  if (!response.ok) {
    throw new Error(`RAiD could not be updated`);
  }

  return await response.json();
};

/**
 * Transforms RAID data before updates to handle empty date fields
 * 
 * This function converts empty string dates to undefined values,
 * which is required by the API for proper handling of date fields.
 * 
 * @param raid - The RAID data to transform
 * @returns The transformed RAID data ready for API submission
 */
export const transformBeforeUpdate = (raid: RaidDto): RaidDto => {
  // set all endDates to `undefined` if the value is an empty string
  if (raid?.date?.endDate === "") {
    raid.date.endDate = undefined;
  }
  if (raid?.title) {
    raid.title.forEach((title) => {
      if (title.endDate === "") {
        title.endDate = undefined;
      }
    });
  }
  if (raid?.contributor) {
    raid.contributor.forEach((contributor) => {
      if (contributor.position) {
        contributor.position.forEach((position) => {
          if (position.endDate === "") {
            position.endDate = undefined;
          }
        });
      }
    });
  }
  if (raid?.organisation) {
    raid.organisation.forEach((organisation) => {
      if (organisation.role) {
        organisation.role.forEach((role) => {
          if (role.endDate === "") {
            role.endDate = undefined;
          }
        });
      }
    });
  }

  return raid;
};
