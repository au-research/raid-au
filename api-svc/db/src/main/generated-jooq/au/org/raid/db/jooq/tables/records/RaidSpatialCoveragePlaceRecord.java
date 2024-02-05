/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables.records;


import au.org.raid.db.jooq.tables.RaidSpatialCoveragePlace;
import org.jooq.Field;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.TableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class RaidSpatialCoveragePlaceRecord extends TableRecordImpl<RaidSpatialCoveragePlaceRecord> implements Record3<Integer, String, Integer> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for
     * <code>api_svc.raid_spatial_coverage_place.raid_spatial_coverage_id</code>.
     */
    public RaidSpatialCoveragePlaceRecord setRaidSpatialCoverageId(Integer value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for
     * <code>api_svc.raid_spatial_coverage_place.raid_spatial_coverage_id</code>.
     */
    public Integer getRaidSpatialCoverageId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>api_svc.raid_spatial_coverage_place.place</code>.
     */
    public RaidSpatialCoveragePlaceRecord setPlace(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_spatial_coverage_place.place</code>.
     */
    public String getPlace() {
        return (String) get(1);
    }

    /**
     * Setter for <code>api_svc.raid_spatial_coverage_place.language_id</code>.
     */
    public RaidSpatialCoveragePlaceRecord setLanguageId(Integer value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.raid_spatial_coverage_place.language_id</code>.
     */
    public Integer getLanguageId() {
        return (Integer) get(2);
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, Integer> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<Integer, String, Integer> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return RaidSpatialCoveragePlace.RAID_SPATIAL_COVERAGE_PLACE.RAID_SPATIAL_COVERAGE_ID;
    }

    @Override
    public Field<String> field2() {
        return RaidSpatialCoveragePlace.RAID_SPATIAL_COVERAGE_PLACE.PLACE;
    }

    @Override
    public Field<Integer> field3() {
        return RaidSpatialCoveragePlace.RAID_SPATIAL_COVERAGE_PLACE.LANGUAGE_ID;
    }

    @Override
    public Integer component1() {
        return getRaidSpatialCoverageId();
    }

    @Override
    public String component2() {
        return getPlace();
    }

    @Override
    public Integer component3() {
        return getLanguageId();
    }

    @Override
    public Integer value1() {
        return getRaidSpatialCoverageId();
    }

    @Override
    public String value2() {
        return getPlace();
    }

    @Override
    public Integer value3() {
        return getLanguageId();
    }

    @Override
    public RaidSpatialCoveragePlaceRecord value1(Integer value) {
        setRaidSpatialCoverageId(value);
        return this;
    }

    @Override
    public RaidSpatialCoveragePlaceRecord value2(String value) {
        setPlace(value);
        return this;
    }

    @Override
    public RaidSpatialCoveragePlaceRecord value3(Integer value) {
        setLanguageId(value);
        return this;
    }

    @Override
    public RaidSpatialCoveragePlaceRecord values(Integer value1, String value2, Integer value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached RaidSpatialCoveragePlaceRecord
     */
    public RaidSpatialCoveragePlaceRecord() {
        super(RaidSpatialCoveragePlace.RAID_SPATIAL_COVERAGE_PLACE);
    }

    /**
     * Create a detached, initialised RaidSpatialCoveragePlaceRecord
     */
    public RaidSpatialCoveragePlaceRecord(Integer raidSpatialCoverageId, String place, Integer languageId) {
        super(RaidSpatialCoveragePlace.RAID_SPATIAL_COVERAGE_PLACE);

        setRaidSpatialCoverageId(raidSpatialCoverageId);
        setPlace(place);
        setLanguageId(languageId);
    }
}
