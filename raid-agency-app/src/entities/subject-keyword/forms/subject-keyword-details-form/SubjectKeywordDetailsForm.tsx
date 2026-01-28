import { useEffect } from "react";
import LanguageSelector from "@/components/fields/LanguageSelector";
import { TextInputField } from "@/components/fields/TextInputField";
import { IndeterminateCheckBox } from "@mui/icons-material";
import { Grid, IconButton, Stack, Tooltip, Typography } from "@mui/material";
import { useState } from "react";
import { useFormContext } from "react-hook-form";
import languageSchema from "@/references/language_schema.json";
import { Check, Delete } from "lucide-react";
import CustomizedDialogs from "@/components/alert-dialog/alert-dialog";

function FieldGrid({
  parentIndex,
  index,
  isRowHighlighted,
}: {
  parentIndex: number;
  index: number;
  isRowHighlighted: boolean;
}) {
  const { setValue, watch } = useFormContext();

  const schemaUriPath = `subject.${parentIndex}.keyword.${index}.language.schemaUri`;
  const currentSchemaUri = watch(schemaUriPath);

  useEffect(() => {
    if (!currentSchemaUri && languageSchema?.[0]?.uri) {
      setValue(schemaUriPath, languageSchema[0].uri);
    }
  }, [currentSchemaUri, setValue, schemaUriPath]);

  return (
    <Grid container spacing={2} className={isRowHighlighted ? "remove" : ""}>
      <TextInputField
        name={`subject.${parentIndex}.keyword.${index}.text`}
        label="Keyword"
        placeholder="Keyword"
        required={true}
        width={6}
      />
      <LanguageSelector
        name={`subject.${parentIndex}.keyword.${index}.language.id`}
        width={6}
        required={false}
      />
    </Grid>
  );
}

export function SubjectKeywordDetailsForm({
  parentIndex,
  index,
  handleRemoveItem,
}: {
  parentIndex: number;
  index: number;
  handleRemoveItem: (index: number) => void;
}) {
  const key = `subject.${parentIndex}.keyword.${index}`;
  const label = "Subject Keyword";
  const [isRowHighlighted, setIsRowHighlighted] = useState(false);
  const { getValues } = useFormContext();
  
  const handleMouseEnter = () => setIsRowHighlighted(true);
  const handleMouseLeave = () => setIsRowHighlighted(false);
  const [alertOpen, setAlertOpen] = useState(false);
  const currentValue = getValues(`${key}.text`);

  return (
    <Stack gap={2}>
      <Typography variant="body2">
        <span
          style={{ textDecoration: isRowHighlighted ? "line-through" : "" }}
        >
          {currentValue ? currentValue : `${label} # ${index + 1}`}
        </span>
        {isRowHighlighted && " (to be deleted)"}
      </Typography>
      <Stack direction="row" alignItems="flex-start" gap={1}>
        <FieldGrid
          parentIndex={parentIndex}
          index={index}
          isRowHighlighted={isRowHighlighted}
        />
        <Tooltip title={`Remove ${label}`} placement="right">
          <IconButton
            aria-label="delete"
            color="error"
            onMouseEnter={handleMouseEnter}
            onMouseLeave={handleMouseLeave}
            onClick={() => setAlertOpen(true)}
          >
            <IndeterminateCheckBox />
          </IconButton>
        </Tooltip>
        <CustomizedDialogs
          modalTitle="Confirm Removal"
          modalContent={`Are you sure you want to remove ${label} # ${index + 1}?`}
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
                handleRemoveItem(index);
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
