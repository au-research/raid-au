/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables;


import au.org.raid.db.jooq.ApiSvc;
import au.org.raid.db.jooq.Keys;
import au.org.raid.db.jooq.tables.records.DescriptionRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function4;
import org.jooq.Identity;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row4;
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
public class Description extends TableImpl<DescriptionRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>api_svc.description</code>
     */
    public static final Description DESCRIPTION = new Description();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<DescriptionRecord> getRecordType() {
        return DescriptionRecord.class;
    }

    /**
     * The column <code>api_svc.description.id</code>.
     */
    public final TableField<DescriptionRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>api_svc.description.raid_name</code>.
     */
    public final TableField<DescriptionRecord, String> RAID_NAME = createField(DSL.name("raid_name"), SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>api_svc.description.description_type_id</code>.
     */
    public final TableField<DescriptionRecord, Integer> DESCRIPTION_TYPE_ID = createField(DSL.name("description_type_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>api_svc.description.value</code>.
     */
    public final TableField<DescriptionRecord, String> VALUE = createField(DSL.name("value"), SQLDataType.CLOB, this, "");

    private Description(Name alias, Table<DescriptionRecord> aliased) {
        this(alias, aliased, null);
    }

    private Description(Name alias, Table<DescriptionRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>api_svc.description</code> table reference
     */
    public Description(String alias) {
        this(DSL.name(alias), DESCRIPTION);
    }

    /**
     * Create an aliased <code>api_svc.description</code> table reference
     */
    public Description(Name alias) {
        this(alias, DESCRIPTION);
    }

    /**
     * Create a <code>api_svc.description</code> table reference
     */
    public Description() {
        this(DSL.name("description"), null);
    }

    public <O extends Record> Description(Table<O> child, ForeignKey<O, DescriptionRecord> key) {
        super(child, key, DESCRIPTION);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : ApiSvc.API_SVC;
    }

    @Override
    public Identity<DescriptionRecord, Integer> getIdentity() {
        return (Identity<DescriptionRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<DescriptionRecord> getPrimaryKey() {
        return Keys.DESCRIPTION_PKEY;
    }

    @Override
    public List<ForeignKey<DescriptionRecord, ?>> getReferences() {
        return Arrays.asList(Keys.DESCRIPTION__FK_DESCRIPTION_RAID_NAME, Keys.DESCRIPTION__FK_DESCRIPTION_TYPE);
    }

    private transient Raid _raid;
    private transient DescriptionType _descriptionType;

    /**
     * Get the implicit join path to the <code>api_svc.raid</code> table.
     */
    public Raid raid() {
        if (_raid == null)
            _raid = new Raid(this, Keys.DESCRIPTION__FK_DESCRIPTION_RAID_NAME);

        return _raid;
    }

    /**
     * Get the implicit join path to the <code>api_svc.description_type</code>
     * table.
     */
    public DescriptionType descriptionType() {
        if (_descriptionType == null)
            _descriptionType = new DescriptionType(this, Keys.DESCRIPTION__FK_DESCRIPTION_TYPE);

        return _descriptionType;
    }

    @Override
    public Description as(String alias) {
        return new Description(DSL.name(alias), this);
    }

    @Override
    public Description as(Name alias) {
        return new Description(alias, this);
    }

    @Override
    public Description as(Table<?> alias) {
        return new Description(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Description rename(String name) {
        return new Description(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Description rename(Name name) {
        return new Description(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Description rename(Table<?> name) {
        return new Description(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row4 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, String, Integer, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function4<? super Integer, ? super String, ? super Integer, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function4<? super Integer, ? super String, ? super Integer, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
