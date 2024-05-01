/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables;


import au.org.raid.db.jooq.ApiSvc;
import au.org.raid.db.jooq.Keys;
import au.org.raid.db.jooq.enums.SchemaStatus;
import au.org.raid.db.jooq.tables.records.RelatedObjectTypeSchemaRecord;

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
public class RelatedObjectTypeSchema extends TableImpl<RelatedObjectTypeSchemaRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>api_svc.related_object_type_schema</code>
     */
    public static final RelatedObjectTypeSchema RELATED_OBJECT_TYPE_SCHEMA = new RelatedObjectTypeSchema();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<RelatedObjectTypeSchemaRecord> getRecordType() {
        return RelatedObjectTypeSchemaRecord.class;
    }

    /**
     * The column <code>api_svc.related_object_type_schema.id</code>.
     */
    public final TableField<RelatedObjectTypeSchemaRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>api_svc.related_object_type_schema.uri</code>.
     */
    public final TableField<RelatedObjectTypeSchemaRecord, String> URI = createField(DSL.name("uri"), SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>api_svc.related_object_type_schema.status</code>.
     */
    public final TableField<RelatedObjectTypeSchemaRecord, SchemaStatus> STATUS = createField(DSL.name("status"), SQLDataType.VARCHAR.asEnumDataType(au.org.raid.db.jooq.enums.SchemaStatus.class), this, "");

    private RelatedObjectTypeSchema(Name alias, Table<RelatedObjectTypeSchemaRecord> aliased) {
        this(alias, aliased, null);
    }

    private RelatedObjectTypeSchema(Name alias, Table<RelatedObjectTypeSchemaRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>api_svc.related_object_type_schema</code> table
     * reference
     */
    public RelatedObjectTypeSchema(String alias) {
        this(DSL.name(alias), RELATED_OBJECT_TYPE_SCHEMA);
    }

    /**
     * Create an aliased <code>api_svc.related_object_type_schema</code> table
     * reference
     */
    public RelatedObjectTypeSchema(Name alias) {
        this(alias, RELATED_OBJECT_TYPE_SCHEMA);
    }

    /**
     * Create a <code>api_svc.related_object_type_schema</code> table reference
     */
    public RelatedObjectTypeSchema() {
        this(DSL.name("related_object_type_schema"), null);
    }

    public <O extends Record> RelatedObjectTypeSchema(Table<O> child, ForeignKey<O, RelatedObjectTypeSchemaRecord> key) {
        super(child, key, RELATED_OBJECT_TYPE_SCHEMA);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : ApiSvc.API_SVC;
    }

    @Override
    public Identity<RelatedObjectTypeSchemaRecord, Integer> getIdentity() {
        return (Identity<RelatedObjectTypeSchemaRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<RelatedObjectTypeSchemaRecord> getPrimaryKey() {
        return Keys.RELATED_OBJECT_TYPE_SCHEMA_PKEY;
    }

    @Override
    public RelatedObjectTypeSchema as(String alias) {
        return new RelatedObjectTypeSchema(DSL.name(alias), this);
    }

    @Override
    public RelatedObjectTypeSchema as(Name alias) {
        return new RelatedObjectTypeSchema(alias, this);
    }

    @Override
    public RelatedObjectTypeSchema as(Table<?> alias) {
        return new RelatedObjectTypeSchema(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public RelatedObjectTypeSchema rename(String name) {
        return new RelatedObjectTypeSchema(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public RelatedObjectTypeSchema rename(Name name) {
        return new RelatedObjectTypeSchema(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public RelatedObjectTypeSchema rename(Table<?> name) {
        return new RelatedObjectTypeSchema(name.getQualifiedName(), null);
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
