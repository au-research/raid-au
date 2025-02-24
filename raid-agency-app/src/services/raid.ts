import packageJson from "@/../package.json";
import { RaidDto } from "@/generated/raid";
import { RaidHistoryType } from "@/pages/raid-history";
import { fetchServicePoints } from "@/services/service-points";
import { getApiEndpoint } from "@/utils/api-utils/api-utils";

const endpoint = getApiEndpoint();
const API_ENDPOINT = `${endpoint}/raid`;

export const fetchRaids = async ({
  fields,
  token,
}: {
  fields?: string[];
  token: string;
}): Promise<RaidDto[]> => {
  const url = new URL(`${API_ENDPOINT}/`);

  if (fields && fields.length > 0) {
    const fieldsQuery = fields.join(",");
    url.searchParams.set("includeFields", fieldsQuery);
  }

  const response = await fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      "X-Raid-Api-Version": packageJson.apiVersion,
    },
  });
  return await response.json();
};

export const fetchRaid = async ({
  handle,
  token,
}: {
  handle: string;
  token: string;
}): Promise<RaidDto> => {
  const response = await fetch(`${API_ENDPOINT}/${handle}`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      "X-Raid-Api-Version": packageJson.apiVersion,
    },
  });
  return await response.json();
};

export const fetchRaidHistory = async ({
  id,
  token,
}: {
  id: string;
  token: string;
}): Promise<RaidHistoryType[]> => {
  const response = await fetch(`${API_ENDPOINT}/${id}/history`, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      "X-Raid-Api-Version": packageJson.apiVersion,
    },
  });
  return await response.json();
};

export const createRaid = async ({
  data,
  token,
}: {
  data: RaidDto;
  token: string;
}): Promise<RaidDto> => {
  try {
    const response = await fetch(`${API_ENDPOINT}/`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
        "X-Raid-Api-Version": packageJson.apiVersion,
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(JSON.stringify(errorData) || "Failed to create raid");
    }

    const responseData = await response.json();
    return responseData;
  } catch (error) {
    let errorMessage = "Failed to create raid";
    if (error instanceof Error) {
      errorMessage = error.message;
    }
    throw new Error(errorMessage);
  }
};

export const updateRaid = async ({
  id,
  data,
  token,
}: {
  id: string;
  data: RaidDto;
  token: string;
}): Promise<RaidDto> => {
  try {
    const raidToBeUpdated = beforeRaidUpdate(data);
    const response = await fetch(`${API_ENDPOINT}/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
        "X-Raid-Api-Version": packageJson.apiVersion,
      },
      body: JSON.stringify(raidToBeUpdated),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(JSON.stringify(errorData) || "Failed to create raid");
    }

    const responseData = await response.json();
    return responseData;
  } catch (error) {
    let errorMessage = "Failed to create raid";
    if (error instanceof Error) {
      errorMessage = error.message;
    }
    throw new Error(errorMessage);
  }
};

export const beforeRaidUpdate = (raid: RaidDto): RaidDto => {
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
