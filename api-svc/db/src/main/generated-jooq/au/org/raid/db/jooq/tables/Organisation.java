/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables;


import au.org.raid.db.jooq.ApiSvc;
import au.org.raid.db.jooq.Keys;
import au.org.raid.db.jooq.tables.records.OrganisationRecord;
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
public class Organisation extends TableImpl<OrganisationRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>api_svc.organisation</code>
     */
    public static final Organisation ORGANISATION = new Organisation();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<OrganisationRecord> getRecordType() {
        return OrganisationRecord.class;
    }

    /**
     * The column <code>api_svc.organisation.id</code>.
     */
    public final TableField<OrganisationRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>api_svc.organisation.pid</code>.
     */
    public final TableField<OrganisationRecord, String> PID = createField(DSL.name("pid"), SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>api_svc.organisation.schema_id</code>.
     */
    public final TableField<OrganisationRecord, Integer> SCHEMA_ID = createField(DSL.name("schema_id"), SQLDataType.INTEGER.nullable(false), this, "");

    private Organisation(Name alias, Table<OrganisationRecord> aliased) {
        this(alias, aliased, null);
    }

    private Organisation(Name alias, Table<OrganisationRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>api_svc.organisation</code> table reference
     */
    public Organisation(String alias) {
        this(DSL.name(alias), ORGANISATION);
    }

    /**
     * Create an aliased <code>api_svc.organisation</code> table reference
     */
    public Organisation(Name alias) {
        this(alias, ORGANISATION);
    }

    /**
     * Create a <code>api_svc.organisation</code> table reference
     */
    public Organisation() {
        this(DSL.name("organisation"), null);
    }

    public <O extends Record> Organisation(Table<O> child, ForeignKey<O, OrganisationRecord> key) {
        super(child, key, ORGANISATION);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : ApiSvc.API_SVC;
    }

    @Override
    public Identity<OrganisationRecord, Integer> getIdentity() {
        return (Identity<OrganisationRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<OrganisationRecord> getPrimaryKey() {
        return Keys.ORGANISATION_PKEY;
    }

    @Override
    public List<UniqueKey<OrganisationRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.ORGANISATION_PID_SCHEMA_ID_KEY);
    }

    @Override
    public List<ForeignKey<OrganisationRecord, ?>> getReferences() {
        return Arrays.asList(Keys.ORGANISATION__ORGANISATION_SCHEMA_ID_FKEY);
    }

    private transient OrganisationSchema _organisationSchema;

    /**
     * Get the implicit join path to the
     * <code>api_svc.organisation_schema</code> table.
     */
    public OrganisationSchema organisationSchema() {
        if (_organisationSchema == null)
            _organisationSchema = new OrganisationSchema(this, Keys.ORGANISATION__ORGANISATION_SCHEMA_ID_FKEY);

        return _organisationSchema;
    }

    @Override
    public Organisation as(String alias) {
        return new Organisation(DSL.name(alias), this);
    }

    @Override
    public Organisation as(Name alias) {
        return new Organisation(alias, this);
    }

    @Override
    public Organisation as(Table<?> alias) {
        return new Organisation(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Organisation rename(String name) {
        return new Organisation(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Organisation rename(Name name) {
        return new Organisation(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Organisation rename(Table<?> name) {
        return new Organisation(name.getQualifiedName(), null);
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
