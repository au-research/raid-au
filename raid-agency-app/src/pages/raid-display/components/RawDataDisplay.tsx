import { SnackbarContextInterface, useSnackbar } from "@/components/snackbar";
import { RaidDto } from "@/generated/raid";
import { copyToClipboardWithNotification } from "@/utils/copy-utils/copyWithNotify";
import { downloadJson } from "@/utils/file-utils/file-utils";
import {
  ContentCopy as ContentCopyIcon,
  FileDownloadOutlined as FileDownloadOutlinedIcon,
} from "@mui/icons-material";
import {
  Card,
  CardContent,
  CardHeader,
  IconButton,
  Tooltip,
} from "@mui/material";
import { useParams } from "react-router-dom";

export const RawDataDisplay = ({ raidData }: { raidData: RaidDto }) => {
  const snackbar = useSnackbar();
  const { prefix, suffix } = useParams();

  return (
    <Card>
      {/* <CardHeader title="Raw Data" /> */}
      <CardHeader
        action={
          <>
            <Tooltip title="Copy raw JSON" placement="top">
              <IconButton
                aria-label="copy-json"
                onClick={async () => {
                    await copyToClipboardWithNotification(
                      JSON.stringify(raidData, null, 2),
                      "Copied raw JSON data to clipboard",
                      snackbar as SnackbarContextInterface
                    )
                  }
                }
              >
                <ContentCopyIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Download JSON" placement="top">
              <IconButton
                aria-label="download-json"
                onClick={() =>
                  downloadJson({
                    data: raidData,
                    snackbar,
                    prefix,
                    suffix,
                    labelPlural: "raid",
                  })
                }
              >
                <FileDownloadOutlinedIcon />
              </IconButton>
            </Tooltip>
          </>
        }
        title="Raw Data"
      />
      <CardContent sx={{ fontSize: 12 }}>
        <pre>{JSON.stringify(raidData, null, 2)}</pre>
      </CardContent>
    </Card>
  );
};
