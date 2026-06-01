begin transaction;

alter table only raid_contributor_position
    drop constraint raid_contributor_position_raid_contributor_id_contributor_p_key;

alter table only raid_contributor_role
    drop constraint raid_contributor_role_raid_contributor_id_contributor_role__key;

alter table only raid_organisation_role
    drop constraint raid_organisation_role_raid_organisation_id_organisation_ro_key;

alter table only raid_related_object
    drop constraint raid_related_object_handle_related_object_id_related_object_key;

alter table only raid_related_object_category
    drop constraint raid_related_object_category_raid_related_object_id_related_key;

alter table only raid_subject
    drop constraint raid_subject_handle_subject_type_id_key;

alter table only raid_subject_keyword
    drop constraint raid_subject_keyword_raid_subject_id_keyword_language_id_key;

end transaction;
