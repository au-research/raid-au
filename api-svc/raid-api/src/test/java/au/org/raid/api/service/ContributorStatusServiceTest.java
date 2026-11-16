package au.org.raid.api.service;

import au.org.raid.api.repository.RaidContributorRepository;
import au.org.raid.api.service.raid.RaidService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributorStatusServiceTest {
    private static final String ORCID = "https://orcid.org/0009-0002-5128-5184";
    private static final String STATUS = "AUTHENTICATED";

    @Mock
    private ContributorService contributorService;
    @Mock
    private RaidContributorRepository raidContributorRepository;
    @Mock
    private RaidService raidService;
    @InjectMocks
    private ContributorStatusService contributorStatusService;

    @Test
    @DisplayName("An unknown contributor changes nothing and reports not found")
    void unknownContributor() {
        when(contributorService.updateStatus(ORCID, STATUS)).thenReturn(false);

        assertThat(contributorStatusService.updateStatus(ORCID, STATUS), is(false));

        verifyNoInteractions(raidContributorRepository, raidService);
    }

    @Test
    @DisplayName("The stored status is set first, then every RAiD naming the contributor is refreshed")
    void refreshesEveryRaid() {
        when(contributorService.updateStatus(ORCID, STATUS)).thenReturn(true);
        when(raidContributorRepository.findHandlesByContributorPid(ORCID)).thenReturn(List.of("10.1/a", "10.1/b"));
        when(raidService.refreshContributorStatus("10.1/a")).thenReturn(true);
        when(raidService.refreshContributorStatus("10.1/b")).thenReturn(false);

        assertThat(contributorStatusService.updateStatus(ORCID, STATUS), is(true));

        final var inOrder = inOrder(contributorService, raidService);
        inOrder.verify(contributorService).updateStatus(ORCID, STATUS);
        inOrder.verify(raidService).refreshContributorStatus("10.1/a");
        inOrder.verify(raidService).refreshContributorStatus("10.1/b");
    }

    @Test
    @DisplayName("A contributor on no RAiDs is still found")
    void contributorOnNoRaids() {
        when(contributorService.updateStatus(ORCID, STATUS)).thenReturn(true);
        when(raidContributorRepository.findHandlesByContributorPid(ORCID)).thenReturn(List.of());

        assertThat(contributorStatusService.updateStatus(ORCID, STATUS), is(true));

        verifyNoInteractions(raidService);
    }

    @Test
    @DisplayName("One failing RAiD doesn't stop the others, and the call fails so the updater retries")
    void carriesOnPastAFailureThenThrows() {
        when(contributorService.updateStatus(ORCID, STATUS)).thenReturn(true);
        when(raidContributorRepository.findHandlesByContributorPid(ORCID)).thenReturn(List.of("10.1/a", "10.1/b", "10.1/c"));
        when(raidService.refreshContributorStatus("10.1/a")).thenReturn(true);
        when(raidService.refreshContributorStatus("10.1/b")).thenThrow(new IllegalStateException("version conflict"));
        when(raidService.refreshContributorStatus("10.1/c")).thenReturn(true);

        final var exception = assertThrows(ContributorStatusService.ContributorStatusRefreshException.class,
                () -> contributorStatusService.updateStatus(ORCID, STATUS));

        assertThat(exception.getMessage(), containsString("1 of 3"));
        verify(raidService).refreshContributorStatus("10.1/c");
    }
}
