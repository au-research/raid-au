package au.org.raid.api.client.repository;

import au.org.raid.api.config.properties.RepositoryClientProperties;
import au.org.raid.api.factory.HttpEntityFactory;
import au.org.raid.api.model.datacite.repository.DataciteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataciteRepositoryClientTest {
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RepositoryClientProperties properties;
    @Mock
    private HttpEntityFactory httpEntityFactory;
    @InjectMocks
    private DataciteRepositoryClient dataciteRepositoryClient;

    @Test
    @DisplayName("createRepository() returns created repository")
    void createRepository_ReturnsCreatedRepository() {
        final var url = "https://api.datacite.org/repositories";
        final var username = "test-username";
        final var password = "test-password";
        final var repository = DataciteRepository.builder().build();
        final var createdRepository = DataciteRepository.builder().build();
        final var httpEntity = new HttpEntity<>(repository);
        final var response = new ResponseEntity<>(createdRepository, HttpStatusCode.valueOf(201));

        when(properties.getUrl()).thenReturn(url);
        when(properties.getUsername()).thenReturn(username);
        when(properties.getPassword()).thenReturn(password);
        when(httpEntityFactory.create(repository, username, password)).thenReturn(httpEntity);
        when(restTemplate.exchange(url, HttpMethod.POST, httpEntity, DataciteRepository.class)).thenReturn(response);

        final var result = dataciteRepositoryClient.createRepository(repository);

        assertThat(result, is(createdRepository));
    }

    @Test
    @DisplayName("createRepository() propagates HttpClientErrorException")
    void createRepository_PropagatesHttpClientErrorException() {
        final var url = "https://api.datacite.org/repositories";
        final var username = "test-username";
        final var password = "test-password";
        final var repository = DataciteRepository.builder().build();
        final var httpEntity = new HttpEntity<>(repository);

        when(properties.getUrl()).thenReturn(url);
        when(properties.getUsername()).thenReturn(username);
        when(properties.getPassword()).thenReturn(password);
        when(httpEntityFactory.create(repository, username, password)).thenReturn(httpEntity);
        when(restTemplate.exchange(url, HttpMethod.POST, httpEntity, DataciteRepository.class))
                .thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(422)));

        final var exception = assertThrows(HttpClientErrorException.class,
                () -> dataciteRepositoryClient.createRepository(repository));

        assertThat(exception.getStatusCode(), is(HttpStatusCode.valueOf(422)));
    }

    @Test
    @DisplayName("createRepository() returns null when response body is null")
    void createRepository_ReturnsNullWhenResponseBodyIsNull() {
        final var url = "https://api.datacite.org/repositories";
        final var username = "test-username";
        final var password = "test-password";
        final var repository = DataciteRepository.builder().build();
        final var httpEntity = new HttpEntity<>(repository);
        final DataciteRepository nullBody = null;
        final var response = ResponseEntity.status(HttpStatusCode.valueOf(201)).body(nullBody);

        when(properties.getUrl()).thenReturn(url);
        when(properties.getUsername()).thenReturn(username);
        when(properties.getPassword()).thenReturn(password);
        when(httpEntityFactory.create(repository, username, password)).thenReturn(httpEntity);
        when(restTemplate.exchange(url, HttpMethod.POST, httpEntity, DataciteRepository.class)).thenReturn(response);

        final var result = dataciteRepositoryClient.createRepository(repository);

        assertThat(result, is(nullValue()));
    }
}