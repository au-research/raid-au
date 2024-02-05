/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables;


import au.org.raid.db.jooq.ApiSvc;
import au.org.raid.db.jooq.Keys;
import au.org.raid.db.jooq.tables.records.RaidAlternateUrlRecord;
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
public class RaidAlternateUrl extends TableImpl<RaidAlternateUrlRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>api_svc.raid_alternate_url</code>
     */
    public static final RaidAlternateUrl RAID_ALTERNATE_URL = new RaidAlternateUrl();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<RaidAlternateUrlRecord> getRecordType() {
        return RaidAlternateUrlRecord.class;
    }

    /**
     * The column <code>api_svc.raid_alternate_url.handle</code>.
     */
    public final TableField<RaidAlternateUrlRecord, String> HANDLE = createField(DSL.name("handle"), SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>api_svc.raid_alternate_url.url</code>.
     */
    public final TableField<RaidAlternateUrlRecord, String> URL = createField(DSL.name("url"), SQLDataType.VARCHAR.nullable(false), this, "");

    private RaidAlternateUrl(Name alias, Table<RaidAlternateUrlRecord> aliased) {
        this(alias, aliased, null);
    }

    private RaidAlternateUrl(Name alias, Table<RaidAlternateUrlRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>api_svc.raid_alternate_url</code> table reference
     */
    public RaidAlternateUrl(String alias) {
        this(DSL.name(alias), RAID_ALTERNATE_URL);
    }

    /**
     * Create an aliased <code>api_svc.raid_alternate_url</code> table reference
     */
    public RaidAlternateUrl(Name alias) {
        this(alias, RAID_ALTERNATE_URL);
    }

    /**
     * Create a <code>api_svc.raid_alternate_url</code> table reference
     */
    public RaidAlternateUrl() {
        this(DSL.name("raid_alternate_url"), null);
    }

    public <O extends Record> RaidAlternateUrl(Table<O> child, ForeignKey<O, RaidAlternateUrlRecord> key) {
        super(child, key, RAID_ALTERNATE_URL);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : ApiSvc.API_SVC;
    }

    @Override
    public UniqueKey<RaidAlternateUrlRecord> getPrimaryKey() {
        return Keys.RAID_ALTERNATE_URL_PKEY;
    }

    @Override
    public List<ForeignKey<RaidAlternateUrlRecord, ?>> getReferences() {
        return Arrays.asList(Keys.RAID_ALTERNATE_URL__RAID_ALTERNATE_URL_HANDLE_FKEY);
    }

    private transient Raid _raid;

    /**
     * Get the implicit join path to the <code>api_svc.raid</code> table.
     */
    public Raid raid() {
        if (_raid == null)
            _raid = new Raid(this, Keys.RAID_ALTERNATE_URL__RAID_ALTERNATE_URL_HANDLE_FKEY);

        return _raid;
    }

    @Override
    public RaidAlternateUrl as(String alias) {
        return new RaidAlternateUrl(DSL.name(alias), this);
    }

    @Override
    public RaidAlternateUrl as(Name alias) {
        return new RaidAlternateUrl(alias, this);
    }

    @Override
    public RaidAlternateUrl as(Table<?> alias) {
        return new RaidAlternateUrl(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidAlternateUrl rename(String name) {
        return new RaidAlternateUrl(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidAlternateUrl rename(Name name) {
        return new RaidAlternateUrl(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidAlternateUrl rename(Table<?> name) {
        return new RaidAlternateUrl(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<String, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function2<? super String, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function2<? super String, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
