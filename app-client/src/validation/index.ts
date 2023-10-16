import { z } from "zod";
import { titlesValidationSchema } from "../Forms/RaidForm/components/FormTitlesComponent";
import { datesValidationSchema } from "Forms/RaidForm/components/FormDatesComponent";

export const ValidationFormSchema = z.object({
  identifier: z
    .object({
      id: z.string().nonempty(),
      owner: z.object({
        id: z.string().nonempty(),
        schemaUri: z.string().nonempty(),
        servicePoint: z.number().int(),
      }),
      license: z.string().nonempty(),
      version: z.number().int(),
      globalUrl: z.string().nonempty(),
      schemaUri: z.string().nonempty(),
      raidAgencyUrl: z.string().nonempty(),
      registrationAgency: z.object({
        id: z.string().nonempty(),
        schemaUri: z.string().nonempty(),
      }),
    })
    .optional(),
  title: titlesValidationSchema,
  date: datesValidationSchema,
  description: z.array(
    z.object({
      text: z.string().nonempty(),
      type: z.object({
        id: z.string(),
        schemaUri: z.literal(
          "https://github.com/au-research/raid-metadata/tree/main/scheme/description/type/v1/"
        ),
      }),
      language: z.object({
        id: z.string().nonempty(),
        schemaUri: z.literal("https://iso639-3.sil.org"),
      }),
    })
  ),
  access: z.object({
    type: z.object({
      id: z.string(),
      schemaUri: z.literal(
        "https://github.com/au-research/raid-metadata/tree/main/scheme/access/type/v1/"
      ),
    }),
    accessStatement: z.object({
      text: z.string().nonempty(),
      language: z.object({
        id: z.string().nonempty(),
        schemaUri: z.literal("https://iso639-3.sil.org"),
      }),
    }),
  }),
  alternateUrl: z.array(
    z.object({
      url: z.string().url().nonempty(),
    })
  ),
  contributor: z.array(
    z.object({
      id: z
        .string()
        .regex(
          new RegExp("^https://orcid.org/\\d{4}-\\d{4}-\\d{4}-\\d{3}[0-9X]$")
        ),
      schemaUri: z.literal("https://orcid.org/"),
      position: z.array(
        z.object({
          id: z.string(),
          schemaUri: z.literal(
            "https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1/"
          ),
          startDate: z.string(),
        })
      ),
      role: z.array(
        z.object({
          id: z.string(),
          schemaUri: z.literal("https://credit.niso.org/"),
        })
      ),
    })
  ),
  organisation: z.array(
    z.object({
      id: z.string().nonempty(),
      schemaUri: z.string().nonempty(),
      role: z.array(
        z.object({
          id: z.string(),
          schemaUri: z.literal(
            "https://github.com/au-research/raid-metadata/tree/main/scheme/organisation/role/v1/"
          ),
          startDate: z.string(),
          endDate: z.string().optional(),
        })
      ),
    })
  ),
  subject: z.array(
    z.object({
      id: z.string().nonempty(),
      schemaUri: z.string().nonempty(),
      keyword: z.array(
        z.object({
          text: z.string().nonempty(),
          language: z.object({
            id: z.string().nonempty(),
            schemaUri: z.literal("https://iso639-3.sil.org"),
          }),
        })
      ),
    })
  ),
  relatedRaid: z.array(
    z.object({
      id: z.string().nonempty(),
      type: z.object({
        id: z.string(),
        schemaUri: z.string(),
      }),
    })
  ),
  relatedObject: z.array(
    z.object({
      id: z.string().nonempty(),
      schemaUri: z.string().nonempty(),
      type: z.object({
        id: z.string(),
        schemaUri: z.string(),
      }),
      category: z.object({
        id: z.string(),
        schemaUri: z.string(),
      }),
    })
  ),
  alternateIdentifier: z.array(
    z.object({
      id: z.string().nonempty(),
      type: z.string().nonempty(),
    })
  ),
  spatialCoverage: z.array(
    z.object({
      id: z.string().nonempty(),
      schemaUri: z.string().nonempty(),
      place: z.string().nonempty(),
      language: z.object({
        id: z.string().nonempty(),
        schemaUri: z.literal("https://iso639-3.sil.org"),
      }),
    })
  ),
  traditionalKnowledgeLabel: z.array(
    z.object({
      id: z.string().nonempty(),
      schemaUri: z.string().nonempty(),
    })
  ),
});
