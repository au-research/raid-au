package au.org.raid.inttest;

import au.org.raid.inttest.config.AuthConfig;
import au.org.raid.inttest.dto.keycloak.RaidUserPermissionsRequest;
import au.org.raid.inttest.service.Handle;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class RaidPermissionsIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private AuthConfig authConfig;

    @Test
    @DisplayName("Raid user cannot read raid without permissions")
    void raidUserReadWithoutPermissions() {
        final var raidUserContext = userService.createUser("raid-au", "raid-user");
        final var raidAdminuserContext = userService.createUser("raid-au", "raid-admin");
        final var mintedRaid = testClient.raidApi(raidAdminuserContext.getToken()).mintRaid(createRequest).getBody();

        assert mintedRaid != null;

        final var handle = new Handle(mintedRaid.getIdentifier().getId());

        try {
            testClient.raidApi(raidUserContext.getToken()).findRaidByName(handle.getPrefix(), handle.getSuffix());
        } catch (final FeignException e) {
            assertThat(e.status(), is(403));
            // pass
        } catch (final Exception e) {
            log.error("Failed", e);
            fail(e.getMessage());
        } finally {
            userService.deleteUser(raidAdminuserContext.getId());
            userService.deleteUser(raidUserContext.getId());
        }
    }

    @Test
    @DisplayName("Raid user cannot update raid without permissions")
    void raidUserUpdateWithoutPermissions() {

        final var raidUserContext = userService.createUser("raid-au", "raid-user");
        final var raidAdminUserContext = userService.createUser("raid-au", "raid-admin");
        final var mintedRaid = testClient.raidApi(raidAdminUserContext.getToken()).mintRaid(createRequest).getBody();

        assert mintedRaid != null;

        final var handle = new Handle(mintedRaid.getIdentifier().getId());

        try {
            testClient.raidApi(raidUserContext.getToken())
                    .updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(mintedRaid));
        } catch (final FeignException e) {
            assertThat(e.status(), is(403));
            // pass
        } catch (final Exception e) {
            log.error("Failed", e);
            fail(e.getMessage());
        } finally {
            userService.deleteUser(raidAdminUserContext.getId());
            userService.deleteUser(raidUserContext.getId());
        }
    }

    @Test
    @DisplayName("Raid user can read raid with permissions")
    void raidUserReadWithPermissions() {
        final var raidUserContext = userService.createUser("raid-au", "raid-user");
        final var raidAdminUserContext = userService.createUser("raid-au", "raid-admin");

        final var mintedRaid = testClient.raidApi(raidAdminUserContext.getToken()).mintRaid(createRequest).getBody();

        assert mintedRaid != null;

        final var handle = new Handle(mintedRaid.getIdentifier().getId());

        final var raidUserPermissionsRequest = RaidUserPermissionsRequest.builder()
                .userId(raidUserContext.getId())
                .handle(handle.toString())
                .build();

        keycloakClient.keycloakApi(authConfig.getIntegrationTestClient()).addRaidUser(raidUserPermissionsRequest);

        try {
            final var token = tokenService.getUserToken(raidUserContext.getUsername(), raidUserContext.getPassword());
            testClient.raidApi(token).findRaidByName(handle.getPrefix(), handle.getSuffix());
        } catch (final Exception e) {
            fail("Raid user should be able to read raid");
        } finally {
            userService.deleteUser(raidUserContext.getId());
            userService.deleteUser(raidAdminUserContext.getId());
        }
    }

    @Test
    @DisplayName("Raid user can update raid with permissions")
    void raidUserUpdateWithPermissions() {
        final var raidUserContext = userService.createUser("raid-au", "raid-user");
        final var raidAdminUserContext = userService.createUser("raid-au", "raid-admin");

        final var mintedRaid = testClient.raidApi(raidAdminUserContext.getToken()).mintRaid(createRequest).getBody();

        assert mintedRaid != null;

        final var handle = new Handle(mintedRaid.getIdentifier().getId());

        final var raidUserPermissionsRequest = RaidUserPermissionsRequest.builder()
                .userId(raidUserContext.getId())
                .handle(handle.toString())
                .build();

        keycloakClient.keycloakApi(authConfig.getIntegrationTestClient())
                .addRaidUser(raidUserPermissionsRequest);

        try {
            final var token = tokenService.getUserToken(raidUserContext.getUsername(), raidUserContext.getPassword());
            testClient.raidApi(token)
                    .updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(mintedRaid));
        } catch (final Exception e) {
            fail("Raid user should be able to update raid");
        } finally {
            userService.deleteUser(raidUserContext.getId());
            userService.deleteUser(raidAdminUserContext.getId());
        }
    }

    @Test
    @DisplayName("Raid admin can't read embargoed raids from same service point if no permissions")
    void raidAdminUnableToReadEmbargoedRaids() {
        final var raidAdminUserContext = userService.createUser("raid-au", "raid-admin");

        try {
            final var raid1 = testClient.raidApi(userContext.getToken()).mintRaid(createRequest).getBody();
            assert raid1 != null;
            final var raid2 = testClient.raidApi(raidAdminUserContext.getToken()).mintRaid(createRequest).getBody();
            assert raid2 != null;

            final var token = tokenService.getUserToken(raidAdminUserContext.getUsername(), raidAdminUserContext.getPassword());
            final var response = testClient.raidApi(token).findAllRaids(null, null, null);

            assert response.getBody() != null;

            final var raids = response.getBody().stream().map(raidDto -> raidDto.getIdentifier().getId()).toList();
            assertThat(raids, hasItem(raid2.getIdentifier().getId()));
            assertThat(raids, not(hasItem(raid1.getIdentifier().getId())));
        } finally {
            userService.deleteUser(raidAdminUserContext.getId());
        }
    }

    @Test
    @DisplayName("Should return 403 response when searching by contributor id without 'pid-searcher' role")
    void searchByContributorIdFails() {
        try {
            final var contributorId = "https://orcid.org/0009-0006-4129-5257";
            raidApi.findAllRaids(Collections.emptyList(), contributorId, null);
            fail("Expecting 403 response but got 200");
        } catch (FeignException e) {
            assertThat(e.status(), is(403));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    @DisplayName("Should return 403 response when searching by organisation id without 'pid-searcher' role")
    void searchByOrganisationIdFails() {
        try {
            final var organisationId = "https://ror.org/038sjwq14";
            raidApi.findAllRaids(Collections.emptyList(), null, organisationId);
            fail("Expecting 403 response but got 200");
        } catch (FeignException e) {
            assertThat(e.status(), is(403));
        } catch (Exception e) {
            fail();
        }
    }
}