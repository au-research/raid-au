package au.org.raid.api.service;

import au.org.raid.api.client.repository.DataciteRepositoryClient;
import au.org.raid.api.model.datacite.repository.DataciteRepository;
import au.org.raid.api.model.datacite.repository.DataciteRepositoryFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryServiceTest {
    @Mock
    private DataciteRepositoryFactory repositoryFactory;
    @Mock
    private DataciteRepositoryClient repositoryClient;
    @InjectMocks
    private RepositoryService repositoryService;

    @Test
    @DisplayName("create() returns repository on first attempt")
    void create_ReturnsRepositoryOnFirstAttempt() {
        final var name = "test-name";
        final var password = "test-password";
        final var repository = DataciteRepository.builder().build();
        final var createdRepository = DataciteRepository.builder().build();

        when(repositoryFactory.create(name, password)).thenReturn(repository);
        when(repositoryClient.createRepository(repository)).thenReturn(createdRepository);

        final var result = repositoryService.create(name, password);

        assertThat(result, is(createdRepository));
        verify(repositoryClient, times(1)).createRepository(repository);
    }

    @Test
    @DisplayName("create() retries on 422 and succeeds")
    void create_RetriesOn422AndSucceeds() {
        final var name = "test-name";
        final var password = "test-password";
        final var repository = DataciteRepository.builder().build();
        final var createdRepository = DataciteRepository.builder().build();

        when(repositoryFactory.create(name, password)).thenReturn(repository);
        when(repositoryClient.createRepository(repository))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(422)))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(422)))
                .thenReturn(createdRepository);

        final var result = repositoryService.create(name, password);

        assertThat(result, is(createdRepository));
        verify(repositoryClient, times(3)).createRepository(repository);
    }

    @Test
    @DisplayName("create() throws exception after max attempts with 422 errors")
    void create_ThrowsExceptionAfterMaxAttempts() {
        final var name = "test-name";
        final var password = "test-password";
        final var repository = DataciteRepository.builder().build();

        when(repositoryFactory.create(name, password)).thenReturn(repository);
        when(repositoryClient.createRepository(repository))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(422)));

        final var exception = assertThrows(RuntimeException.class,
                () -> repositoryService.create(name, password));

        assertThat(exception.getMessage(), is("Too many attempts. Unable to create Datacite repository"));
        verify(repositoryClient, times(5)).createRepository(repository);
    }

    @Test
    @DisplayName("create() throws non-422 exception immediately")
    void create_ThrowsNon422ExceptionImmediately() {
        final var name = "test-name";
        final var password = "test-password";
        final var repository = DataciteRepository.builder().build();

        when(repositoryFactory.create(name, password)).thenReturn(repository);
        when(repositoryClient.createRepository(repository))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)));

        assertThrows(HttpClientErrorException.class,
                () -> repositoryService.create(name, password));

        verify(repositoryClient, times(1)).createRepository(repository);
    }

    @Test
    @DisplayName("create() throws 500 error immediately without retry")
    void create_Throws500ErrorImmediately() {
        final var name = "test-name";
        final var password = "test-password";
        final var repository = DataciteRepository.builder().build();

        when(repositoryFactory.create(name, password)).thenReturn(repository);
        when(repositoryClient.createRepository(repository))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(500)));

        assertThrows(HttpClientErrorException.class,
                () -> repositoryService.create(name, password));

        verify(repositoryClient, times(1)).createRepository(repository);
    }

    @Test
    @DisplayName("BUG: create() should regenerate repository on 422 retry to get new random symbol")
    void create_ShouldRegenerateRepositoryOnRetry() {
        // This test demonstrates a potential bug: when a 422 occurs (e.g., due to duplicate symbol),
        // the retry uses the SAME repository object with the SAME symbol, so all retries will fail.
        // If the 422 is due to a symbol collision, the factory should be called again to generate
        // a new random symbol for each retry attempt.

        final var name = "test-name";
        final var password = "test-password";
        final var repository = DataciteRepository.builder().build();
        final var createdRepository = DataciteRepository.builder().build();

        when(repositoryFactory.create(name, password)).thenReturn(repository);
        when(repositoryClient.createRepository(repository))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(422)))
                .thenReturn(createdRepository);

        repositoryService.create(name, password);

        // BUG: Currently the factory is only called once, so if a 422 is due to duplicate symbol,
        // all retries will fail with the same symbol.
        // The factory SHOULD be called on each retry to generate a new random symbol.
        // This assertion will FAIL because the factory is only called once.
        verify(repositoryFactory, times(2)).create(name, password);
    }
}