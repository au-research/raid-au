/**
 * Central validation schema module for the RAID application
 * 
 * This module combines all entity-specific validation schemas into a single 
 * comprehensive validation schema that matches the RAID data structure.
 * The validation architecture follows these principles:
 * 
 * 1. Each entity has its own validation schema in a dedicated directory
 * 2. The main RaidValidationSchema composes all entity schemas
 * 3. Schemas use Zod for type-safe validation integrated with TypeScript
 * 4. React Hook Form consumes these schemas via @hookform/resolvers/zod
 */

import { accessValidationSchema } from "@/entities/access/validation-schema/access-validation-schema";
import { alternateIdentifierValidationSchema } from "@/entities/alternate-identifier/validation-schema/alternate-identifier-validation-schema";
import { alternateUrlValidationSchema } from "@/entities/alternate-url/validation-schema/alternate-url-validation-schema";
import { contributorValidationSchema } from "@/entities/contributor/validation-schema/contributor-validation-schema";
import { dateValidationSchema } from "@/entities/date/validation-schema/date-validation-schema";
import { descriptionValidationSchema } from "@/entities/description/validation-schema/description-validation-schema";
import { identifierValidationSchema } from "@/entities/identifier/validation-schema/identifier-validation-schema";
import { organisationValidationSchema } from "@/entities/organisation/validation-schema/organisation-validation-schema";
import { relatedObjectValidationSchema } from "@/entities/related-object/validation-schema/related-object-validation-schema";
import { relatedRaidValidationSchema } from "@/entities/related-raid/validation-schema/related-raid-validation-schema";
import { spatialCoverageValidationSchema } from "@/entities/spatial-coverage/validation-schema/spatial-coverage-validation-schema";
import { subjectValidationSchema } from "@/entities/subject/validation-schema/subject-validation-schema";
import { titleValidationSchema } from "@/entities/title/validation-schema/title-validation-schema";
import { z } from "zod";

export const RaidValidationSchema = z.object({
  identifier: identifierValidationSchema,
  title: titleValidationSchema,
  date: dateValidationSchema,
  description: descriptionValidationSchema,
  access: accessValidationSchema,
  alternateUrl: alternateUrlValidationSchema,
  contributor: contributorValidationSchema,
  organisation: organisationValidationSchema,
  subject: subjectValidationSchema,
  relatedRaid: relatedRaidValidationSchema,
  relatedObject: relatedObjectValidationSchema,
  alternateIdentifier: alternateIdentifierValidationSchema,
  spatialCoverage: spatialCoverageValidationSchema,
});
