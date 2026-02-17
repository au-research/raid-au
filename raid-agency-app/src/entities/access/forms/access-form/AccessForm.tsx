import LanguageSelector from "@/components/fields/LanguageSelector";
import { TextInputField } from "@/components/fields/TextInputField";
import { TextSelectField } from "@/components/fields/TextSelectField";
import { RaidDto } from "@/generated/raid";
import generalMapping from "@/mapping/data/general-mapping.json";
import { Card, CardContent, CardHeader, Grid, Stack } from "@mui/material";
import { memo, useContext, useEffect } from "react";
import {
  Control,
  FieldErrors,
  useFormContext,
  UseFormTrigger,
  useWatch,
} from "react-hook-form";
import { MetadataContext } from "@/components/raid-form/RaidForm";
import { CustomStyledTooltip } from "@/components/tooltips/StyledTooltip";
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import languageSchema from "@/references/language_schema.json";

const AccessForm = memo(
  ({
    control,
    errors,
  }: {
    control: Control<RaidDto>;
    errors: FieldErrors<RaidDto>;
    trigger: UseFormTrigger<RaidDto>;
  }) => {
    const key = "access";
    const label = "Access";
    const labelPlural = "Access";

    const accessTypeId = useWatch({
      control,
      name: "access.type.id",
    });
    const { setValue, watch } = useFormContext();
    const schemaUriPath = `access.language.schemaUri`;
    const currentSchemaUri = watch(schemaUriPath);

    useEffect(() => {
      const embargoed = accessTypeId?.includes("c_f1cf/");
      if (!currentSchemaUri && embargoed && languageSchema?.[0]?.uri) {
        setValue(schemaUriPath, languageSchema[0].uri);
      }
    }, [currentSchemaUri, setValue, schemaUriPath, accessTypeId]);

    const accessTypeOptions = generalMapping
      .filter((el) => el.field === "access.type.id")
      .map((el) => ({
        value: el.key,
        label: el.value,
      }));
    const metadata = useContext(MetadataContext);
    const tooltip = metadata?.[key]?.tooltip;
    return (
      <Card
        sx={{
          borderLeft: errors[key] ? "3px solid" : "none",
          borderLeftColor: "error.main",
        }}
        id={key}
      >
        <Stack direction="row" alignItems="center">
          <CardHeader sx={{padding: "16px 0 16px 16px"}} title={labelPlural} />
          <CustomStyledTooltip
            title={label}
            content={tooltip || ""}
            variant="info"
            placement="top"
            tooltipIcon={<InfoOutlinedIcon />}
          >
          </CustomStyledTooltip>
        </Stack>
        <CardContent>
          <Grid container spacing={2}>
            <TextSelectField
              options={accessTypeOptions}
              name={`access.type.id`}
              label="Access Type"
              required={true}
              width={6}
            />
            {/* Only whow embargo related fields if access type is c_f1cf (embargoed) */}
            {accessTypeId?.includes("c_f1cf/") && (
              <>
                <TextInputField
                  name={`access.statement.text`}
                  label="Access Statement"
                  required={true}
                  width={12}
                />
                <LanguageSelector
                  name={`access.statement.language.id`}
                  width={6}
                />

                <TextInputField
                  name={`access.embargoExpiry`}
                  label="Embargo Expiry Date"
                  placeholder="Embargo Expiry Date"
                  required={true}
                  width={6}
                />
              </>
            )}
          </Grid>
        </CardContent>
      </Card>
    );
  }
);

AccessForm.displayName = "AccessForm";
export { AccessForm };
