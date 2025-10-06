export const messages = {
    // General messages
    required: "This field is required",
    invalidEmail: "Please enter a valid email address",
    minLength: (min: number) => `Minimum length is ${min} characters`,
    maxLength: (max: number) => `Maximum length is ${max} characters`,
    // Organisation messages
    organisationNotFound: "Organisation not found",
    organisationLookup: "Lookup organisation",
    organisationSelected: (name: string) => `Organisation selected: ${name}`,
    // Raid messages
    raidCreated: "Raid created successfully",
    raidUpdated: "Raid updated successfully",
    raidNotFound: "❌ Raid not found",
    // User messages
    userNotFound: "User not found",
    userAdded: "User added successfully",
    userRemoved: "User removed successfully",

    // Validation messages
    validationError: "Validation error occurred",
    fieldRequired: (field: string) => `${field} is required`,
    fieldInvalid: (field: string) => `${field} is invalid`,
    fieldTooShort: (field: string, min: number) => `${field} must be at least ${min} characters long`,
    fieldTooLong: (field: string, max: number) => `${field} must not exceed ${max} characters`,

    // Form messages    
    formSubmitted: "Form submitted successfully",
    formSubmissionFailed: "Form submission failed", 
    formReset: "Form has been reset",
    formValidationError: "Form validation error",

    // Miscellaneous messages
    loading: "Loading, please wait...",
    saving: "Saving, please wait...",
    actionSuccess: "Action completed successfully",
    actionFailed: "Action failed, please try again",
    noDataFound: "No data found",
    unauthorized: "You are not authorized to perform this action",
    forbidden: "Access forbidden",

    // Confirmation messages
    deleteConfirmation: "Are you sure you want to delete this item?",
    saveChangesConfirmation: "You have unsaved changes. Do you want to leave without saving?",
    unsavedChanges: "You have unsaved changes. Please save before leaving.",
    // Notification messages
    notificationSuccess: "Notification sent successfully",
    notificationFailed: "Notification sending failed",
    notificationNotFound: "Notification not found",

    // API messages
    apiTimeout: "The request timed out, please try again later",    
    apiNotFound: "The requested resource was not found",
    apiBadRequest: "Bad request, please check your input",
    apiUnauthorized: "Unauthorized access, please log in",
    apiForbidden: "You do not have permission to access this resource",

    // Service Point messages
    servicePointCreated: "Service point created successfully",
    servicePointUpdated: "Service point updated successfully",
    servicePointDeleted: "Service point deleted successfully",
    servicePointNotFound: "Service point not found",
    servicePointCreationFailed: "Service point creation failed",
    servicePointUpdateFailed: "Service point update failed",
    servicePointDeletionFailed: "Service point deletion failed",
    servicePointUniqueRepositoryID: "Repository ID must be unique. The provided Repository ID is already in use.",
}
