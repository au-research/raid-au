/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables.records;


import au.org.raid.db.jooq.tables.RaidHistory;
import org.jooq.Field;
import org.jooq.Record3;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;

import java.time.LocalDateTime;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class RaidHistoryRecord extends UpdatableRecordImpl<RaidHistoryRecord> implements Record5<String, Integer, String, String, LocalDateTime> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>api_svc.raid_history.handle</code>.
     */
    public RaidHistoryRecord setHandle(String value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_history.handle</code>.
     */
    public String getHandle() {
        return (String) get(0);
    }

    /**
     * Setter for <code>api_svc.raid_history.revision</code>.
     */
    public RaidHistoryRecord setRevision(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_history.revision</code>.
     */
    public Integer getRevision() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>api_svc.raid_history.change_type</code>.
     */
    public RaidHistoryRecord setChangeType(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_history.change_type</code>.
     */
    public String getChangeType() {
        return (String) get(2);
    }

    /**
     * Setter for <code>api_svc.raid_history.diff</code>.
     */
    public RaidHistoryRecord setDiff(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_history.diff</code>.
     */
    public String getDiff() {
        return (String) get(3);
    }

    /**
     * Setter for <code>api_svc.raid_history.created</code>.
     */
    public RaidHistoryRecord setCreated(LocalDateTime value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_history.created</code>.
     */
    public LocalDateTime getCreated() {
        return (LocalDateTime) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record3<String, Integer, String> key() {
        return (Record3) super.key();
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<String, Integer, String, String, LocalDateTime> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<String, Integer, String, String, LocalDateTime> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return RaidHistory.RAID_HISTORY.HANDLE;
    }

    @Override
    public Field<Integer> field2() {
        return RaidHistory.RAID_HISTORY.REVISION;
    }

    @Override
    public Field<String> field3() {
        return RaidHistory.RAID_HISTORY.CHANGE_TYPE;
    }

    @Override
    public Field<String> field4() {
        return RaidHistory.RAID_HISTORY.DIFF;
    }

    @Override
    public Field<LocalDateTime> field5() {
        return RaidHistory.RAID_HISTORY.CREATED;
    }

    @Override
    public String component1() {
        return getHandle();
    }

    @Override
    public Integer component2() {
        return getRevision();
    }

    @Override
    public String component3() {
        return getChangeType();
    }

    @Override
    public String component4() {
        return getDiff();
    }

    @Override
    public LocalDateTime component5() {
        return getCreated();
    }

    @Override
    public String value1() {
        return getHandle();
    }

    @Override
    public Integer value2() {
        return getRevision();
    }

    @Override
    public String value3() {
        return getChangeType();
    }

    @Override
    public String value4() {
        return getDiff();
    }

    @Override
    public LocalDateTime value5() {
        return getCreated();
    }

    @Override
    public RaidHistoryRecord value1(String value) {
        setHandle(value);
        return this;
    }

    @Override
    public RaidHistoryRecord value2(Integer value) {
        setRevision(value);
        return this;
    }

    @Override
    public RaidHistoryRecord value3(String value) {
        setChangeType(value);
        return this;
    }

    @Override
    public RaidHistoryRecord value4(String value) {
        setDiff(value);
        return this;
    }

    @Override
    public RaidHistoryRecord value5(LocalDateTime value) {
        setCreated(value);
        return this;
    }

    @Override
    public RaidHistoryRecord values(String value1, Integer value2, String value3, String value4, LocalDateTime value5) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached RaidHistoryRecord
     */
    public RaidHistoryRecord() {
        super(RaidHistory.RAID_HISTORY);
    }

    /**
     * Create a detached, initialised RaidHistoryRecord
     */
    public RaidHistoryRecord(String handle, Integer revision, String changeType, String diff, LocalDateTime created) {
        super(RaidHistory.RAID_HISTORY);

        setHandle(handle);
        setRevision(revision);
        setChangeType(changeType);
        setDiff(diff);
        setCreated(created);
    }
}
