/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables;


import au.org.raid.db.jooq.ApiSvc;
import au.org.raid.db.jooq.Keys;
import au.org.raid.db.jooq.tables.records.ContributorRecord;

import java.util.Arrays;
import java.util.List;
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
public class Contributor extends TableImpl<ContributorRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>api_svc.contributor</code>
     */
    public static final Contributor CONTRIBUTOR = new Contributor();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ContributorRecord> getRecordType() {
        return ContributorRecord.class;
    }

    /**
     * The column <code>api_svc.contributor.id</code>.
     */
    public final TableField<ContributorRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>api_svc.contributor.pid</code>.
     */
    public final TableField<ContributorRecord, String> PID = createField(DSL.name("pid"), SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>api_svc.contributor.schema_id</code>.
     */
    public final TableField<ContributorRecord, Integer> SCHEMA_ID = createField(DSL.name("schema_id"), SQLDataType.INTEGER.nullable(false), this, "");

    private Contributor(Name alias, Table<ContributorRecord> aliased) {
        this(alias, aliased, null);
    }

    private Contributor(Name alias, Table<ContributorRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>api_svc.contributor</code> table reference
     */
    public Contributor(String alias) {
        this(DSL.name(alias), CONTRIBUTOR);
    }

    /**
     * Create an aliased <code>api_svc.contributor</code> table reference
     */
    public Contributor(Name alias) {
        this(alias, CONTRIBUTOR);
    }

    /**
     * Create a <code>api_svc.contributor</code> table reference
     */
    public Contributor() {
        this(DSL.name("contributor"), null);
    }

    public <O extends Record> Contributor(Table<O> child, ForeignKey<O, ContributorRecord> key) {
        super(child, key, CONTRIBUTOR);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : ApiSvc.API_SVC;
    }

    @Override
    public Identity<ContributorRecord, Integer> getIdentity() {
        return (Identity<ContributorRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<ContributorRecord> getPrimaryKey() {
        return Keys.CONTRIBUTOR_PKEY;
    }

    @Override
    public List<ForeignKey<ContributorRecord, ?>> getReferences() {
        return Arrays.asList(Keys.CONTRIBUTOR__FK_CONTRIBUTOR_SCHEMA_ID);
    }

    private transient ContributorSchema _contributorSchema;

    /**
     * Get the implicit join path to the <code>api_svc.contributor_schema</code>
     * table.
     */
    public ContributorSchema contributorSchema() {
        if (_contributorSchema == null)
            _contributorSchema = new ContributorSchema(this, Keys.CONTRIBUTOR__FK_CONTRIBUTOR_SCHEMA_ID);

        return _contributorSchema;
    }

    @Override
    public Contributor as(String alias) {
        return new Contributor(DSL.name(alias), this);
    }

    @Override
    public Contributor as(Name alias) {
        return new Contributor(alias, this);
    }

    @Override
    public Contributor as(Table<?> alias) {
        return new Contributor(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public Contributor rename(String name) {
        return new Contributor(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Contributor rename(Name name) {
        return new Contributor(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public Contributor rename(Table<?> name) {
        return new Contributor(name.getQualifiedName(), null);
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
