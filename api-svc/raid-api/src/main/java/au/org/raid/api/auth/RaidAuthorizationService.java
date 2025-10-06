package au.org.raid.api.auth;

import au.org.raid.api.exception.ResourceNotFoundException;
import au.org.raid.api.exception.ServicePointNotFoundException;
import au.org.raid.api.service.RaidHistoryService;
import au.org.raid.api.service.ServicePointService;
import au.org.raid.idl.raidv2.model.AccessTypeIdEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

import static au.org.raid.api.config.SecurityConfig.SecurityConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RaidAuthorizationService {

    private final ServicePointService servicePointService;
    private final RaidHistoryService raidHistoryService;

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
            if (raid.getAccess().getType().getId().equals(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_F1CF_)) {
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
                    raid.getIdentifier().getOwner().getServicePoint().longValue() == servicePoint.getId()
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
                this::hasRaidUserPermissions
        );
    }

    public AuthorizationManager<RequestAuthorizationContext> createPatchAccessManager() {
        return AuthorizationManagers.anyOf(
                this::hasContributorWriterRole,
                this::servicePointOwner
        );
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
                    raid.getIdentifier().getOwner().getServicePoint().longValue() == servicePoint.getId()
            );
        } catch (Exception e) {
            log.error("Error checking service point ownership", e);
            return new AuthorizationDecision(false);
        }
    }

    private AuthorizationDecision hasRaidAdminPermissions(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        return hasRaidPermissions(authentication, context, RAID_ADMIN_ROLE, ADMIN_RAIDS_CLAIM);
    }

    private AuthorizationDecision hasRaidUserPermissions(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        return hasRaidPermissions(authentication, context, RAID_USER_ROLE, USER_RAIDS_CLAIM);
    }

    private AuthorizationDecision hasRaidPermissions(Supplier<Authentication> authentication,
                                                     RequestAuthorizationContext context,
                                                     String roleName,
                                                     String claimName) {
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

        var validRaids = token.getClaimAsStringList(claimName);
        return new AuthorizationDecision(validRaids != null && validRaids.contains(handle));
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
