-- A trivial migration set for FlywayRepairIntegrationTest, kept separate from the
-- real migrations so the repair-token behaviour can be exercised without dragging
-- in the whole schema.
create table repair_probe (
    id bigint primary key
);
