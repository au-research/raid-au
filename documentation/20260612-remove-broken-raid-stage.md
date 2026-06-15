# Remove broken raid from stage database

## What changed

### Problem

The stage API's `/raid/` endpoint returned HTTP 500 because one raid (`10.82841/5c9d5cd8`) had two separate issues:

1. **NULL fields**: `start_date`, `end_date`, and `confidential` were NULL, causing serialisation failures when Jackson tried to build the response.

2. **Invalid enum value in metadata JSONB**: The `metadata` column contained a `traditionalKnowledgeLabel` with schema URI `https://localcontexts.org/labels/biocultural-labels/`, but the generated `TraditionalKnowledgeLabelSchemaUriEnum` only defines `https://localcontexts.org/labels/traditional-knowledge-labels/`. The `RaidIngestService.findAll()` code path deserialises the JSONB via `objectMapper.readValue()`, which threw `IllegalArgumentException` on the unrecognised enum value.

### Root cause

The DB migration `V24__traditional_knowledge_labels.sql` defines two schema URIs (traditional-knowledge-labels and biocultural-labels), but the OpenAPI-generated enum only includes the traditional-knowledge-labels value. The raid in question was the only one using biocultural-labels.

### Fix

Created a stage-only Flyway migration `V41.1__remove_broken_raid.sql` in `db/env/stage/` that deletes the broken raid and all its related data across 17 FK-dependent tables in the correct cascade order.

Verified locally against a stage DB dump: all 42 remaining raids return successfully from `/raid/`.

The underlying enum mismatch (missing biocultural-labels in `TraditionalKnowledgeLabelSchemaUriEnum`) was not fixed — the decision was to remove the problematic data rather than extend the enum, since this was the only affected raid.

## JIRA tickets

- Parent: [RAID-461](https://ardc.atlassian.net/browse/RAID-461)

## PR

- [#517](https://github.com/au-research/raid-au/pull/517) on branch `feature/RAID-461`
