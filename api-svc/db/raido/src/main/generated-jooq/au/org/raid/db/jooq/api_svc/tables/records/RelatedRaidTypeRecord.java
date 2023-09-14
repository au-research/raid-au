/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.api_svc.tables.records;


import au.org.raid.db.jooq.api_svc.tables.RelatedRaidType;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class RelatedRaidTypeRecord extends UpdatableRecordImpl<RelatedRaidTypeRecord> implements Record4<Integer, String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>api_svc.related_raid_type.schema_id</code>.
     */
    public RelatedRaidTypeRecord setSchemaId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.related_raid_type.schema_id</code>.
     */
    public Integer getSchemaId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>api_svc.related_raid_type.uri</code>.
     */
    public RelatedRaidTypeRecord setUri(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.related_raid_type.uri</code>.
     */
    public String getUri() {
        return (String) get(1);
    }

    /**
     * Setter for <code>api_svc.related_raid_type.name</code>.
     */
    public RelatedRaidTypeRecord setName(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.related_raid_type.name</code>.
     */
    public String getName() {
        return (String) get(2);
    }

    /**
     * Setter for <code>api_svc.related_raid_type.description</code>.
     */
    public RelatedRaidTypeRecord setDescription(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.related_raid_type.description</code>.
     */
    public String getDescription() {
        return (String) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record2<Integer, String> key() {
        return (Record2) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, String, String, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Integer, String, String, String> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return RelatedRaidType.RELATED_RAID_TYPE.SCHEMA_ID;
    }

    @Override
    public Field<String> field2() {
        return RelatedRaidType.RELATED_RAID_TYPE.URI;
    }

    @Override
    public Field<String> field3() {
        return RelatedRaidType.RELATED_RAID_TYPE.NAME;
    }

    @Override
    public Field<String> field4() {
        return RelatedRaidType.RELATED_RAID_TYPE.DESCRIPTION;
    }

    @Override
    public Integer component1() {
        return getSchemaId();
    }

    @Override
    public String component2() {
        return getUri();
    }

    @Override
    public String component3() {
        return getName();
    }

    @Override
    public String component4() {
        return getDescription();
    }

    @Override
    public Integer value1() {
        return getSchemaId();
    }

    @Override
    public String value2() {
        return getUri();
    }

    @Override
    public String value3() {
        return getName();
    }

    @Override
    public String value4() {
        return getDescription();
    }

    @Override
    public RelatedRaidTypeRecord value1(Integer value) {
        setSchemaId(value);
        return this;
    }

    @Override
    public RelatedRaidTypeRecord value2(String value) {
        setUri(value);
        return this;
    }

    @Override
    public RelatedRaidTypeRecord value3(String value) {
        setName(value);
        return this;
    }

    @Override
    public RelatedRaidTypeRecord value4(String value) {
        setDescription(value);
        return this;
    }

    @Override
    public RelatedRaidTypeRecord values(Integer value1, String value2, String value3, String value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached RelatedRaidTypeRecord
     */
    public RelatedRaidTypeRecord() {
        super(RelatedRaidType.RELATED_RAID_TYPE);
    }

    /**
     * Create a detached, initialised RelatedRaidTypeRecord
     */
    public RelatedRaidTypeRecord(Integer schemaId, String uri, String name, String description) {
        super(RelatedRaidType.RELATED_RAID_TYPE);

        setSchemaId(schemaId);
        setUri(uri);
        setName(name);
        setDescription(description);
    }
}
