import { IndeterminateCheckBox } from "@mui/icons-material";
import { Grid, IconButton, Stack, Tooltip, Typography, TextField } from "@mui/material";
import { useState } from "react";
import { useFormContext } from "react-hook-form";
import CustomizedDialogs from "@/components/alert-dialog/alert-dialog";
import { Check, Delete } from "lucide-react";

function FieldGrid({
  index,
  isRowHighlighted,
  selectedCode,
}: {
  index: number;
  isRowHighlighted: boolean;
  selectedCode?: any;
}) {
  return (
    <Grid sx={{width: "100%"}} className={isRowHighlighted ? "remove" : ""}>
      <TextField
        value={selectedCode}
        name={`subject.${index}.id`}
        placeholder="Type"
        required={true}
        fullWidth
        sx={{ height: "50px" }}        
      />
    </Grid>
  );
}

export function SubjectDetailsForm({
  index,
  handleRemoveItem,
  selectedCode,
  id
}: {
  index: number;
  handleRemoveItem: (id: string, index: number) => void;
  selectedCode?: any;
  id: string
}) {
  const key = "subject";
  const label = "Subject";

  const [isRowHighlighted, setIsRowHighlighted] = useState(false);
  const { getValues } = useFormContext();
  const [alertOpen, setAlertOpen] = useState(false);

  const handleMouseEnter = () => setIsRowHighlighted(true);
  const handleMouseLeave = () => setIsRowHighlighted(false);

  return (
    <Stack gap={2}>
      <Typography variant="body2">
        <span
          style={{ textDecoration: isRowHighlighted ? "line-through" : "" }}
        >
          {getValues(`${key}.${index}.text`)
            ? getValues(`${key}.${index}.text`)
            : `${label} # ${index + 1}`}
        </span>
        {isRowHighlighted && " (to be deleted)"}
      </Typography>

      <Stack direction="row" alignItems="flex-start" gap={1}>
        <FieldGrid
          index={index}
          isRowHighlighted={isRowHighlighted}
          selectedCode={selectedCode}
        />

        <Tooltip title={`Remove ${label}`} placement="right">
          <IconButton
            aria-label="delete"
            color="error"
            onMouseEnter={handleMouseEnter}
            onMouseLeave={handleMouseLeave}
            onClick={() => {
              setAlertOpen(true);
            }}
          >
            <IndeterminateCheckBox />
          </IconButton>
        </Tooltip>
        <CustomizedDialogs
          modalTitle="Confirm Removal"
          modalContent={`Deleting this subject will also delete its keywords. Are you sure you want to remove it?`}
          alertOpen={alertOpen}
          onClose={() => setAlertOpen(false)}
          modalAction={true}
          modalActions={[
            {
              label: "Cancel",
              onClick: () => setAlertOpen(false),
              icon: Delete,
              bgColor: "primary.main",
            },
            {
              label: "Yes",  
              onClick: () => {
                handleRemoveItem(id, index);
                setAlertOpen(false);
              },
              icon: Check,
              bgColor: "error.main",
            }
          ]}
        />
      </Stack>
    </Stack>
  );
}
