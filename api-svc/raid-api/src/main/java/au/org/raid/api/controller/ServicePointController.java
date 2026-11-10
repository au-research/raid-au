package au.org.raid.api.controller;

import au.org.raid.api.service.ServicePointService;
import au.org.raid.idl.raidv2.api.ServicePointApi;
import au.org.raid.idl.raidv2.model.PublicServicePoint;
import au.org.raid.idl.raidv2.model.ServicePoint;
import au.org.raid.idl.raidv2.model.ServicePointCreateRequest;
import au.org.raid.idl.raidv2.model.ServicePointUpdateRequest;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.net.URI;
import java.util.List;

@Controller
@CrossOrigin
@RequiredArgsConstructor
@SecurityScheme(name = "bearerAuth", scheme = "bearer", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
public class ServicePointController implements ServicePointApi {
    private final ServicePointService servicePointService;

    public ResponseEntity<List<ServicePoint>> findAllServicePoints() {
        return ResponseEntity.ok(servicePointService.findAll());
    }

    public ResponseEntity<List<PublicServicePoint>> findAllPublicServicePoints() {
        return ResponseEntity.ok(servicePointService.findAllPublic());
    }

    public ResponseEntity<ServicePoint> createServicePoint(
            final ServicePointCreateRequest servicePoint
    ) {

        final var created = servicePointService.create(servicePoint);

        return ResponseEntity
                .created(URI.create("/service-point/%d".formatted(created.getId())))
                .body(created);
    }

    public ResponseEntity<ServicePoint> updateServicePoint(
            final Long id,
            final ServicePointUpdateRequest servicePoint
    ) {
        return ResponseEntity.ok(servicePointService.update(servicePoint));
    }

    public ResponseEntity<ServicePoint> findServicePointById(final Long id) {
        return ResponseEntity.of(servicePointService.findById(id));
    }
}
