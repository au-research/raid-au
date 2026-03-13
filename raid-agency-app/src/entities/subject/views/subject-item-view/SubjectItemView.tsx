import { DisplayItem } from "@/components/display-item";
import { SubjectKeywordItemView } from "@/entities/subject-keyword/views/subject-keyword-item-view";
import { Subject } from "@/generated/raid";
import { Divider, Grid, Stack, Typography } from "@mui/material";

import { memo, useEffect, useMemo, useState } from "react";

const SubjectItemiew = memo(({ subject, i }: { subject: Subject; i: number }) => {
  const [subjectName, setSubjectName] = useState<string>("");
  /**
 * Builds the ARDC vocabulary API URL based on the subject ID
 * Supports: anzsrc-for, anzsrc-seo, and other ARDC vocabularies
 */
  function buildVocabApiUrl(subjectId: string): string {
    const baseUrl = "https://vocabs.ardc.edu.au/repository/api/lda";

    // Extract vocab type and year from the ID
    const match = subjectId.match(/\/def\/([^\/]+)\/(\d{4})\//);

    if (!match) {
      throw new Error(`Invalid vocabulary ID format: ${subjectId}`);
    }

    const [, vocabType, year] = match;

    // Handle the vocab identifier construction
    // Most ARDC vocabs follow pattern: {vocab-name}-{year}-{suffix}
    // e.g., anzsrc-for → anzsrc-2020-for
    //       anzsrc-seo → anzsrc-2020-seo
    let vocabIdentifier: string;

    if (vocabType.startsWith('anzsrc-')) {
      // For ANZSRC vocabs, insert year between prefix and suffix
      const suffix = vocabType.replace('anzsrc-', '');
      vocabIdentifier = `anzsrc-${year}-${suffix}`;
    } else {
      // For other vocabs, append year
      vocabIdentifier = `${vocabType}-${year}`;
    }

    return `${baseUrl}/${vocabIdentifier}/resource.json?uri=${encodeURIComponent(subjectId)}`;
  }

  useEffect(() => {
    const fetchData = async () => {
      const apiUrl = buildVocabApiUrl(subject?.id || "");
      const response = await fetch(
        apiUrl,
        {
          headers: {
            'Accept': 'application/json'
          }
        }
      );
      const data = await response.json();
      setSubjectName(data.result.primaryTopic.prefLabel._value.toLowerCase().replace(/\b\w/, (c: string) => c.toUpperCase()));
    };
    fetchData();
  }, []);

  return (
    <Stack gap={2}>
      <Typography variant="body1">Subject #{i + 1}</Typography>
      <Grid container spacing={2}>
        <DisplayItem label="Subject" value={subjectName} width={12} />
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
