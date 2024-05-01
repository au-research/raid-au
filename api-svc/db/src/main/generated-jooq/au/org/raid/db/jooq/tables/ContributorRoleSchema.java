/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables;


import au.org.raid.db.jooq.ApiSvc;
import au.org.raid.db.jooq.Keys;
import au.org.raid.db.jooq.enums.SchemaStatus;
import au.org.raid.db.jooq.tables.records.ContributorRoleSchemaRecord;

import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function3;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row3;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ContributorRoleSchema extends TableImpl<ContributorRoleSchemaRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>api_svc.contributor_role_schema</code>
     */
    public static final ContributorRoleSchema CONTRIBUTOR_ROLE_SCHEMA = new ContributorRoleSchema();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ContributorRoleSchemaRecord> getRecordType() {
        return ContributorRoleSchemaRecord.class;
    }

    /**
     * The column <code>api_svc.contributor_role_schema.id</code>.
     */
    public final TableField<ContributorRoleSchemaRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>api_svc.contributor_role_schema.uri</code>.
     */
    public final TableField<ContributorRoleSchemaRecord, String> URI = createField(DSL.name("uri"), SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>api_svc.contributor_role_schema.status</code>.
     */
    public final TableField<ContributorRoleSchemaRecord, SchemaStatus> STATUS = createField(DSL.name("status"), SQLDataType.VARCHAR.asEnumDataType(au.org.raid.db.jooq.enums.SchemaStatus.class), this, "");

    private ContributorRoleSchema(Name alias, Table<ContributorRoleSchemaRecord> aliased) {
        this(alias, aliased, null);
    }

    private ContributorRoleSchema(Name alias, Table<ContributorRoleSchemaRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>api_svc.contributor_role_schema</code> table
     * reference
     */
    public ContributorRoleSchema(String alias) {
        this(DSL.name(alias), CONTRIBUTOR_ROLE_SCHEMA);
    }

    /**
     * Create an aliased <code>api_svc.contributor_role_schema</code> table
     * reference
     */
    public ContributorRoleSchema(Name alias) {
        this(alias, CONTRIBUTOR_ROLE_SCHEMA);
    }

    /**
     * Create a <code>api_svc.contributor_role_schema</code> table reference
     */
    public ContributorRoleSchema() {
        this(DSL.name("contributor_role_schema"), null);
    }

    public <O extends Record> ContributorRoleSchema(Table<O> child, ForeignKey<O, ContributorRoleSchemaRecord> key) {
        super(child, key, CONTRIBUTOR_ROLE_SCHEMA);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : ApiSvc.API_SVC;
    }

    @Override
    public Identity<ContributorRoleSchemaRecord, Integer> getIdentity() {
        return (Identity<ContributorRoleSchemaRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<ContributorRoleSchemaRecord> getPrimaryKey() {
        return Keys.CONTRIBUTOR_ROLE_SCHEMA_PKEY;
    }

    @Override
    public ContributorRoleSchema as(String alias) {
        return new ContributorRoleSchema(DSL.name(alias), this);
    }

    @Override
    public ContributorRoleSchema as(Name alias) {
        return new ContributorRoleSchema(alias, this);
    }

    @Override
    public ContributorRoleSchema as(Table<?> alias) {
        return new ContributorRoleSchema(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public ContributorRoleSchema rename(String name) {
        return new ContributorRoleSchema(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ContributorRoleSchema rename(Name name) {
        return new ContributorRoleSchema(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public ContributorRoleSchema rename(Table<?> name) {
        return new ContributorRoleSchema(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, SchemaStatus> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function3<? super Integer, ? super String, ? super SchemaStatus, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function3<? super Integer, ? super String, ? super SchemaStatus, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
