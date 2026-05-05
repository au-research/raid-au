# RAID-547: Audit and Fix Documentation

## What Changed and Why

Verified 19 documentation files against the current codebase and updated them
to reflect the actual state of the system. The most significant changes relate
to the authentication/authorization architecture, which has moved from a custom
api-token system to Keycloak-based OAuth2/OIDC.

### Security & Access Control Documentation

1. **doc/security/access-control/authentication/oauth2_api-token_exchange.md** -
   Complete rewrite. Replaced Google OAuth2 + custom HS256 api-token flow with
   Keycloak-based OIDC flow. Changed app-client to raid-agency-app. Removed
   references to deleted classes (SignInContainer.tsx, AppUserAuthnEndpoint.java,
   AuthProvider.tsx). Updated API path from /raid/v1/ to /raid/. Updated link
   from api-token-spring-authorization.md to spring-security-configuration.md.

2. **doc/security/access-control/authentication/readme.md** - Major rewrite.
   Removed custom api-token/HS256 concepts. Updated to describe Keycloak as IAM
   server with RS256 JWTs. Updated OAuth2 terminology. Removed
   api-key/api-token/app-user from RAiD terms. Removed symmetric crypto section.

3. **doc/security/access-control/authorization/spring-security-configuration.md** -
   Complete rewrite. Replaced references to RaidWebSecurityConfig and
   RaidV2AuthenticationProvider with SecurityConfig.java and
   RaidAuthorizationService.java. Changed EmbeddedJetty to embedded Tomcat.
   Rewrote Mermaid diagram. Added roles table. Removed link to non-existent
   api-token-authz-flow.md.

4. **doc/security/access-control/readme.md** - Significant updates. Changed
   SP_ADMIN to operator. Updated human flow for Keycloak auth. Updated machine
   flow to describe Keycloak service accounts. Updated audit section
   (raid_history now exists). Updated user identification to JWT sub claim.

5. **doc/security/api-svc/readme.md** - No changes. Brief placeholder is
   accurate.

6. **doc/security/api-svc/sql-injection.md** - No changes. jOOQ still in use,
   content is accurate.

7. **doc/security/api-svc/ssrf.md** - No changes. AbstractUriValidator
   (regex + HTTP HEAD) pattern confirmed in codebase; content is accurate.

8. **doc/security/api-svc/xss.md** - No changes. Content is accurate: the
   api-svc does not sanitize HTML, clients must implement output-encoding.

### API Integration Documentation

9. **doc/api-integration/minting-a-raid.md** - Major rewrite. Replaced custom
   api-token auth with Keycloak access tokens (human flow and client credentials
   grant). Updated API paths from /raid/v1 to /raid/. Rewrote curl examples to
   use current RaidCreateRequest schema (vocabulary URIs for types instead of
   plain strings). Updated token security guidance. Added metadata schema link.

10. **doc/api-integration/api-client-onboarding-guide.md** - Updated Raido to
    RAiD. Changed app-client to raid-agency-app.

### Root-Level Files

11. **readme.md** - Updated Node.js version requirement from >= 18 to >= 22
    (matches CI workflow).

12. **security.md** - Updated Raido to RAiD (2 occurrences).

### API Service Documentation

13. **api-svc/doc/adr/2022-09-26-docker.md** - Added status note confirming
    Docker is still in use, images pushed to AWS ECR.

14. **api-svc/doc/adr/2022-10-06-api-access.md** - Added status note documenting
    that pre-shared API keys have been superseded by OAuth2/OIDC via Keycloak.

15. **api-svc/doc/code/db-transaction-guidleline.md** - Updated local Postgres
    version reference from PG 15 to PG 16 to match docker-compose.yaml.

16. **api-svc/doc/code/guideline.md** - No changes needed.

17. **api-svc/idl-raid-v2/doc/metadata-schema-coverage.md** - Major update.
    Removed obsolete schema references. Updated metadata blocks for language
    fields, contributor ISNI support, and RelatedObject changes.

18. **api-svc/idl-raid-v2/readme.md** - Added spec file path, generated output
    location, and build dependency notes.

19. **buildSrc/readme.md** - No changes needed.

### Code Standards & ADR Documentation

20. **doc/adr/2022-07-21_build-tooling.md** - Updated create-react-app/webpack
    reference to Vite (current frontend build tool).

21. **doc/adr/readme.md** - Added index section listing all 6 ADR files
    (previously only 2 were linked).

22. **doc/code/guideline.md** - Fixed broken link to deleted
    `api-svc/spring/src/main/java/raido/apisvc/spring/config/environment/readme.md`,
    replaced with reference to `application.yaml` at correct current path.

23. **doc/code/review-checklist.md** - Fixed same broken environment readme link.
    Updated `app-client` to `raid-agency-app` in XSS section.

24. **doc/character-sets-and-encodings.md** - Updated GitHub URLs from
    `au-research/raido` to `au-research/raid-au`. Rewrote "Current
    functionality" section for DataCite DOI minting. Updated `raido.org` to
    `raid.org`.

25. **doc/raid-vs-raido.md** - Updated note that `raid.org` now exists and is
    operational (was previously noted as "does not exist yet").

26. **doc/observability.md** - Major rewrite. Removed references to deleted
    MetricProps.java, deleted metrics docs, and Micrometer (no longer in
    dependencies). Renamed app-client to raid-agency-app.

27. **doc/search-index/sitemap-search-index.md** - Removed broken link to
    deleted BuildSearchIndex.java. Added note that doc serves as design
    reference.

### Files Verified — No Changes Needed

- **api-svc/idl-raid-v2/src/readme.md** - Global vs regional distinction still
  accurate.
- **doc/adr/2022-07-21_adr.md** - ADR format guidance still followed.
- **doc/adr/2022-07-21_example-adr.md** - Template still useful and accurate.
- **doc/adr/2022-07-21_typescript.md** - TypeScript still actively used (v5.9.3).
- **doc/code/optional.md** - Conventions still valid.
- **doc/code/readme.md** - All links to standard/guideline/optional are correct.
- **doc/code/standard.md** - All mandatory standards still enforced (UTF-8, UTC,
  Unix line endings, no credentials).
- **doc/security/api-svc/sql-injection.md** - jOOQ parameterized queries still
  in use.
- **doc/security/api-svc/ssrf.md** - AbstractUriValidator regex+HTTP HEAD
  pattern confirmed.
- **doc/security/api-svc/xss.md** - Client-side output-encoding responsibility
  still accurate.

## JIRA Tickets

- [RAID-547](https://ardc.atlassian.net/browse/RAID-547)
