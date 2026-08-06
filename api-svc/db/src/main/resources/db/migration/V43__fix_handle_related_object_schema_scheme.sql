-- RAID-786: V29 seeded the related_object_schema vocabulary with the Handle
-- resolver as 'http://hdl.handle.net/', but RelatedObjectSchemaUriEnum, the
-- Handle validator, and DataciteRelatedIdentifierFactory all use the canonical
-- 'https://hdl.handle.net/'. Handle was never an accepted relatedObject
-- schemaUri before RAID-786 (the validator allow-list only permitted doi.org
-- and web.archive.org), so the http:// row is orphaned and no relatedObject
-- references it. Correcting the scheme lets Handle-scoped related objects
-- persist via RelatedObjectService.create's findByUri lookup.
update related_object_schema
set uri = 'https://hdl.handle.net/'
where uri = 'http://hdl.handle.net/'
  and not exists (
    select 1 from related_object_schema where uri = 'https://hdl.handle.net/'
  );
