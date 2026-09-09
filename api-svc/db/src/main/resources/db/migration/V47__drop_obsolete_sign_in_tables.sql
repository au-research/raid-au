-- Drop the pre-Keycloak sign-in tables.
--
-- app_user and user_authz_request were created by V4__sign_in_tables.sql when the
-- service managed its own users. Authentication now runs through Keycloak: no
-- code writes either table, AppUserRepository had no callers, and
-- user_authz_request had no application code at all.
--
-- user_authz_request holds a foreign key to app_user, so it goes first.

drop table if exists user_authz_request;
drop table if exists app_user;
