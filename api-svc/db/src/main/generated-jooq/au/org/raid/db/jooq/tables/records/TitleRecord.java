/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables.records;


import au.org.raid.db.jooq.tables.Title;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TitleRecord extends UpdatableRecordImpl<TitleRecord> implements Record4<Integer, String, Integer, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>api_svc.title.id</code>.
     */
    public TitleRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.title.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>api_svc.title.raid_name</code>.
     */
    public TitleRecord setRaidName(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.title.raid_name</code>.
     */
    public String getRaidName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>api_svc.title.title_type_id</code>.
     */
    public TitleRecord setTitleTypeId(Integer value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.title.title_type_id</code>.
     */
    public Integer getTitleTypeId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>api_svc.title.value</code>.
     */
    public TitleRecord setValue(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.title.value</code>.
     */
    public String getValue() {
        return (String) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row4<Integer, String, Integer, String> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    public Row4<Integer, String, Integer, String> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return Title.TITLE.ID;
    }

    @Override
    public Field<String> field2() {
        return Title.TITLE.RAID_NAME;
    }

    @Override
    public Field<Integer> field3() {
        return Title.TITLE.TITLE_TYPE_ID;
    }

    @Override
    public Field<String> field4() {
        return Title.TITLE.VALUE;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getRaidName();
    }

    @Override
    public Integer component3() {
        return getTitleTypeId();
    }

    @Override
    public String component4() {
        return getValue();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getRaidName();
    }

    @Override
    public Integer value3() {
        return getTitleTypeId();
    }

    @Override
    public String value4() {
        return getValue();
    }

    @Override
    public TitleRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public TitleRecord value2(String value) {
        setRaidName(value);
        return this;
    }

    @Override
    public TitleRecord value3(Integer value) {
        setTitleTypeId(value);
        return this;
    }

    @Override
    public TitleRecord value4(String value) {
        setValue(value);
        return this;
    }

    @Override
    public TitleRecord values(Integer value1, String value2, Integer value3, String value4) {
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
     * Create a detached TitleRecord
     */
    public TitleRecord() {
        super(Title.TITLE);
    }

    /**
     * Create a detached, initialised TitleRecord
     */
    public TitleRecord(Integer id, String raidName, Integer titleTypeId, String value) {
        super(Title.TITLE);

        setId(id);
        setRaidName(raidName);
        setTitleTypeId(titleTypeId);
        setValue(value);
    }
}
