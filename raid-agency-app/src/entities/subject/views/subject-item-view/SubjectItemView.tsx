import { DisplayItem } from "@/components/display-item";
import { SubjectKeywordItemView } from "@/entities/subject-keyword/views/subject-keyword-item-view";
import { Subject } from "@/generated/raid";
import { useMapping } from "@/mapping";
import { Divider, Grid, Stack, Typography } from "@mui/material";

import { memo, useMemo } from "react";

const SubjectItemiew = memo(({ subject, i }: { subject: Subject; i: number }) => {
  const { subjectMap } = useMapping();

  const extractedSubjectId = useMemo(
    () =>
      subject?.id?.replace(
        "https://linked.data.gov.au/def/anzsrc-for/2020/",
        ""
      ) || "",
    [subject?.id]
  );

  const subjectMappedValue = useMemo(
    () => subjectMap.get(String(extractedSubjectId)) ?? "",
    [extractedSubjectId, subjectMap]
  );

  return (
    <Stack gap={2}>
      <Typography variant="body1">Subject #{i + 1}</Typography>

      <Grid container spacing={2}>
        <DisplayItem label="Subject" value={subjectMappedValue} width={12} />
      </Grid>
      <Stack gap={2} sx={{ pl: 3 }}>
        <Stack direction="row" alignItems="baseline">
          <Typography variant="body1" sx={{ mr: 0.5 }}>
            Keywords
          </Typography>
          <Typography variant="caption" color="text.disabled">
            Subject #{i + 1}
          </Typography>
        </Stack>
        <Stack gap={2} divider={<Divider />}>
          {subject?.keyword?.map((subjectKeyword, j) => (
            <SubjectKeywordItemView
              subjectKeyword={subjectKeyword}
              key={`subject=${i}-keyword-${j}`}
            />
          ))}
        </Stack>
      </Stack>
    </Stack>
  );
});

SubjectItemiew.displayName = "SubjectItemiew";

export { SubjectItemiew };
