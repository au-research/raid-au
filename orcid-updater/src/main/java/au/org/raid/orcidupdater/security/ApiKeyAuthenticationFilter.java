package au.org.raid.orcidupdater.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Authenticates the RAiD API by the shared key in the {@value #HEADER} header (RAID-975 caller
 * authentication; the central updater had none, RAID-887). A missing or wrong key leaves the
 * request unauthenticated, so the filter chain's entry point answers 401. The key is never logged.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Api-Key";
    public static final String RAID_API_ROLE = "raid-api";

    private final byte[] expectedKey;

    public ApiKeyAuthenticationFilter(final String expectedKey) {
        this.expectedKey = expectedKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        final var presented = request.getHeader(HEADER);

        // MessageDigest.isEqual is constant-time, so response timing can't be used to guess the key
        if (presented != null && MessageDigest.isEqual(expectedKey, presented.getBytes(StandardCharsets.UTF_8))) {
            final var authentication = new UsernamePasswordAuthenticationToken(
                    RAID_API_ROLE, null, AuthorityUtils.createAuthorityList("ROLE_" + RAID_API_ROLE));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
