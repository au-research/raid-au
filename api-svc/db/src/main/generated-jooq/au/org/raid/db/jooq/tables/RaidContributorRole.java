/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables;


import au.org.raid.db.jooq.ApiSvc;
import au.org.raid.db.jooq.Keys;
import au.org.raid.db.jooq.tables.records.RaidContributorRoleRecord;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class RaidContributorRole extends TableImpl<RaidContributorRoleRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>api_svc.raid_contributor_role</code>
     */
    public static final RaidContributorRole RAID_CONTRIBUTOR_ROLE = new RaidContributorRole();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<RaidContributorRoleRecord> getRecordType() {
        return RaidContributorRoleRecord.class;
    }

    /**
     * The column <code>api_svc.raid_contributor_role.id</code>.
     */
    public final TableField<RaidContributorRoleRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column
     * <code>api_svc.raid_contributor_role.raid_contributor_id</code>.
     */
    public final TableField<RaidContributorRoleRecord, Integer> RAID_CONTRIBUTOR_ID = createField(DSL.name("raid_contributor_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column
     * <code>api_svc.raid_contributor_role.contributor_role_id</code>.
     */
    public final TableField<RaidContributorRoleRecord, Integer> CONTRIBUTOR_ROLE_ID = createField(DSL.name("contributor_role_id"), SQLDataType.INTEGER.nullable(false), this, "");

    private RaidContributorRole(Name alias, Table<RaidContributorRoleRecord> aliased) {
        this(alias, aliased, null);
    }

    private RaidContributorRole(Name alias, Table<RaidContributorRoleRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>api_svc.raid_contributor_role</code> table
     * reference
     */
    public RaidContributorRole(String alias) {
        this(DSL.name(alias), RAID_CONTRIBUTOR_ROLE);
    }

    /**
     * Create an aliased <code>api_svc.raid_contributor_role</code> table
     * reference
     */
    public RaidContributorRole(Name alias) {
        this(alias, RAID_CONTRIBUTOR_ROLE);
    }

    /**
     * Create a <code>api_svc.raid_contributor_role</code> table reference
     */
    public RaidContributorRole() {
        this(DSL.name("raid_contributor_role"), null);
    }

    public <O extends Record> RaidContributorRole(Table<O> child, ForeignKey<O, RaidContributorRoleRecord> key) {
        super(child, key, RAID_CONTRIBUTOR_ROLE);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : ApiSvc.API_SVC;
    }

    @Override
    public Identity<RaidContributorRoleRecord, Integer> getIdentity() {
        return (Identity<RaidContributorRoleRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<RaidContributorRoleRecord> getPrimaryKey() {
        return Keys.RAID_CONTRIBUTOR_ROLE_PKEY;
    }

    @Override
    public List<ForeignKey<RaidContributorRoleRecord, ?>> getReferences() {
        return Arrays.asList(Keys.RAID_CONTRIBUTOR_ROLE__RAID_CONTRIBUTOR_ROLE_RAID_CONTRIBUTOR_ID_FKEY, Keys.RAID_CONTRIBUTOR_ROLE__RAID_CONTRIBUTOR_ROLE_CONTRIBUTOR_ROLE_ID_FKEY);
    }

    private transient RaidContributor _raidContributor;
    private transient ContributorRole _contributorRole;

    /**
     * Get the implicit join path to the <code>api_svc.raid_contributor</code>
     * table.
     */
    public RaidContributor raidContributor() {
        if (_raidContributor == null)
            _raidContributor = new RaidContributor(this, Keys.RAID_CONTRIBUTOR_ROLE__RAID_CONTRIBUTOR_ROLE_RAID_CONTRIBUTOR_ID_FKEY);

        return _raidContributor;
    }

    /**
     * Get the implicit join path to the <code>api_svc.contributor_role</code>
     * table.
     */
    public ContributorRole contributorRole() {
        if (_contributorRole == null)
            _contributorRole = new ContributorRole(this, Keys.RAID_CONTRIBUTOR_ROLE__RAID_CONTRIBUTOR_ROLE_CONTRIBUTOR_ROLE_ID_FKEY);

        return _contributorRole;
    }

    @Override
    public RaidContributorRole as(String alias) {
        return new RaidContributorRole(DSL.name(alias), this);
    }

    @Override
    public RaidContributorRole as(Name alias) {
        return new RaidContributorRole(alias, this);
    }

    @Override
    public RaidContributorRole as(Table<?> alias) {
        return new RaidContributorRole(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidContributorRole rename(String name) {
        return new RaidContributorRole(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidContributorRole rename(Name name) {
        return new RaidContributorRole(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidContributorRole rename(Table<?> name) {
        return new RaidContributorRole(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, Integer, Integer> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function3<? super Integer, ? super Integer, ? super Integer, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function3<? super Integer, ? super Integer, ? super Integer, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
