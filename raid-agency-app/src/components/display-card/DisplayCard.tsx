import { SnackbarContextInterface, useSnackbar } from "@/components/snackbar";
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
import { memo } from "react";
import { useParams } from "react-router-dom";

/**
 * Reusable card component for displaying data sections
 * 
 * Provides a consistent card layout with title and utility actions (copy/download)
 * for any content. Typically used for displaying sections of RAID data.
 * 
 * @param {string} labelPlural - Title to display in the card header
 * @param {any} data - Data object represented by this card (used for copy/download)
 * @param {React.ReactNode} children - Content to render inside the card
 * @returns {JSX.Element} Formatted card with header actions and content
 */
export const DisplayCard = memo(
  ({
    labelPlural,
    data,
    children,
  }: {
    labelPlural: string;
    data: any;
    children: React.ReactNode;
  }) => {
    const snackbar = useSnackbar();
    const { prefix, suffix } = useParams();

    return (
      <Card>
        <CardHeader
          title={labelPlural}
          action={
            <>
              <Tooltip title="Copy raw JSON" placement="top">
                <IconButton
                  aria-label="copy-json"
                  onClick={async () => {
                      await copyToClipboardWithNotification(
                        JSON.stringify(data, null, 2),
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
                      data,
                      snackbar,
                      prefix,
                      suffix,
                      labelPlural,
                    })
                  }
                >
                  <FileDownloadOutlinedIcon />
                </IconButton>
              </Tooltip>
            </>
          }
        />
        <CardContent>{children}</CardContent>
      </Card>
    );
  }
);

DisplayCard.displayName = "DisplayCard";
