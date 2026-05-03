CREATE TEMPORARY TABLE handles_to_delete AS
SELECT DISTINCT r.handle
FROM api_svc.raid r
WHERE r.handle LIKE '102.100.100/%'
   OR r.handle LIKE '10378.1/%'
   OR EXISTS (SELECT 1 FROM api_svc.raid_title rt JOIN api_svc.title_type tt ON rt.title_type_id = tt.id WHERE rt.handle = r.handle AND tt.uri LIKE '%github.com%')
   OR EXISTS (SELECT 1 FROM api_svc.raid_description rd JOIN api_svc.description_type dt ON rd.description_type_id = dt.id WHERE rd.handle = r.handle AND dt.uri LIKE '%github.com%')
   OR EXISTS (SELECT 1 FROM api_svc.raid_contributor rc JOIN api_svc.raid_contributor_position rcp ON rc.id = rcp.raid_contributor_id JOIN api_svc.contributor_position cp ON rcp.contributor_position_id = cp.id WHERE rc.handle = r.handle AND cp.uri LIKE '%github.com%')
   OR EXISTS (SELECT 1 FROM api_svc.raid_organisation ro JOIN api_svc.raid_organisation_role ror ON ro.id = ror.raid_organisation_id JOIN api_svc.organisation_role or2 ON ror.organisation_role_id = or2.id WHERE ro.handle = r.handle AND or2.uri LIKE '%github.com%')
   OR EXISTS (SELECT 1 FROM api_svc.raid_related_object rro JOIN api_svc.related_object_type rot ON rro.related_object_type_id = rot.id WHERE rro.handle = r.handle AND rot.uri LIKE '%github.com%')
   OR EXISTS (SELECT 1 FROM api_svc.raid_related_object rro2 JOIN api_svc.raid_related_object_category rroc ON rro2.id = rroc.raid_related_object_id JOIN api_svc.related_object_category roc ON rroc.related_object_category_id = roc.id WHERE rro2.handle = r.handle AND roc.uri LIKE '%github.com%')
   OR EXISTS (SELECT 1 FROM api_svc.related_raid rr JOIN api_svc.related_raid_type rrt ON rr.related_raid_type_id = rrt.id WHERE rr.handle = r.handle AND rrt.uri LIKE '%github.com%')
   OR EXISTS (SELECT 1 FROM api_svc.raid r2 JOIN api_svc.access_type at2 ON r2.access_type_id = at2.id WHERE r2.handle = r.handle AND at2.uri LIKE '%github.com%')
   OR EXISTS (SELECT 1 FROM api_svc.raid_traditional_knowledge_label rtkl JOIN api_svc.traditional_knowledge_label tkl ON rtkl.traditional_knowledge_label_id = tkl.id WHERE rtkl.handle = r.handle AND tkl.uri LIKE '%localcontexts.org%');

DELETE FROM api_svc.raid_history
WHERE handle IN (SELECT handle FROM handles_to_delete);

DELETE FROM api_svc.raid_title
WHERE handle IN (SELECT handle FROM handles_to_delete);

DELETE FROM api_svc.raid_organisation_role
WHERE raid_organisation_id IN (
    SELECT ro.id FROM api_svc.raid_organisation ro
    WHERE ro.handle IN (SELECT handle FROM handles_to_delete)
);

DELETE FROM api_svc.raid_organisation
WHERE handle IN (SELECT handle FROM handles_to_delete);

DELETE FROM api_svc.raid_subject_keyword
WHERE raid_subject_id IN (
    SELECT rs.id FROM api_svc.raid_subject rs
    WHERE rs.handle IN (SELECT handle FROM handles_to_delete)
);

DELETE FROM api_svc.raid
WHERE handle IN (SELECT handle FROM handles_to_delete);

DROP TABLE handles_to_delete;
