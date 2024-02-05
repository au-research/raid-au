/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables;


import au.org.raid.db.jooq.ApiSvc;
import au.org.raid.db.jooq.Keys;
import au.org.raid.db.jooq.tables.records.RaidOrganisationRecord;
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
public class RaidOrganisation extends TableImpl<RaidOrganisationRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>api_svc.raid_organisation</code>
     */
    public static final RaidOrganisation RAID_ORGANISATION = new RaidOrganisation();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<RaidOrganisationRecord> getRecordType() {
        return RaidOrganisationRecord.class;
    }

    /**
     * The column <code>api_svc.raid_organisation.id</code>.
     */
    public final TableField<RaidOrganisationRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>api_svc.raid_organisation.handle</code>.
     */
    public final TableField<RaidOrganisationRecord, String> HANDLE = createField(DSL.name("handle"), SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>api_svc.raid_organisation.organisation_id</code>.
     */
    public final TableField<RaidOrganisationRecord, Integer> ORGANISATION_ID = createField(DSL.name("organisation_id"), SQLDataType.INTEGER.nullable(false), this, "");

    private RaidOrganisation(Name alias, Table<RaidOrganisationRecord> aliased) {
        this(alias, aliased, null);
    }

    private RaidOrganisation(Name alias, Table<RaidOrganisationRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>api_svc.raid_organisation</code> table reference
     */
    public RaidOrganisation(String alias) {
        this(DSL.name(alias), RAID_ORGANISATION);
    }

    /**
     * Create an aliased <code>api_svc.raid_organisation</code> table reference
     */
    public RaidOrganisation(Name alias) {
        this(alias, RAID_ORGANISATION);
    }

    /**
     * Create a <code>api_svc.raid_organisation</code> table reference
     */
    public RaidOrganisation() {
        this(DSL.name("raid_organisation"), null);
    }

    public <O extends Record> RaidOrganisation(Table<O> child, ForeignKey<O, RaidOrganisationRecord> key) {
        super(child, key, RAID_ORGANISATION);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : ApiSvc.API_SVC;
    }

    @Override
    public Identity<RaidOrganisationRecord, Integer> getIdentity() {
        return (Identity<RaidOrganisationRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<RaidOrganisationRecord> getPrimaryKey() {
        return Keys.RAID_ORGANISATION_PKEY;
    }

    @Override
    public List<UniqueKey<RaidOrganisationRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.RAID_ORGANISATION_HANDLE_ORGANISATION_ID_KEY);
    }

    @Override
    public RaidOrganisation as(String alias) {
        return new RaidOrganisation(DSL.name(alias), this);
    }

    @Override
    public RaidOrganisation as(Name alias) {
        return new RaidOrganisation(alias, this);
    }

    @Override
    public RaidOrganisation as(Table<?> alias) {
        return new RaidOrganisation(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidOrganisation rename(String name) {
        return new RaidOrganisation(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidOrganisation rename(Name name) {
        return new RaidOrganisation(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidOrganisation rename(Table<?> name) {
        return new RaidOrganisation(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, Integer> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function3<? super Integer, ? super String, ? super Integer, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function3<? super Integer, ? super String, ? super Integer, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
