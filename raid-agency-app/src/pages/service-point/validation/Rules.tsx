import { z } from "zod";

// Validation schema for creating a service point

export const createServicePointRequestValidationSchema = z.object({
  servicePointCreateRequest: z.object({
    name: z.string().min(3, "Name must be at least 3 characters"),
    identifierOwner: z.string(),
    adminEmail: z.string()
      .min(1, "Admin email is required")
      .email("Invalid email format"), // Better than regex for emails
    techEmail: z.string()
      .min(1, "Tech email is required")
      .email("Invalid email format"),
    enabled: z.boolean(),
    password: z.string().min(1, "Password is required").min(8, "Password must be at least 8 characters"),
    prefix: z.string()
      .min(1, "Prefix is required")
      .regex(/^10\.\d+$/, "Prefix must follow format 10.xxx"),
    repositoryId: z.string()
      .min(1, "Repository ID is required")
      .regex(/^[A-Z]+\.[A-Z]+$/, "Repository ID must be ABCD.EFGH format"),
    appWritesEnabled: z.boolean(),
  }),
});
