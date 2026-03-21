import {useErrorDialog} from "@/components/error-dialog";
import {RaidForm} from "@/components/raid-form";
import {RaidFormErrorMessage} from "@/components/raid-form-error-message";
import {RaidDto} from "@/generated/raid";
import {newRaid, raidRequest} from "@/utils/data-utils";
import {Container, Stack} from "@mui/material";

import {useMutation} from "@tanstack/react-query";
import {useNavigate} from "react-router-dom";
import {raidService} from "@/services/raid-service.ts";
import { useSnackbar } from "@/components/snackbar/hooks/useSnackbar";
import { messages } from "@/constants/messages";

export const MintRaid = () => {
  const { openErrorDialog } = useErrorDialog();
  const navigate = useNavigate();
  const snackbar = useSnackbar();

  const mintMutation = useMutation({
    mutationFn: raidService.create,
    onSuccess: async (data) => {
      const resultHandle = new URL(data.identifier?.id ?? "");
      const [prefix, suffix] = resultHandle.pathname.split("/").filter(Boolean);
      navigate(`/raids/${prefix}/${suffix}`);
      snackbar.openSnackbar(messages.raidCreated, 3000, "success");
    },
    onError: (error: Error) => {
      RaidFormErrorMessage(error, openErrorDialog);
    },
  });

  const handleSubmit = async (data: RaidDto) => {
    mintMutation.mutate(raidRequest(data));
  };

  return (
    <Container maxWidth="lg" sx={{ py: 2 }}>
      <Stack gap={2}>
        <RaidForm
          raidData={newRaid}
          onSubmit={handleSubmit}
          isSubmitting={mintMutation.isPending}
          prefix={""}
          suffix={""}
        />
      </Stack>
    </Container>
  );
};
