package au.org.raid.api.auth;

import au.org.raid.api.exception.ResourceNotFoundException;
import au.org.raid.api.exception.ServicePointNotFoundException;
import au.org.raid.api.service.RaidHistoryService;
import au.org.raid.api.service.ServicePointService;
import au.org.raid.api.service.keycloak.KeycloakService;
import au.org.raid.api.service.keycloak.dto.RaidPermissionsResponse;
import au.org.raid.idl.raidv2.model.AccessTypeIdEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static au.org.raid.api.config.SecurityConfig.SecurityConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RaidAuthorizationService {

    private final ServicePointService servicePointService;
    private final RaidHistoryService raidHistoryService;
    private final KeycloakService keycloakService;

    public AuthorizationManager<RequestAuthorizationContext> createReadAccessManager() {
        return AuthorizationManagers.anyOf(
                this::isOperator,
                this::anyServicePointUserUnlessEmbargoed,
                this::servicePointOwner,
                this::hasRaidAdminPermissions,
                this::hasRaidUserPermissions,
                this::hasPidSearcherRoleIfPidSearch,
                this::isContributorWriter
        );
    }

    private AuthorizationDecision isContributorWriter(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        return new AuthorizationDecision(hasRole(authentication.get(), CONTRIBUTOR_WRITER_ROLE));
    }

    private AuthorizationDecision hasPidSearcherRoleIfPidSearch(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        if (isPidSearch(context)) {
            var token = getJwtToken(authentication.get());
            if (token == null) {
                return new AuthorizationDecision(false);
            }

            return new AuthorizationDecision(hasRole(authentication.get(), PID_SEARCHER_ROLE));
        }

        // Not a PID search, so this rule doesn't apply
        return new AuthorizationDecision(false);
    }

    private AuthorizationDecision anyServicePointUserUnlessEmbargoed(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        if (isPidSearch(context)) {
            return new AuthorizationDecision(false);
        }

        var token = getJwtToken(authentication.get());
        if (token == null) {
            return new AuthorizationDecision(false);
        }

        // Must have either SERVICE_POINT_USER_ROLE or RAID_ADMIN_ROLE
        if (!hasRole(authentication.get(), SERVICE_POINT_USER_ROLE) &&
                !hasRole(authentication.get(), RAID_ADMIN_ROLE)) {
            return new AuthorizationDecision(false);
        }

        var pathParts = context.getRequest().getRequestURI().split("/");
        if (pathParts.length <= 2) {
            // No specific handle in path, allow access
            return new AuthorizationDecision(true);
        }

        var handle = extractHandle(context);
        if (handle == null) {
            return new AuthorizationDecision(false);
        }

        try {
            var raid = raidHistoryService.findByHandle(handle)
                    .orElseThrow(() -> new ResourceNotFoundException(handle));

            // Check if embargoed - deny access if it is
            if (raid.getAccess().getType().getId() == AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_F1CF_) {
                return new AuthorizationDecision(false);
            }

            var groupId = token.getClaimAsString(SERVICE_POINT_GROUP_ID_CLAIM);
            if (groupId == null) {
                return new AuthorizationDecision(false);
            }

            var servicePoint = servicePointService.findByGroupId(groupId)
                    .orElseThrow(() -> new ServicePointNotFoundException(groupId));

            // Allow access if user's service point owns the raid
            return new AuthorizationDecision(
                    servicePoint.getId().equals(raid.getIdentifier().getOwner().getServicePoint().longValue())
            );
        } catch (Exception e) {
            log.error("Error checking service point user access", e);
            return new AuthorizationDecision(false);
        }
    }

    public AuthorizationManager<RequestAuthorizationContext> createWriteAccessManager() {
        return AuthorizationManagers.anyOf(
                this::servicePointOwner,
                this::hasRaidAdminPermissions,
                this::hasRaidUserPermissions,
                this::isOperator
        );
    }

    public AuthorizationManager<RequestAuthorizationContext> createPatchAccessManager() {
        return AuthorizationManagers.anyOf(
                this::hasContributorWriterRole,
                this::servicePointOwner
        );
    }

    /**
     * Authorization manager that grants access when the authenticated user holds a scoped
     * {@code service-point-admin:<groupId>} realm role (see RAID-712) for the service point
     * that owns the raid being accessed.
     *
     * <p>Not yet wired into {@link au.org.raid.api.config.SecurityConfig}'s authorization
     * rules for any endpoint. This is preparatory plumbing added in RAID-723; it will be
     * consumed by later RAID-712 work.
     */
    public AuthorizationManager<RequestAuthorizationContext> isServicePointAdmin() {
        return this::isServicePointAdmin;
    }

    private AuthorizationDecision isOperator(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        return new AuthorizationDecision(hasRole(authentication.get(), OPERATOR_ROLE));
    }

    private AuthorizationDecision hasContributorWriterRole(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        return new AuthorizationDecision(hasRole(authentication.get(), CONTRIBUTOR_WRITER_ROLE));
    }

    private AuthorizationDecision servicePointOwner(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        if (isPidSearch(context)) {
            return new AuthorizationDecision(false);
        }

        var token = getJwtToken(authentication.get());
        if (token == null || !hasRole(authentication.get(), SERVICE_POINT_USER_ROLE)) {
            return new AuthorizationDecision(false);
        }

        var groupId = token.getClaimAsString(SERVICE_POINT_GROUP_ID_CLAIM);
        if (groupId == null) {
            return new AuthorizationDecision(false);
        }

        try {
            var servicePoint = servicePointService.findByGroupId(groupId)
                    .orElseThrow(() -> new ServicePointNotFoundException(groupId));

            var handle = extractHandle(context);
            if (handle == null) {
                return new AuthorizationDecision(false);
            }

            var raid = raidHistoryService.findByHandle(handle)
                    .orElseThrow(() -> new ResourceNotFoundException(handle));

            return new AuthorizationDecision(
                    servicePoint.getId().equals(raid.getIdentifier().getOwner().getServicePoint().longValue())
            );
        } catch (Exception e) {
            log.error("Error checking service point ownership", e);
            return new AuthorizationDecision(false);
        }
    }

    private AuthorizationDecision isServicePointAdmin(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        if (isPidSearch(context)) {
            return new AuthorizationDecision(false);
        }

        var administeredGroupIds = getAdministeredGroupIds(authentication.get());
        if (administeredGroupIds.isEmpty()) {
            return new AuthorizationDecision(false);
        }

        var handle = extractHandle(context);
        if (handle == null) {
            return new AuthorizationDecision(false);
        }

        try {
            var raid = raidHistoryService.findByHandle(handle)
                    .orElseThrow(() -> new ResourceNotFoundException(handle));

            var servicePointId = raid.getIdentifier().getOwner().getServicePoint().longValue();
            var servicePoint = servicePointService.findById(servicePointId)
                    .orElseThrow(() -> new ServicePointNotFoundException(servicePointId));

            return new AuthorizationDecision(administeredGroupIds.contains(servicePoint.getGroupId()));
        } catch (Exception e) {
            log.error("Error checking service point admin access", e);
            return new AuthorizationDecision(false);
        }
    }

    /**
     * Extracts the Keycloak group IDs for which the given authentication holds a scoped
     * {@code service-point-admin:<groupId>} realm role (see RAID-712).
     *
     * <p>Granted authorities of this form are already passed through by
     * {@link au.org.raid.api.config.SecurityConfig#extractAuthorities}; this method gives
     * callers the vocabulary to recognise and resolve them. Not yet consumed by any
     * endpoint's authorization rules.
     */
    public Set<String> getAdministeredGroupIds(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::extractScopedAdminGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private String extractScopedAdminGroupId(String authority) {
        var prefix = "ROLE_" + SERVICE_POINT_ADMIN_ROLE_PREFIX + ":";
        if (!authority.startsWith(prefix)) {
            return null;
        }

        var groupId = authority.substring(prefix.length());
        return groupId.isBlank() ? null : groupId;
    }

    private AuthorizationDecision hasRaidAdminPermissions(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        return hasRaidPermissions(authentication, context, RAID_ADMIN_ROLE, (permissions, handle) -> permissions.getAdminRaids().contains(handle));
    }

    private AuthorizationDecision hasRaidUserPermissions(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        return hasRaidPermissions(authentication, context, RAID_USER_ROLE, (permissions, handle) -> permissions.getUserRaids().contains(handle));
    }

    private AuthorizationDecision hasRaidPermissions(Supplier<Authentication> authentication,
                                                     RequestAuthorizationContext context,
                                                     String roleName,
                                                     java.util.function.BiFunction<RaidPermissionsResponse, String, Boolean> permissionChecker) {
        if (isPidSearch(context)) {
            return new AuthorizationDecision(false);
        }

        var token = getJwtToken(authentication.get());
        if (token == null || !hasRole(authentication.get(), roleName)) {
            return new AuthorizationDecision(false);
        }

        var handle = extractHandle(context);
        if (handle == null) {
            return new AuthorizationDecision(false);
        }

        try {
            var userId = token.getSubject();
            var permissions = keycloakService.getRaidPermissions(userId);
            return new AuthorizationDecision(permissionChecker.apply(permissions, handle));
        } catch (Exception e) {
            log.error("Error checking raid permissions", e);
            return new AuthorizationDecision(false);
        }
    }

    // Helper methods
    private Jwt getJwtToken(Authentication authentication) {
        return authentication instanceof JwtAuthenticationToken jwtAuth ? jwtAuth.getToken() : null;
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }

    private String extractHandle(RequestAuthorizationContext context) {
        var pathParts = context.getRequest().getRequestURI().split("/");
        if (pathParts.length < 4) {
            log.debug("Invalid path for permissions. Handle must be present {}", context.getRequest().getRequestURI());
            return null;
        }
        return "%s/%s".formatted(pathParts[2], pathParts[3]);
    }

    private boolean isPidSearch(RequestAuthorizationContext context) {
        return context.getRequest().getParameter("contributor.id") != null ||
                context.getRequest().getParameter("organisation.id") != null;
    }
}
