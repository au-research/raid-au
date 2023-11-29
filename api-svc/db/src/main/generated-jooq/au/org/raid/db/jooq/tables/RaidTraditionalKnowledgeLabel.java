/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables;


import au.org.raid.db.jooq.ApiSvc;
import au.org.raid.db.jooq.Keys;
import au.org.raid.db.jooq.tables.records.RaidTraditionalKnowledgeLabelRecord;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function2;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row2;
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
public class RaidTraditionalKnowledgeLabel extends TableImpl<RaidTraditionalKnowledgeLabelRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of
     * <code>api_svc.raid_traditional_knowledge_label</code>
     */
    public static final RaidTraditionalKnowledgeLabel RAID_TRADITIONAL_KNOWLEDGE_LABEL = new RaidTraditionalKnowledgeLabel();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<RaidTraditionalKnowledgeLabelRecord> getRecordType() {
        return RaidTraditionalKnowledgeLabelRecord.class;
    }

    /**
     * The column
     * <code>api_svc.raid_traditional_knowledge_label.raid_name</code>.
     */
    public final TableField<RaidTraditionalKnowledgeLabelRecord, String> RAID_NAME = createField(DSL.name("raid_name"), SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column
     * <code>api_svc.raid_traditional_knowledge_label.traditional_knowledge_label_id</code>.
     */
    public final TableField<RaidTraditionalKnowledgeLabelRecord, Integer> TRADITIONAL_KNOWLEDGE_LABEL_ID = createField(DSL.name("traditional_knowledge_label_id"), SQLDataType.INTEGER.nullable(false), this, "");

    private RaidTraditionalKnowledgeLabel(Name alias, Table<RaidTraditionalKnowledgeLabelRecord> aliased) {
        this(alias, aliased, null);
    }

    private RaidTraditionalKnowledgeLabel(Name alias, Table<RaidTraditionalKnowledgeLabelRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>api_svc.raid_traditional_knowledge_label</code>
     * table reference
     */
    public RaidTraditionalKnowledgeLabel(String alias) {
        this(DSL.name(alias), RAID_TRADITIONAL_KNOWLEDGE_LABEL);
    }

    /**
     * Create an aliased <code>api_svc.raid_traditional_knowledge_label</code>
     * table reference
     */
    public RaidTraditionalKnowledgeLabel(Name alias) {
        this(alias, RAID_TRADITIONAL_KNOWLEDGE_LABEL);
    }

    /**
     * Create a <code>api_svc.raid_traditional_knowledge_label</code> table
     * reference
     */
    public RaidTraditionalKnowledgeLabel() {
        this(DSL.name("raid_traditional_knowledge_label"), null);
    }

    public <O extends Record> RaidTraditionalKnowledgeLabel(Table<O> child, ForeignKey<O, RaidTraditionalKnowledgeLabelRecord> key) {
        super(child, key, RAID_TRADITIONAL_KNOWLEDGE_LABEL);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : ApiSvc.API_SVC;
    }

    @Override
    public UniqueKey<RaidTraditionalKnowledgeLabelRecord> getPrimaryKey() {
        return Keys.RAID_TRADITIONAL_KNOWLEDGE_LABEL_PKEY;
    }

    @Override
    public List<ForeignKey<RaidTraditionalKnowledgeLabelRecord, ?>> getReferences() {
        return Arrays.asList(Keys.RAID_TRADITIONAL_KNOWLEDGE_LABEL__FK_RAID_TRADITIONAL_KNOWLEDGE_LABEL_RAID_NAME);
    }

    private transient Raid _raid;

    /**
     * Get the implicit join path to the <code>api_svc.raid</code> table.
     */
    public Raid raid() {
        if (_raid == null)
            _raid = new Raid(this, Keys.RAID_TRADITIONAL_KNOWLEDGE_LABEL__FK_RAID_TRADITIONAL_KNOWLEDGE_LABEL_RAID_NAME);

        return _raid;
    }

    @Override
    public RaidTraditionalKnowledgeLabel as(String alias) {
        return new RaidTraditionalKnowledgeLabel(DSL.name(alias), this);
    }

    @Override
    public RaidTraditionalKnowledgeLabel as(Name alias) {
        return new RaidTraditionalKnowledgeLabel(alias, this);
    }

    @Override
    public RaidTraditionalKnowledgeLabel as(Table<?> alias) {
        return new RaidTraditionalKnowledgeLabel(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidTraditionalKnowledgeLabel rename(String name) {
        return new RaidTraditionalKnowledgeLabel(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidTraditionalKnowledgeLabel rename(Name name) {
        return new RaidTraditionalKnowledgeLabel(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public RaidTraditionalKnowledgeLabel rename(Table<?> name) {
        return new RaidTraditionalKnowledgeLabel(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<String, Integer> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    public <U> SelectField<U> mapping(Function2<? super String, ? super Integer, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    public <U> SelectField<U> mapping(Class<U> toType, Function2<? super String, ? super Integer, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
