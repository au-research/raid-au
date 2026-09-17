import { describe, it, expect } from "vitest";
import { renderHook, act, waitFor } from "@testing-library/react";

import { classifyRelatedObjectIdentifier, useBulkUpload } from "./useBulkUpload";
import type { BulkUploadVocabulary } from "../types";

// ------------------------------------------------------------------
// Pure classification logic — one case per recognised scheme, plus the
// unrecognised/malformed cases (RAID-801).
// ------------------------------------------------------------------

describe("classifyRelatedObjectIdentifier", () => {
  it.each([
    ["DOI", "https://doi.org/10.5281/zenodo.1234567", "https://doi.org/"],
    ["dx.doi.org DOI", "https://dx.doi.org/10.5281/zenodo.1234567", "https://doi.org/"],
    [
      "Web Archive",
      "https://web.archive.org/web/20220101000000/https://example.com",
      "https://web.archive.org/",
    ],
    [
      "Handle",
      "https://hdl.handle.net/20.500.12345/abc123",
      "https://hdl.handle.net/",
    ],
    [
      "RRID",
      "https://scicrunch.org/resolver/RRID:AB_2298772",
      "https://scicrunch.org/resolver/",
    ],
    [
      "ARK",
      "https://example-repository.edu/ark:/13030/kt6f59n8z3",
      "https://arks.org/",
    ],
  ])("classifies a valid %s identifier", (_label, url, expectedSchemaUri) => {
    expect(classifyRelatedObjectIdentifier(url)).toBe(expectedSchemaUri);
  });

  it.each([
    ["a non-URL string", "not-a-url"],
    ["a generic, unrecognised URL", "https://example.com/some-unrelated-path"],
    ["a malformed Handle (no suffix after the NAAN)", "https://hdl.handle.net/20.500.12345"],
    ["a malformed RRID (missing the underscore-separated value)", "https://scicrunch.org/resolver/RRID:AB"],
    ["a malformed ARK (non-numeric NAAN)", "https://example.edu/ark:/abcde/kt6f59n8z3"],
  ])("returns null for %s", (_label, url) => {
    expect(classifyRelatedObjectIdentifier(url)).toBeNull();
  });
});

// ------------------------------------------------------------------
// Integration — the real parse -> classify -> validate path via the hook,
// covering the same set of schemes plus a bad row in a single file.
// ------------------------------------------------------------------

const VOCABULARY: BulkUploadVocabulary = {
  relatedObjectTypes: [
    { key: "https://vocabulary.raid.org/relatedObject.type.schema/329/1", value: "Dataset" },
  ],
  relatedObjectCategories: [
    { key: "https://vocabulary.raid.org/relatedObject.category.schemaUri/386/1", value: "Output" },
  ],
};

const CSV_HEADER = "Import table starts here\nIdentifier,Type,Categories\n";
const CSV_FOOTER = "\nImport table ends here\n";

function makeCsvFile(rows: string[]): File {
  const csv = CSV_HEADER + rows.join("\n") + CSV_FOOTER;
  return new File([csv], "upload.csv", { type: "text/csv" });
}

describe("useBulkUpload row classification (integration)", () => {
  it("classifies Handle, RRID, and ARK rows as valid, and flags an unrecognised identifier", async () => {
    const { result } = renderHook(() => useBulkUpload(VOCABULARY));

    const file = makeCsvFile([
      "https://hdl.handle.net/20.500.12345/abc123,Dataset,Output",
      "https://scicrunch.org/resolver/RRID:AB_2298772,Dataset,Output",
      "https://example-repository.edu/ark:/13030/kt6f59n8z3,Dataset,Output",
      "https://example.com/some-unrelated-path,Dataset,Output",
    ]);

    await act(async () => {
      await result.current.handleFileUpload(file);
    });

    await waitFor(() => expect(result.current.editableRows).toHaveLength(4));

    const [handleRow, rridRow, arkRow, badRow] = result.current.editableRows;

    expect(handleRow.errors.Identifier).toBeUndefined();
    expect(rridRow.errors.Identifier).toBeUndefined();
    expect(arkRow.errors.Identifier).toBeUndefined();

    // Regression guard: an identifier matching no recognised scheme still
    // fails the same way it did before Handle/RRID/ARK were recognised.
    expect(badRow.errors.Identifier).toMatch(/Must be a valid DOI/);

    expect(result.current.status).toBe("invalid");
  });

  it("still classifies a DOI row as valid (regression guard)", async () => {
    const { result } = renderHook(() => useBulkUpload(VOCABULARY));

    const file = makeCsvFile(["https://doi.org/10.5281/zenodo.1234567,Dataset,Output"]);

    await act(async () => {
      await result.current.handleFileUpload(file);
    });

    await waitFor(() => expect(result.current.editableRows).toHaveLength(1));
    expect(result.current.editableRows[0].errors.Identifier).toBeUndefined();
    expect(result.current.status).toBe("valid");
  });
});
