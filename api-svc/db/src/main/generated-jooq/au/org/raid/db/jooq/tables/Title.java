/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables;


import au.org.raid.db.jooq.ApiSvc;
import au.org.raid.db.jooq.Keys;
import au.org.raid.db.jooq.tables.records.TitleRecord;

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
public class Title extends TableImpl<TitleRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>api_svc.title</code>
     */
    public static final Title TITLE = new Title();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<TitleRecord> getRecordType() {
        return TitleRecord.class;
    }

    /**
     * The column <code>api_svc.title.id</code>.
     */
    public final TableField<TitleRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>api_svc.title.raid_name</code>.
     */
    public final TableField<TitleRecord, String> RAID_NAME = createField(DSL.name("raid_name"), SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>api_svc.title.title_type_id</code>.
     */
    public final TableField<TitleRecord, Integer> TITLE_TYPE_ID = createField(DSL.name("title_type_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>api_svc.title.value</code>.
     */
    public final TableField<TitleRecord, String> VALUE = createField(DSL.name("value"), SQLDataType.CLOB, this, "");

    private Title(Name alias, Table<TitleRecord> aliased) {
        this(alias, aliased, null);
    }

    private Title(Name alias, Table<TitleRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>api_svc.title</code> table reference
     */
    public Title(String alias) {
        this(DSL.name(alias), TITLE);
    }

    /**
     * Create an aliased <code>api_svc.title</code> table reference
     */
    public Title(Name alias) {
        this(alias, TITLE);
    }

    /**
     * Create a <code>api_svc.title</code> table reference
     */
    public Title() {
        this(DSL.name("title"), null);
    }

    public <O extends Record> Title(Table<O> child, ForeignKey<O, TitleRecord> key) {
        super(child, key, TITLE);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : ApiSvc.API_SVC;
    }

    @Override
    public Identity<TitleRecord, Integer> getIdentity() {
        return (Identity<TitleRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<TitleRecord> getPrimaryKey() {
        return Keys.TITLE_PKEY;
    }

    @Override
    public List<ForeignKey<TitleRecord, ?>> getReferences() {
        return Arrays.asList(Keys.TITLE__FK_TITLE_RAID_NAME, Keys.TITLE__FK_TITLE_TYPE);
    }

    private transient Raid _raid;
    private transient TitleType _titleType;

    /**
     * Get the implicit join path to the <code>api_svc.raid</code> table.
     */
    public Raid raid() {
        if (_raid == null)
            _raid = new Raid(this, Keys.TITLE__FK_TITLE_RAID_NAME);

        return _raid;
    }

    /**
     * Get the implicit join path to the <code>api_svc.title_type</code> table.
     */
    public TitleType titleType() {
        if (_titleType == null)
            _titleType = new TitleType(this, Keys.TITLE__FK_TITLE_TYPE);

        return _titleType;
    }

    @Override
    public Title as(String alias) {
        return new Title(DSL.name(alias), this);
    }

    @Override
    public Title as(Name alias) {
        return new Title(alias, this);
    }

    @Override
    public Title as(Table<?> alias) {
        return new Title(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Title rename(String name) {
        return new Title(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Title rename(Name name) {
        return new Title(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Title rename(Table<?> name) {
        return new Title(name.getQualifiedName(), null);
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
