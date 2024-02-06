/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables.records;


import au.org.raid.db.jooq.tables.TeamUser;

import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.TableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class TeamUserRecord extends TableRecordImpl<TeamUserRecord> implements Record2<Long, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>api_svc.team_user.app_user_id</code>.
     */
    public TeamUserRecord setAppUserId(Long value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.team_user.app_user_id</code>.
     */
    public Long getAppUserId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>api_svc.team_user.team_id</code>.
     */
    public TeamUserRecord setTeamId(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.team_user.team_id</code>.
     */
    public String getTeamId() {
        return (String) get(1);
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row2<Long, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    @Override
    public Row2<Long, String> valuesRow() {
        return (Row2) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return TeamUser.TEAM_USER.APP_USER_ID;
    }

    @Override
    public Field<String> field2() {
        return TeamUser.TEAM_USER.TEAM_ID;
    }

    @Override
    public Long component1() {
        return getAppUserId();
    }

    @Override
    public String component2() {
        return getTeamId();
    }

    @Override
    public Long value1() {
        return getAppUserId();
    }

    @Override
    public String value2() {
        return getTeamId();
    }

    @Override
    public TeamUserRecord value1(Long value) {
        setAppUserId(value);
        return this;
    }

    @Override
    public TeamUserRecord value2(String value) {
        setTeamId(value);
        return this;
    }

    @Override
    public TeamUserRecord values(Long value1, String value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached TeamUserRecord
     */
    public TeamUserRecord() {
        super(TeamUser.TEAM_USER);
    }

    /**
     * Create a detached, initialised TeamUserRecord
     */
    public TeamUserRecord(Long appUserId, String teamId) {
        super(TeamUser.TEAM_USER);

        setAppUserId(appUserId);
        setTeamId(teamId);
    }
}
