/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables.records;


import au.org.raid.db.jooq.tables.RaidOrganisationRole;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record5;
import org.jooq.Row5;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class RaidOrganisationRoleRecord extends UpdatableRecordImpl<RaidOrganisationRoleRecord> implements Record5<Integer, Integer, Integer, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>api_svc.raid_organisation_role.id</code>.
     */
    public RaidOrganisationRoleRecord setId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_organisation_role.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for
     * <code>api_svc.raid_organisation_role.raid_organisation_id</code>.
     */
    public RaidOrganisationRoleRecord setRaidOrganisationId(Integer value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for
     * <code>api_svc.raid_organisation_role.raid_organisation_id</code>.
     */
    public Integer getRaidOrganisationId() {
        return (Integer) get(1);
    }

    /**
     * Setter for
     * <code>api_svc.raid_organisation_role.organisation_role_id</code>.
     */
    public RaidOrganisationRoleRecord setOrganisationRoleId(Integer value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for
     * <code>api_svc.raid_organisation_role.organisation_role_id</code>.
     */
    public Integer getOrganisationRoleId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>api_svc.raid_organisation_role.start_date</code>.
     */
    public RaidOrganisationRoleRecord setStartDate(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_organisation_role.start_date</code>.
     */
    public String getStartDate() {
        return (String) get(3);
    }

    /**
     * Setter for <code>api_svc.raid_organisation_role.end_date</code>.
     */
    public RaidOrganisationRoleRecord setEndDate(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_organisation_role.end_date</code>.
     */
    public String getEndDate() {
        return (String) get(4);
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
    public Row5<Integer, Integer, Integer, String, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    @Override
    public Row5<Integer, Integer, Integer, String, String> valuesRow() {
        return (Row5) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return RaidOrganisationRole.RAID_ORGANISATION_ROLE.ID;
    }

    @Override
    public Field<Integer> field2() {
        return RaidOrganisationRole.RAID_ORGANISATION_ROLE.RAID_ORGANISATION_ID;
    }

    @Override
    public Field<Integer> field3() {
        return RaidOrganisationRole.RAID_ORGANISATION_ROLE.ORGANISATION_ROLE_ID;
    }

    @Override
    public Field<String> field4() {
        return RaidOrganisationRole.RAID_ORGANISATION_ROLE.START_DATE;
    }

    @Override
    public Field<String> field5() {
        return RaidOrganisationRole.RAID_ORGANISATION_ROLE.END_DATE;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public Integer component2() {
        return getRaidOrganisationId();
    }

    @Override
    public Integer component3() {
        return getOrganisationRoleId();
    }

    @Override
    public String component4() {
        return getStartDate();
    }

    @Override
    public String component5() {
        return getEndDate();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public Integer value2() {
        return getRaidOrganisationId();
    }

    @Override
    public Integer value3() {
        return getOrganisationRoleId();
    }

    @Override
    public String value4() {
        return getStartDate();
    }

    @Override
    public String value5() {
        return getEndDate();
    }

    @Override
    public RaidOrganisationRoleRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public RaidOrganisationRoleRecord value2(Integer value) {
        setRaidOrganisationId(value);
        return this;
    }

    @Override
    public RaidOrganisationRoleRecord value3(Integer value) {
        setOrganisationRoleId(value);
        return this;
    }

    @Override
    public RaidOrganisationRoleRecord value4(String value) {
        setStartDate(value);
        return this;
    }

    @Override
    public RaidOrganisationRoleRecord value5(String value) {
        setEndDate(value);
        return this;
    }

    @Override
    public RaidOrganisationRoleRecord values(Integer value1, Integer value2, Integer value3, String value4, String value5) {
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
     * Create a detached RaidOrganisationRoleRecord
     */
    public RaidOrganisationRoleRecord() {
        super(RaidOrganisationRole.RAID_ORGANISATION_ROLE);
    }

    /**
     * Create a detached, initialised RaidOrganisationRoleRecord
     */
    public RaidOrganisationRoleRecord(Integer id, Integer raidOrganisationId, Integer organisationRoleId, String startDate, String endDate) {
        super(RaidOrganisationRole.RAID_ORGANISATION_ROLE);

        setId(id);
        setRaidOrganisationId(raidOrganisationId);
        setOrganisationRoleId(organisationRoleId);
        setStartDate(startDate);
        setEndDate(endDate);
    }
}
