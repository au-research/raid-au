package au.org.raid.api.client.ror;

import au.org.raid.api.client.ror.dto.Name;
import au.org.raid.api.client.ror.dto.RorSchemaV21;
import au.org.raid.api.client.ror.dto.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RorClientTest {
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RorRequestEntityFactory requestEntityFactory;
    @InjectMocks
    private RorClient rorClient;


    @Test
    @DisplayName("Should fetch organisation with valid id")
    public void fetchOrganisation() {
        final var ror = "https://ror.org/038sjwq14";
        final var organisation = new RorSchemaV21();
        final var requestEntity = RequestEntity.get("").build();

        final var response = new ResponseEntity<>(organisation, HttpStatusCode.valueOf(200));

        when(requestEntityFactory.create(ror)).thenReturn(requestEntity);

        when(restTemplate.exchange(requestEntity, RorSchemaV21.class)).thenReturn(response);

        assertThat(rorClient.getOrganisation(ror), is(organisation));
    }


    @Test
    @DisplayName("Should fetch organisation with valid id")
    public void fetchName() {
        final var ror = "https://ror.org/038sjwq14";
        final var organisation = new RorSchemaV21();
        final var nameValue = "_name";
        final var name = new Name();
        name.setValue(nameValue);
        name.setTypes(Set.of(Type.ROR_DISPLAY));
        organisation.setNames(Set.of(name));
        final var requestEntity = RequestEntity.get("").build();

        final var response = new ResponseEntity<>(organisation, HttpStatusCode.valueOf(200));

        when(requestEntityFactory.create(ror)).thenReturn(requestEntity);

        when(restTemplate.exchange(requestEntity, RorSchemaV21.class)).thenReturn(response);

        assertThat(rorClient.getOrganisationName(ror), is(nameValue));
    }

    @Test
    @DisplayName("exists method should return true given an existing ROR")
    void existsReturnsTrue() {
        final var ror = "https://ror.org/038sjwq14";
        final var organisation = new RorSchemaV21();
        final var requestEntity = RequestEntity.get("").build();

        final var response = new ResponseEntity<>(organisation, HttpStatusCode.valueOf(200));

        when(requestEntityFactory.create(ror)).thenReturn(requestEntity);

        when(restTemplate.exchange(requestEntity, RorSchemaV21.class)).thenReturn(response);

        assertThat(rorClient.exists(ror), is(true));
    }

    @Test
    @DisplayName("exists method should return false given a non-existent ROR")
    void existsReturnsFalse() {
        final var ror = "https://ror.org/038sjwq14";
        final var requestEntity = RequestEntity.get("").build();

        final var exception = new HttpClientErrorException(HttpStatusCode.valueOf(404));

        when(requestEntityFactory.create(ror)).thenReturn(requestEntity);

        doThrow(exception).when(restTemplate).exchange(requestEntity, RorSchemaV21.class);

        assertThat(rorClient.exists(ror), is(false));
    }
}