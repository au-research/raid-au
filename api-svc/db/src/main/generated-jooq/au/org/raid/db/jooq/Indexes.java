/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq;


import au.org.raid.db.jooq.tables.AppUser;
import au.org.raid.db.jooq.tables.FlywaySchemaHistory;
import au.org.raid.db.jooq.tables.Raid;
import au.org.raid.db.jooq.tables.UserAuthzRequest;
import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling indexes of tables in api_svc.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index APP_USER_ID_FIELDS_ACTIVE_KEY = Internal.createIndex(DSL.name("app_user_id_fields_active_key"), AppUser.APP_USER, new OrderField[] { AppUser.APP_USER.EMAIL, AppUser.APP_USER.CLIENT_ID, AppUser.APP_USER.SUBJECT }, true);
    public static final Index FLYWAY_SCHEMA_HISTORY_S_IDX = Internal.createIndex(DSL.name("flyway_schema_history_s_idx"), FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY, new OrderField[] { FlywaySchemaHistory.FLYWAY_SCHEMA_HISTORY.SUCCESS }, false);
    public static final Index IDX_RAID_SERVICE_POINT_ID_DATE_CREATED = Internal.createIndex(DSL.name("idx_raid_service_point_id_date_created"), Raid.RAID, new OrderField[] { Raid.RAID.SERVICE_POINT_ID, Raid.RAID.DATE_CREATED.desc() }, false);
    public static final Index USER_AUTHZ_REQUEST_ONCE_ACTIVE_KEY = Internal.createIndex(DSL.name("user_authz_request_once_active_key"), UserAuthzRequest.USER_AUTHZ_REQUEST, new OrderField[] { UserAuthzRequest.USER_AUTHZ_REQUEST.SERVICE_POINT_ID, UserAuthzRequest.USER_AUTHZ_REQUEST.CLIENT_ID, UserAuthzRequest.USER_AUTHZ_REQUEST.SUBJECT }, true);
}
