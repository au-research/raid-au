/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables;


import au.org.raid.db.jooq.ApiSvc;
import au.org.raid.db.jooq.Keys;
import au.org.raid.db.jooq.tables.records.DescriptionTypeRecord;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.util.function.Function;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class DescriptionType extends TableImpl<DescriptionTypeRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>api_svc.description_type</code>
     */
    public static final DescriptionType DESCRIPTION_TYPE = new DescriptionType();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DescriptionTypeRecord> getRecordType() {
        return DescriptionTypeRecord.class;
    }

    /**
     * The column <code>api_svc.description_type.id</code>.
     */
    public final TableField<DescriptionTypeRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>api_svc.description_type.uri</code>.
     */
    public final TableField<DescriptionTypeRecord, String> URI = createField(DSL.name("uri"), SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>api_svc.description_type.schema_id</code>.
     */
    public final TableField<DescriptionTypeRecord, Integer> SCHEMA_ID = createField(DSL.name("schema_id"), SQLDataType.INTEGER.nullable(false), this, "");

    private DescriptionType(Name alias, Table<DescriptionTypeRecord> aliased) {
        this(alias, aliased, null);
    }

    private DescriptionType(Name alias, Table<DescriptionTypeRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>api_svc.description_type</code> table reference
     */
    public DescriptionType(String alias) {
        this(DSL.name(alias), DESCRIPTION_TYPE);
    }

    /**
     * Create an aliased <code>api_svc.description_type</code> table reference
     */
    public DescriptionType(Name alias) {
        this(alias, DESCRIPTION_TYPE);
    }

    /**
     * Create a <code>api_svc.description_type</code> table reference
     */
    public DescriptionType() {
        this(DSL.name("description_type"), null);
    }

    public <O extends Record> DescriptionType(Table<O> child, ForeignKey<O, DescriptionTypeRecord> key) {
        super(child, key, DESCRIPTION_TYPE);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : ApiSvc.API_SVC;
    }

    @Override
    public Identity<DescriptionTypeRecord, Integer> getIdentity() {
        return (Identity<DescriptionTypeRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<DescriptionTypeRecord> getPrimaryKey() {
        return Keys.DESCRIPTION_TYPE_NEW_PKEY;
    }

    @Override
    public DescriptionType as(String alias) {
        return new DescriptionType(DSL.name(alias), this);
    }

    @Override
    public DescriptionType as(Name alias) {
        return new DescriptionType(alias, this);
    }

    @Override
    public DescriptionType as(Table<?> alias) {
        return new DescriptionType(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public DescriptionType rename(String name) {
        return new DescriptionType(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public DescriptionType rename(Name name) {
        return new DescriptionType(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public DescriptionType rename(Table<?> name) {
        return new DescriptionType(name.getQualifiedName(), null);
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
