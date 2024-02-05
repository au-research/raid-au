/*
 * This file is generated by jOOQ.
 */
package au.org.raid.db.jooq.tables.records;


import au.org.raid.db.jooq.enums.IdProvider;
import au.org.raid.db.jooq.enums.UserRole;
import au.org.raid.db.jooq.tables.AppUser;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record10;
import org.jooq.Row10;
import org.jooq.impl.UpdatableRecordImpl;

import java.time.LocalDateTime;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AppUserRecord extends UpdatableRecordImpl<AppUserRecord> implements Record10<Long, Long, String, String, String, IdProvider, UserRole, Boolean, LocalDateTime, LocalDateTime> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>api_svc.app_user.id</code>.
     */
    public AppUserRecord setId(Long value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.app_user.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>api_svc.app_user.service_point_id</code>.
     */
    public AppUserRecord setServicePointId(Long value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.app_user.service_point_id</code>.
     */
    public Long getServicePointId() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>api_svc.app_user.email</code>. should be renamed to
     * "description" or some such.  api-keys do not and orcid 
     *   sign-ins might not have email address
     */
    public AppUserRecord setEmail(String value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.app_user.email</code>. should be renamed to
     * "description" or some such.  api-keys do not and orcid 
     *   sign-ins might not have email address
     */
    public String getEmail() {
        return (String) get(2);
    }

    /**
     * Setter for <code>api_svc.app_user.client_id</code>.
     */
    public AppUserRecord setClientId(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.app_user.client_id</code>.
     */
    public String getClientId() {
        return (String) get(3);
    }

    /**
     * Setter for <code>api_svc.app_user.subject</code>.
     */
    public AppUserRecord setSubject(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.app_user.subject</code>.
     */
    public String getSubject() {
        return (String) get(4);
    }

    /**
     * Setter for <code>api_svc.app_user.id_provider</code>. not a real identity
     * field, its just redundant info we figure it out from 
     *   the clientId or issuer and store it for easy analysis
     */
    public AppUserRecord setIdProvider(IdProvider value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.app_user.id_provider</code>. not a real identity
     * field, its just redundant info we figure it out from 
     *   the clientId or issuer and store it for easy analysis
     */
    public IdProvider getIdProvider() {
        return (IdProvider) get(5);
    }

    /**
     * Setter for <code>api_svc.app_user.role</code>.
     */
    public AppUserRecord setRole(UserRole value) {
        set(6, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.app_user.role</code>.
     */
    public UserRole getRole() {
        return (UserRole) get(6);
    }

    /**
     * Setter for <code>api_svc.app_user.enabled</code>.
     */
    public AppUserRecord setEnabled(Boolean value) {
        set(7, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.app_user.enabled</code>.
     */
    public Boolean getEnabled() {
        return (Boolean) get(7);
    }

    /**
     * Setter for <code>api_svc.app_user.token_cutoff</code>. Any endpoint call
     * with a bearer token issued after this point will be 
     *   rejected. Any authentication attempt after this point will be rejected.
     */
    public AppUserRecord setTokenCutoff(LocalDateTime value) {
        set(8, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.app_user.token_cutoff</code>. Any endpoint call
     * with a bearer token issued after this point will be 
     *   rejected. Any authentication attempt after this point will be rejected.
     */
    public LocalDateTime getTokenCutoff() {
        return (LocalDateTime) get(8);
    }

    /**
     * Setter for <code>api_svc.app_user.date_created</code>.
     */
    public AppUserRecord setDateCreated(LocalDateTime value) {
        set(9, value);
        return this;
    }

    /**
     * Getter for <code>api_svc.app_user.date_created</code>.
     */
    public LocalDateTime getDateCreated() {
        return (LocalDateTime) get(9);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record10 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row10<Long, Long, String, String, String, IdProvider, UserRole, Boolean, LocalDateTime, LocalDateTime> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    @Override
    public Row10<Long, Long, String, String, String, IdProvider, UserRole, Boolean, LocalDateTime, LocalDateTime> valuesRow() {
        return (Row10) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return AppUser.APP_USER.ID;
    }

    @Override
    public Field<Long> field2() {
        return AppUser.APP_USER.SERVICE_POINT_ID;
    }

    @Override
    public Field<String> field3() {
        return AppUser.APP_USER.EMAIL;
    }

    @Override
    public Field<String> field4() {
        return AppUser.APP_USER.CLIENT_ID;
    }

    @Override
    public Field<String> field5() {
        return AppUser.APP_USER.SUBJECT;
    }

    @Override
    public Field<IdProvider> field6() {
        return AppUser.APP_USER.ID_PROVIDER;
    }

    @Override
    public Field<UserRole> field7() {
        return AppUser.APP_USER.ROLE;
    }

    @Override
    public Field<Boolean> field8() {
        return AppUser.APP_USER.ENABLED;
    }

    @Override
    public Field<LocalDateTime> field9() {
        return AppUser.APP_USER.TOKEN_CUTOFF;
    }

    @Override
    public Field<LocalDateTime> field10() {
        return AppUser.APP_USER.DATE_CREATED;
    }

    @Override
    public Long component1() {
        return getId();
    }

    @Override
    public Long component2() {
        return getServicePointId();
    }

    @Override
    public String component3() {
        return getEmail();
    }

    @Override
    public String component4() {
        return getClientId();
    }

    @Override
    public String component5() {
        return getSubject();
    }

    @Override
    public IdProvider component6() {
        return getIdProvider();
    }

    @Override
    public UserRole component7() {
        return getRole();
    }

    @Override
    public Boolean component8() {
        return getEnabled();
    }

    @Override
    public LocalDateTime component9() {
        return getTokenCutoff();
    }

    @Override
    public LocalDateTime component10() {
        return getDateCreated();
    }

    @Override
    public Long value1() {
        return getId();
    }

    @Override
    public Long value2() {
        return getServicePointId();
    }

    @Override
    public String value3() {
        return getEmail();
    }

    @Override
    public String value4() {
        return getClientId();
    }

    @Override
    public String value5() {
        return getSubject();
    }

    @Override
    public IdProvider value6() {
        return getIdProvider();
    }

    @Override
    public UserRole value7() {
        return getRole();
    }

    @Override
    public Boolean value8() {
        return getEnabled();
    }

    @Override
    public LocalDateTime value9() {
        return getTokenCutoff();
    }

    @Override
    public LocalDateTime value10() {
        return getDateCreated();
    }

    @Override
    public AppUserRecord value1(Long value) {
        setId(value);
        return this;
    }

    @Override
    public AppUserRecord value2(Long value) {
        setServicePointId(value);
        return this;
    }

    @Override
    public AppUserRecord value3(String value) {
        setEmail(value);
        return this;
    }

    @Override
    public AppUserRecord value4(String value) {
        setClientId(value);
        return this;
    }

    @Override
    public AppUserRecord value5(String value) {
        setSubject(value);
        return this;
    }

    @Override
    public AppUserRecord value6(IdProvider value) {
        setIdProvider(value);
        return this;
    }

    @Override
    public AppUserRecord value7(UserRole value) {
        setRole(value);
        return this;
    }

    @Override
    public AppUserRecord value8(Boolean value) {
        setEnabled(value);
        return this;
    }

    @Override
    public AppUserRecord value9(LocalDateTime value) {
        setTokenCutoff(value);
        return this;
    }

    @Override
    public AppUserRecord value10(LocalDateTime value) {
        setDateCreated(value);
        return this;
    }

    @Override
    public AppUserRecord values(Long value1, Long value2, String value3, String value4, String value5, IdProvider value6, UserRole value7, Boolean value8, LocalDateTime value9, LocalDateTime value10) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AppUserRecord
     */
    public AppUserRecord() {
        super(AppUser.APP_USER);
    }

    /**
     * Create a detached, initialised AppUserRecord
     */
    public AppUserRecord(Long id, Long servicePointId, String email, String clientId, String subject, IdProvider idProvider, UserRole role, Boolean enabled, LocalDateTime tokenCutoff, LocalDateTime dateCreated) {
        super(AppUser.APP_USER);

        setId(id);
        setServicePointId(servicePointId);
        setEmail(email);
        setClientId(clientId);
        setSubject(subject);
        setIdProvider(idProvider);
        setRole(role);
        setEnabled(enabled);
        setTokenCutoff(tokenCutoff);
        setDateCreated(dateCreated);
    }
}
