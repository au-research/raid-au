import { API_CONSTANTS } from "@/constants/apiConstants";

export const fetchIsniName = async ({
  isni,
}: {
  isni: string;
}): Promise<string> => {
  const response = await fetch(API_CONSTANTS.ISNI.NAME(isni));

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const data = await response.json();
  return data.name ?? "";
};
