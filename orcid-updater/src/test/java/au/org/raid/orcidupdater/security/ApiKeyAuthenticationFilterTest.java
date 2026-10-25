package au.org.raid.orcidupdater.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiKeyAuthenticationFilter")
class ApiKeyAuthenticationFilterTest {
    private final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter("expected-key");

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockFilterChain filter(final String presentedKey) throws Exception {
        final var request = new MockHttpServletRequest("POST", "/raid-changes");
        if (presentedKey != null) {
            request.addHeader(ApiKeyAuthenticationFilter.HEADER, presentedKey);
        }
        final var chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    @Test
    @DisplayName("The correct key authenticates the RAiD API and the request continues")
    void correctKeyAuthenticates() throws Exception {
        final var chain = filter("expected-key");

        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_raid-api");
        assertThat(authentication.getCredentials()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("A wrong key leaves the request unauthenticated")
    void wrongKeyDoesNotAuthenticate() throws Exception {
        final var chain = filter("wrong-key");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("A key that only starts with the expected key does not authenticate")
    void prefixDoesNotAuthenticate() throws Exception {
        filter("expected-key-and-more");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("No key leaves the request unauthenticated")
    void missingKeyDoesNotAuthenticate() throws Exception {
        final var chain = filter(null);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }
}
