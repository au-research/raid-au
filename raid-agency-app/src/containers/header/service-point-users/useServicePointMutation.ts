import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  addUserToGroupAdmins,
  removeUserFromGroupAdmins,
  removeUserFromServicePoint,
  updateUserServicePointUserRole,
} from "@/services/service-points";
/**
 * Custom hook for service point mutations with standardized error handling,
 * cache invalidation, and success notifications
 * 
 * @param {Object} config - Configuration object
 * @param {Function} config.mutationFn - The mutation function to execute
 * @param {string} config.successMessage - Success message to display (can include {variables} placeholder)
 * @param {string[]} [config.invalidateQueries] - Array of query keys to invalidate (defaults to ["servicePoints"])
 * @param {Function} [config.onError] - Optional custom error handler
 * @param {Function} [config.onSuccess] - Optional custom success handler (called after default)
 * @param {Object} [config.snackbar] - Snackbar utility object with openSnackbar method
 * @param {number} [config.snackbarDuration=3000] - Duration for snackbar display
 * @param {string} [config.snackbarSeverity="success"] - Severity level for snackbar
 */
type UseServicePointMutationConfig<TVariables = Record<string, unknown>, TData = unknown, TContext = unknown> = {
  mutationFn: (variables: TVariables) => Promise<TData>;
  successMessage: string;
  invalidateQueries?: (string | unknown[])[];
  onError?: (error: unknown) => void;
  onSuccess?: (data: TData, variables: TVariables, context: TContext) => void;
  snackbar?: {
    openSnackbar: (message: string, duration?: number, severity?: string) => void;
  };
  snackbarDuration?: number;
  snackbarSeverity?: string;
};

export const useServicePointMutation = <TVariables = Record<string, unknown>, TData = unknown, TContext = unknown>({
  mutationFn,
  successMessage,
  invalidateQueries = ["servicePoints", "service-point-request"],
  onError,
  onSuccess,
  snackbar,
  snackbarDuration = 3000,
  snackbarSeverity = "success"
}: UseServicePointMutationConfig<TVariables, TData, TContext>) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn,
    onError: (error) => {
      console.error(error);
      const message = `Failed to perform operation: ${error instanceof Error ? error.message : String(error)}`;
      snackbar && snackbar.openSnackbar(message, snackbarDuration, "error");
      onError?.(error);
    },
    onSuccess: (data, variables, context) => {
      // Invalidate queries
      invalidateQueries.forEach(queryKey => {
        queryClient.invalidateQueries({
          queryKey: Array.isArray(queryKey) ? queryKey : [queryKey],
        });
      });

      // Show success notification
      if (snackbar) {
        // Replace all {variables.*} placeholders with corresponding variable values if present
        const message = successMessage.replace(/\{variables\.([^}]+)\}/g, (_, key) => {
          const value = (variables as Record<string, unknown>)?.[key];
          const stringValue = String(value);
          return value !== undefined ? JSON.stringify(value).replace(/"/g, '').charAt(0).toUpperCase() + stringValue.slice(1) : '';
        });
        snackbar.openSnackbar(message, snackbarDuration, snackbarSeverity);
      }

      // Call custom success handler if provided
      onSuccess?.(data, variables, context as unknown as TContext);
    },
  });
};

// 1: Modify user access
export const useModifyUserAccess = (snackbar: { openSnackbar: (message: string, duration?: number, severity?: string) => void }) => {
  return useServicePointMutation({
    mutationFn: updateUserServicePointUserRole,
    successMessage: '{variables.operation} user role service-point-user',
    snackbar,
  });
};

// 2: Remove user from service point
export const useRemoveUserFromServicePoint = (snackbar: { openSnackbar: (message: string, duration?: number, severity?: string) => void }) => {
  return useServicePointMutation({
    mutationFn: removeUserFromServicePoint,
    successMessage: 'Removed user from service point',
    snackbar,
  });
};

// 3: Remove user from group admins
export const useRemoveUserFromGroupAdmins = (snackbar: { openSnackbar: (message: string, duration?: number, severity?: string) => void }) => {
  return useServicePointMutation({
    mutationFn: removeUserFromGroupAdmins,
    successMessage: 'Removed user from group admins',
    snackbar,
  });
};

// 4: Add user to group admins
export const useAddUserToGroupAdmins = (snackbar: { openSnackbar: (message: string, duration?: number, severity?: string) => void }) => {
  return useServicePointMutation({
    mutationFn: addUserToGroupAdmins,
    successMessage: 'Added user to group admins',
    snackbar,
  });
};
