import packageJson from "@/../package.json";
import contributorPositionGenerator from "@/entities/contributor/position/data-components/contributor-position-generator";
import contributorRoleGenerator from "@/entities/contributor/role/data-components/contributor-role-generator";
import { Contributor } from "@/generated/raid";

type ContributorExtended = Contributor &
  (
    | { uuid: string; id?: never; email?: never }
    | { id: string; uuid?: never; email?: never }
    | { email: string; uuid?: never; id?: never }
  );

const contributorGenerator = (): ContributorExtended => {
  // Create a base object with all required Contributor properties
  const baseData: Omit<Contributor, "id" | "email" | "uuid"> = {
    leader: true,
    contact: true,
    schemaUri: "https://orcid.org/",
    position: [contributorPositionGenerator()],
    role: [contributorRoleGenerator(), contributorRoleGenerator()],
  };

  if (packageJson.apiVersion === "3") {
    return {
      ...baseData,
      email: "",
    } as unknown as ContributorExtended;
  } else {
    return {
      ...baseData,
      id: "",
    } as ContributorExtended;
  }
};

export default contributorGenerator;
