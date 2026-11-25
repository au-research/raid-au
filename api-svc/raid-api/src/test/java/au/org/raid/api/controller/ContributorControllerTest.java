package au.org.raid.api.controller;

import au.org.raid.api.service.ContributorStatusService;
import au.org.raid.idl.raidv2.model.ContributorStatusUpdate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributorControllerTest {
    private static final String ORCID = "https://orcid.org/0009-0002-5128-5184";

    @Mock
    private ContributorStatusService contributorStatusService;
    @InjectMocks
    private ContributorController controller;

    @Test
    @DisplayName("updateContributorStatus() returns 204 when the contributor exists")
    void noContentWhenFound() {
        when(contributorStatusService.updateStatus(ORCID, "AUTHENTICATED")).thenReturn(true);

        final var response = controller.updateContributorStatus(new ContributorStatusUpdate()
                .id(ORCID)
                .status(ContributorStatusUpdate.StatusEnum.AUTHENTICATED));

        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
    }

    @Test
    @DisplayName("updateContributorStatus() returns 404 when no contributor has the id")
    void notFoundWhenUnknown() {
        when(contributorStatusService.updateStatus(ORCID, "AUTHENTICATION_REVOKED")).thenReturn(false);

        final var response = controller.updateContributorStatus(new ContributorStatusUpdate()
                .id(ORCID)
                .status(ContributorStatusUpdate.StatusEnum.AUTHENTICATION_REVOKED));

        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("updateContributorStatus() lets a partial failure through, so the updater sees an error and retries")
    void partialFailurePropagates() {
        when(contributorStatusService.updateStatus(ORCID, "AUTHENTICATED"))
                .thenThrow(new ContributorStatusService.ContributorStatusRefreshException(1, 2));

        assertThrows(ContributorStatusService.ContributorStatusRefreshException.class,
                () -> controller.updateContributorStatus(new ContributorStatusUpdate()
                        .id(ORCID)
                        .status(ContributorStatusUpdate.StatusEnum.AUTHENTICATED)));
    }
}
