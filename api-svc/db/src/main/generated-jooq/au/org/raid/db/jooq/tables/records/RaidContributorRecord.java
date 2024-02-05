/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables.records;


import au.org.raid.db.jooq.tables.RaidContributor;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class RaidContributorRecord extends UpdatableRecordImpl<RaidContributorRecord> implements Record5<Integer, String, Integer, Boolean, Boolean> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>api_svc.raid_contributor.id</code>.
     */
    public RaidContributorRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_contributor.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>api_svc.raid_contributor.handle</code>.
     */
    public RaidContributorRecord setHandle(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_contributor.handle</code>.
     */
    public String getHandle() {
        return (String) get(1);
    }

    /**
     * Setter for <code>api_svc.raid_contributor.contributor_id</code>.
     */
    public RaidContributorRecord setContributorId(Integer value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_contributor.contributor_id</code>.
     */
    public Integer getContributorId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>api_svc.raid_contributor.leader</code>.
     */
    public RaidContributorRecord setLeader(Boolean value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_contributor.leader</code>.
     */
    public Boolean getLeader() {
        return (Boolean) get(3);
    }

    /**
     * Setter for <code>api_svc.raid_contributor.contact</code>.
     */
    public RaidContributorRecord setContact(Boolean value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_contributor.contact</code>.
     */
    public Boolean getContact() {
        return (Boolean) get(4);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row5<Integer, String, Integer, Boolean, Boolean> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<Integer, String, Integer, Boolean, Boolean> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return RaidContributor.RAID_CONTRIBUTOR.ID;
    }

    @Override
    public Field<String> field2() {
        return RaidContributor.RAID_CONTRIBUTOR.HANDLE;
    }

    @Override
    public Field<Integer> field3() {
        return RaidContributor.RAID_CONTRIBUTOR.CONTRIBUTOR_ID;
    }

    @Override
    public Field<Boolean> field4() {
        return RaidContributor.RAID_CONTRIBUTOR.LEADER;
    }

    @Override
    public Field<Boolean> field5() {
        return RaidContributor.RAID_CONTRIBUTOR.CONTACT;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getHandle();
    }

    @Override
    public Integer component3() {
        return getContributorId();
    }

    @Override
    public Boolean component4() {
        return getLeader();
    }

    @Override
    public Boolean component5() {
        return getContact();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getHandle();
    }

    @Override
    public Integer value3() {
        return getContributorId();
    }

    @Override
    public Boolean value4() {
        return getLeader();
    }

    @Override
    public Boolean value5() {
        return getContact();
    }

    @Override
    public RaidContributorRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public RaidContributorRecord value2(String value) {
        setHandle(value);
        return this;
    }

    @Override
    public RaidContributorRecord value3(Integer value) {
        setContributorId(value);
        return this;
    }

    @Override
    public RaidContributorRecord value4(Boolean value) {
        setLeader(value);
        return this;
    }

    @Override
    public RaidContributorRecord value5(Boolean value) {
        setContact(value);
        return this;
    }

    @Override
    public RaidContributorRecord values(Integer value1, String value2, Integer value3, Boolean value4, Boolean value5) {
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
     * Create a detached RaidContributorRecord
     */
    public RaidContributorRecord() {
        super(RaidContributor.RAID_CONTRIBUTOR);
    }

    /**
     * Create a detached, initialised RaidContributorRecord
     */
    public RaidContributorRecord(Integer id, String handle, Integer contributorId, Boolean leader, Boolean contact) {
        super(RaidContributor.RAID_CONTRIBUTOR);

        setId(id);
        setHandle(handle);
        setContributorId(contributorId);
        setLeader(leader);
        setContact(contact);
    }
}
