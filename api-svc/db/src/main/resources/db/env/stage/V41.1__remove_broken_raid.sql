-- Remove raid 10.82841/5c9d5cd8 which has NULL start_date, end_date, and
-- confidential fields, causing HTTP 500 on serialisation.

DO $$
DECLARE
    v_handle CONSTANT VARCHAR := '10.82841/5c9d5cd8';
BEGIN
    -- Grandchild tables (depend on child table IDs)
    DELETE FROM raid_contributor_position WHERE raid_contributor_id IN
        (SELECT id FROM raid_contributor WHERE handle = v_handle);
    DELETE FROM raid_contributor_role WHERE raid_contributor_id IN
        (SELECT id FROM raid_contributor WHERE handle = v_handle);
    DELETE FROM raid_organisation_role WHERE raid_organisation_id IN
        (SELECT id FROM raid_organisation WHERE handle = v_handle);
    DELETE FROM raid_related_object_category WHERE raid_related_object_id IN
        (SELECT id FROM raid_related_object WHERE handle = v_handle);
    DELETE FROM raid_spatial_coverage_place WHERE raid_spatial_coverage_id IN
        (SELECT id FROM raid_spatial_coverage WHERE handle = v_handle);
    DELETE FROM raid_subject_keyword WHERE raid_subject_id IN
        (SELECT id FROM raid_subject WHERE handle = v_handle);

    -- Child tables (FK on handle)
    DELETE FROM raid_contributor WHERE handle = v_handle;
    DELETE FROM raid_organisation WHERE handle = v_handle;
    DELETE FROM raid_related_object WHERE handle = v_handle;
    DELETE FROM raid_spatial_coverage WHERE handle = v_handle;
    DELETE FROM raid_subject WHERE handle = v_handle;
    DELETE FROM raid_title WHERE handle = v_handle;
    DELETE FROM raid_description WHERE handle = v_handle;
    DELETE FROM raid_alternate_identifier WHERE handle = v_handle;
    DELETE FROM raid_alternate_url WHERE handle = v_handle;
    DELETE FROM raid_traditional_knowledge_label WHERE handle = v_handle;
    DELETE FROM related_raid WHERE handle = v_handle;

    -- History
    DELETE FROM raid_history WHERE handle = v_handle;

    -- Parent
    DELETE FROM raid WHERE handle = v_handle;
END $$;
