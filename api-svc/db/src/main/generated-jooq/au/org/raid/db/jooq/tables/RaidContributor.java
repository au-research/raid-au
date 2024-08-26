/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables;


import au.org.raid.db.jooq.ApiSvc;
import au.org.raid.db.jooq.Keys;
import au.org.raid.db.jooq.tables.records.RaidContributorRecord;
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
public class RaidContributor extends TableImpl<RaidContributorRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>api_svc.raid_contributor</code>
     */
    public static final RaidContributor RAID_CONTRIBUTOR = new RaidContributor();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<RaidContributorRecord> getRecordType() {
        return RaidContributorRecord.class;
    }

    /**
     * The column <code>api_svc.raid_contributor.id</code>.
     */
    public final TableField<RaidContributorRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>api_svc.raid_contributor.handle</code>.
     */
    public final TableField<RaidContributorRecord, String> HANDLE = createField(DSL.name("handle"), SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>api_svc.raid_contributor.contributor_id</code>.
     */
    public final TableField<RaidContributorRecord, Integer> CONTRIBUTOR_ID = createField(DSL.name("contributor_id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>api_svc.raid_contributor.leader</code>.
     */
    public final TableField<RaidContributorRecord, Boolean> LEADER = createField(DSL.name("leader"), SQLDataType.BOOLEAN, this, "");

    /**
     * The column <code>api_svc.raid_contributor.contact</code>.
     */
    public final TableField<RaidContributorRecord, Boolean> CONTACT = createField(DSL.name("contact"), SQLDataType.BOOLEAN, this, "");

    private RaidContributor(Name alias, Table<RaidContributorRecord> aliased) {
        this(alias, aliased, null);
    }

    private RaidContributor(Name alias, Table<RaidContributorRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>api_svc.raid_contributor</code> table reference
     */
    public RaidContributor(String alias) {
        this(DSL.name(alias), RAID_CONTRIBUTOR);
    }

    /**
     * Create an aliased <code>api_svc.raid_contributor</code> table reference
     */
    public RaidContributor(Name alias) {
        this(alias, RAID_CONTRIBUTOR);
    }

    /**
     * Create a <code>api_svc.raid_contributor</code> table reference
     */
    public RaidContributor() {
        this(DSL.name("raid_contributor"), null);
    }

    public <O extends Record> RaidContributor(Table<O> child, ForeignKey<O, RaidContributorRecord> key) {
        super(child, key, RAID_CONTRIBUTOR);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : ApiSvc.API_SVC;
    }

    @Override
    public Identity<RaidContributorRecord, Integer> getIdentity() {
        return (Identity<RaidContributorRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<RaidContributorRecord> getPrimaryKey() {
        return Keys.RAID_CONTRIBUTOR_PKEY;
    }

    @Override
    public List<ForeignKey<RaidContributorRecord, ?>> getReferences() {
        return Arrays.asList(Keys.RAID_CONTRIBUTOR__RAID_CONTRIBUTOR_HANDLE_FKEY, Keys.RAID_CONTRIBUTOR__RAID_CONTRIBUTOR_CONTRIBUTOR_ID_FKEY);
    }

    private transient Raid _raid;
    private transient Contributor _contributor;

    /**
     * Get the implicit join path to the <code>api_svc.raid</code> table.
     */
    public Raid raid() {
        if (_raid == null)
            _raid = new Raid(this, Keys.RAID_CONTRIBUTOR__RAID_CONTRIBUTOR_HANDLE_FKEY);

        return _raid;
    }

    /**
     * Get the implicit join path to the <code>api_svc.contributor</code> table.
     */
    public Contributor contributor() {
        if (_contributor == null)
            _contributor = new Contributor(this, Keys.RAID_CONTRIBUTOR__RAID_CONTRIBUTOR_CONTRIBUTOR_ID_FKEY);

        return _contributor;
    }

    @Override
    public RaidContributor as(String alias) {
        return new RaidContributor(DSL.name(alias), this);
    }

    @Override
    public RaidContributor as(Name alias) {
        return new RaidContributor(alias, this);
    }

    @Override
    public RaidContributor as(Table<?> alias) {
        return new RaidContributor(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidContributor rename(String name) {
        return new RaidContributor(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidContributor rename(Name name) {
        return new RaidContributor(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidContributor rename(Table<?> name) {
        return new RaidContributor(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row5<Integer, String, Integer, Boolean, Boolean> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function5<? super Integer, ? super String, ? super Integer, ? super Boolean, ? super Boolean, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function5<? super Integer, ? super String, ? super Integer, ? super Boolean, ? super Boolean, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
