import { useMutation, useQuery } from "@tanstack/react-query";
import {
  fetchClientCredentials,
  rotateClientCredential,
  revokeClientCredential,
  getClientCredentialSecret,
} from "@/services/client-credentials";
import { useServicePointMutation } from "@/containers/header/service-point-users/useServicePointMutation";
import { messages } from "@/constants/messages";
import type { SnackbarContextInterface } from "@/components/snackbar";

export const useClientCredentials = (groupId: string | undefined, token: string | undefined) => {
  return useQuery({
    queryKey: ["clientCredentials", groupId],
    queryFn: () => fetchClientCredentials({ groupId: groupId as string, token }),
    enabled: !!groupId && !!token,
  });
};

// Cast to the factory's looser severity type, matching the existing
// useModifyUserAccess-style call sites in useServicePointMutation.ts.
type LooseSnackbar = { openSnackbar: (message: string, duration?: number, severity?: string) => void };

export const useRotateClientCredential = (groupId: string | undefined, snackbar: SnackbarContextInterface) => {
  return useServicePointMutation({
    mutationFn: rotateClientCredential,
    successMessage: messages.clientCredentialRotated,
    invalidateQueries: [["clientCredentials", groupId]],
    snackbar: snackbar as LooseSnackbar,
  });
};

export const useRevokeClientCredential = (groupId: string | undefined, snackbar: SnackbarContextInterface) => {
  return useServicePointMutation({
    mutationFn: revokeClientCredential,
    successMessage: messages.clientCredentialRevoked,
    invalidateQueries: [["clientCredentials", groupId]],
    snackbar: snackbar as LooseSnackbar,
  });
};

// Not routed through useServicePointMutation: viewing a secret isn't a
// state-changing action worth a success snackbar or a cache invalidation,
// just an on-demand fetch of the current value.
export const useViewClientCredentialSecret = () => {
  return useMutation({ mutationFn: getClientCredentialSecret });
};
