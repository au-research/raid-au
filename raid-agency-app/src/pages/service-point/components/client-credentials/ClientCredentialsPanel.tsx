import { useState } from "react";
import { Alert, Stack } from "@mui/material";
import { useKeycloak } from "@/contexts/keycloak-context";
import { Loading } from "@/pages/loading";
import { ErrorAlertComponent } from "@/components/error-alert-component";
import type { ClientCredentialSecret } from "@/services/client-credentials";
import { useClientCredentials } from "./useClientCredentialMutations";
import { CreateClientCredentialForm } from "./CreateClientCredentialForm";
import { ClientCredentialsTable } from "./ClientCredentialsTable";
import { SecretRevealPanel } from "./SecretRevealPanel";

const SECRET_HEADINGS: Record<"created" | "rotated" | "viewed", string> = {
  created: "Credential created",
  rotated: "Secret rotated",
  viewed: "Viewing secret",
};

export function ClientCredentialsPanel({ groupId }: { groupId: string }) {
  const { token } = useKeycloak();
  const [revealed, setRevealed] = useState<{ secret: ClientCredentialSecret; action: "created" | "rotated" | "viewed" } | null>(null);

  const query = useClientCredentials(groupId, token);

  if (query.isPending) {
    return <Loading />;
  }

  if (query.isError) {
    return <ErrorAlertComponent error="Client credentials could not be fetched" />;
  }

  const credentials = query.data;
  const activeCount = credentials.filter((c) => c.enabled).length;

  return (
    <Stack gap={2}>
      <CreateClientCredentialForm
        groupId={groupId}
        token={token}
        activeCount={activeCount}
        onCreated={(secret) => setRevealed({ secret, action: "created" })}
      />

      {revealed && (
        <SecretRevealPanel
          credential={revealed.secret}
          heading={`${SECRET_HEADINGS[revealed.action]} — ${revealed.secret.label}`}
          onClose={() => setRevealed(null)}
        />
      )}

      {credentials.length === 0 ? (
        <Alert severity="info">
          No client credentials yet. Create one above to let a system authenticate on this service point's behalf.
        </Alert>
      ) : (
        <ClientCredentialsTable
          credentials={credentials}
          groupId={groupId}
          token={token}
          onSecretRevealed={(secret, action) => setRevealed({ secret, action })}
        />
      )}
    </Stack>
  );
}
