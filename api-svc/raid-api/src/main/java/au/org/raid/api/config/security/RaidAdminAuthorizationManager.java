package au.org.raid.api.config.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;
import java.util.function.Supplier;

public class RaidAdminAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    @Override
    public AuthorizationDecision authorize(final Supplier<? extends Authentication> authentication, final RequestAuthorizationContext context) {

        final var pathParts = context.getRequest().getRequestURI().split("/");

        final var handle = "%s/%s".formatted(pathParts[2], pathParts[3]);

        final var token = ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken();
        final var adminRaids = (List<?>) token.getClaims().get("admin_raids");


        if (adminRaids != null && adminRaids.contains(handle)) {
            return new AuthorizationDecision(true);
        }

        return new AuthorizationDecision(false);
    }
}
