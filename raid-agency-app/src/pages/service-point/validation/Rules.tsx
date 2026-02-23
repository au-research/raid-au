import { z } from "zod";

// Validation schema for creating and updating a service point

// Base schema for the service point fields
const servicePointBaseSchema = z.object({
  name: z.string().min(3, "Name must be at least 3 characters"),
  identifierOwner: z.string(),
  adminEmail: z.string()
    .min(1, "Admin email is required")
    .email("Invalid email format"),
  techEmail: z.string()
    .min(1, "Tech email is required")
    .email("Invalid email format"),
  enabled: z.boolean(),
  appWritesEnabled: z.boolean(),
  groupId: z.string().optional(),
});

// Create request schema
export const createServicePointRequestValidationSchema = z.object({
  servicePointCreateRequest: servicePointBaseSchema,
});

export const updateServicePointRequestValidationSchema = z.object({
  id: z.number(),
  servicePointUpdateRequest: servicePointBaseSchema.extend({id: z.number()})
});