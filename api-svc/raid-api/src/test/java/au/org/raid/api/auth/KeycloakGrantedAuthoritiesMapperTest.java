package au.org.raid.api.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakGrantedAuthoritiesMapperTest {

    private KeycloakGrantedAuthoritiesMapper mapper;

    @Mock
    private OidcUserAuthority oidcUserAuthority;

    @Mock
    private OidcUserInfo userInfo;

    @BeforeEach
    void setUp() {
        mapper = new KeycloakGrantedAuthoritiesMapper();
    }

    @Test
    void mapAuthorities_withEmptyAuthorities_returnsEmptySet() {
        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(Collections.emptyList());

        assertThat(result, is(empty()));
    }

    @Test
    void mapAuthorities_withOidcUserAuthority_extractsRolesFromRealmAccess() {
        // Given
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Arrays.asList("admin", "user", "moderator"));

        when(oidcUserAuthority.getUserInfo()).thenReturn(userInfo);
        when(userInfo.hasClaim("realm_access")).thenReturn(true);
        when(userInfo.getClaimAsMap("realm_access")).thenReturn(realmAccess);

        Collection<GrantedAuthority> authorities = Collections.singletonList(oidcUserAuthority);

        // When
        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(authorities);

        // Then
        assertThat(result, hasSize(3));
        assertThat(result, containsInAnyOrder(
                new SimpleGrantedAuthority("ROLE_admin"),
                new SimpleGrantedAuthority("ROLE_user"),
                new SimpleGrantedAuthority("ROLE_moderator")
        ));
    }

    @Test
    void mapAuthorities_withOidcUserAuthority_fallsBackToGroupsClaim() {
        // Given
        when(oidcUserAuthority.getUserInfo()).thenReturn(userInfo);
        when(userInfo.hasClaim("realm_access")).thenReturn(false);
        when(userInfo.hasClaim("groups")).thenReturn(true);
        when(userInfo.getClaim("groups")).thenReturn(Arrays.asList("developer", "tester"));

        Collection<GrantedAuthority> authorities = Collections.singletonList(oidcUserAuthority);

        // When
        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(authorities);

        // Then
        assertThat(result, hasSize(2));
        assertThat(result, containsInAnyOrder(
                new SimpleGrantedAuthority("ROLE_developer"),
                new SimpleGrantedAuthority("ROLE_tester")
        ));
    }

    @Test
    void mapAuthorities_withOidcUserAuthority_noRolesFound_returnsEmptySet() {
        // Given
        when(oidcUserAuthority.getUserInfo()).thenReturn(userInfo);
        when(userInfo.hasClaim("realm_access")).thenReturn(false);
        when(userInfo.hasClaim("groups")).thenReturn(false);

        Collection<GrantedAuthority> authorities = Collections.singletonList(oidcUserAuthority);

        // When
        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(authorities);

        // Then
        assertThat(result, is(empty()));
    }

    @Test
    void mapAuthorities_withOidcUserAuthority_nullRolesInRealmAccess_fallsBackToGroups() {
        // Given
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", null);

        when(oidcUserAuthority.getUserInfo()).thenReturn(userInfo);
        when(userInfo.hasClaim("realm_access")).thenReturn(true);
        when(userInfo.getClaimAsMap("realm_access")).thenReturn(realmAccess);
        when(userInfo.hasClaim("groups")).thenReturn(true);
        when(userInfo.getClaim("groups")).thenReturn(Arrays.asList("fallback-role"));

        Collection<GrantedAuthority> authorities = Collections.singletonList(oidcUserAuthority);

        // When
        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(authorities);

        // Then
        assertThat(result, hasSize(1));
        assertThat(result, contains(new SimpleGrantedAuthority("ROLE_fallback-role")));
    }

    @Test
    void mapAuthorities_withOAuth2UserAuthority_extractsRolesFromRealmAccess() {
        // Given
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Arrays.asList("oauth2-admin", "oauth2-user"));

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("realm_access", realmAccess);

        OAuth2UserAuthority oauth2UserAuthority = new OAuth2UserAuthority(userAttributes);
        Collection<GrantedAuthority> authorities = Collections.singletonList(oauth2UserAuthority);

        // When
        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(authorities);

        // Then
        assertThat(result, hasSize(2));
        assertThat(result, containsInAnyOrder(
                new SimpleGrantedAuthority("ROLE_oauth2-admin"),
                new SimpleGrantedAuthority("ROLE_oauth2-user")
        ));
    }

    @Test
    void mapAuthorities_withOAuth2UserAuthority_noRealmAccess_returnsEmptySet() {
        // Given
        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("sub", "user123"); // OAuth2UserAuthority requires non-empty attributes
        OAuth2UserAuthority oauth2UserAuthority = new OAuth2UserAuthority(userAttributes);
        Collection<GrantedAuthority> authorities = Collections.singletonList(oauth2UserAuthority);

        // When
        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(authorities);

        // Then
        assertThat(result, is(empty()));
    }

    @Test
    void mapAuthorities_withOAuth2UserAuthority_nullRolesInRealmAccess_returnsEmptySet() {
        // Given
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", null);

        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("realm_access", realmAccess);

        OAuth2UserAuthority oauth2UserAuthority = new OAuth2UserAuthority(userAttributes);
        Collection<GrantedAuthority> authorities = Collections.singletonList(oauth2UserAuthority);

        // When
        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(authorities);

        // Then
        assertThat(result, is(empty()));
    }

    @Test
    void generateAuthoritiesFromClaim_filtersNullAndEmptyRoles() {
        // Given
        when(oidcUserAuthority.getUserInfo()).thenReturn(userInfo);
        when(userInfo.hasClaim("realm_access")).thenReturn(true);

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Arrays.asList("valid-role", null, "", "  ", "another-valid-role"));
        when(userInfo.getClaimAsMap("realm_access")).thenReturn(realmAccess);

        Collection<GrantedAuthority> authorities = Collections.singletonList(oidcUserAuthority);

        // When
        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(authorities);

        // Then
        assertThat(result, hasSize(2));
        assertThat(result, containsInAnyOrder(
                new SimpleGrantedAuthority("ROLE_valid-role"),
                new SimpleGrantedAuthority("ROLE_another-valid-role")
        ));
    }

    @Test
    void mapAuthorities_trimsWhitespaceFromRoles() {
        // Given
        when(oidcUserAuthority.getUserInfo()).thenReturn(userInfo);
        when(userInfo.hasClaim("realm_access")).thenReturn(true);

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Arrays.asList("  admin  ", "\tuser\t", "\nrole\n"));
        when(userInfo.getClaimAsMap("realm_access")).thenReturn(realmAccess);

        Collection<GrantedAuthority> authorities = Collections.singletonList(oidcUserAuthority);

        // When
        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(authorities);

        // Then
        assertThat(result, hasSize(3));
        assertThat(result, containsInAnyOrder(
                new SimpleGrantedAuthority("ROLE_admin"),
                new SimpleGrantedAuthority("ROLE_user"),
                new SimpleGrantedAuthority("ROLE_role")
        ));
    }

    @Test
    void mapAuthorities_withMultipleAuthorities_processesOnlyFirst() {
        // Given
        when(oidcUserAuthority.getUserInfo()).thenReturn(userInfo);
        when(userInfo.hasClaim("realm_access")).thenReturn(true);

        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", Arrays.asList("first-authority-role"));
        when(userInfo.getClaimAsMap("realm_access")).thenReturn(realmAccess);

        // Create a second authority that shouldn't be processed
        SimpleGrantedAuthority secondAuthority = new SimpleGrantedAuthority("ROLE_ignored");
        Collection<GrantedAuthority> authorities = Arrays.asList(oidcUserAuthority, secondAuthority);

        // When
        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(authorities);

        // Then
        assertThat(result, hasSize(1));
        assertThat(result, contains(new SimpleGrantedAuthority("ROLE_first-authority-role")));
    }

    @Test
    void mapAuthorities_withUnknownAuthorityType_returnsEmptySet() {
        // Given
        SimpleGrantedAuthority unknownAuthority = new SimpleGrantedAuthority("ROLE_unknown");
        Collection<GrantedAuthority> authorities = Collections.singletonList(unknownAuthority);

        // When
        Collection<? extends GrantedAuthority> result = mapper.mapAuthorities(authorities);

        // Then
        assertThat(result, is(empty()));
    }
}