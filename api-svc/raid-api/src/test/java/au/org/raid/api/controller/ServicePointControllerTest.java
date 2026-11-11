package au.org.raid.api.controller;

import au.org.raid.api.service.ServicePointService;
import au.org.raid.idl.raidv2.model.PublicServicePoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicePointControllerTest {
    @Mock
    private ServicePointService servicePointService;
    @InjectMocks
    private ServicePointController controller;

    @Test
    @DisplayName("findAllPublicServicePoints() returns 200 with the public service points")
    void findAllPublicServicePoints() {
        final var servicePoints = List.of(new PublicServicePoint().id(1L).name("n").identifierOwner("o"));
        when(servicePointService.findAllPublic()).thenReturn(servicePoints);

        final var response = controller.findAllPublicServicePoints();

        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(servicePoints));
        verify(servicePointService).findAllPublic();
    }
}
