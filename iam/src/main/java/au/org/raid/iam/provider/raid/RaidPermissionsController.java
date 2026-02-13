package au.org.raid.iam.provider.raid;

import au.org.raid.iam.provider.exception.UserNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

@Slf4j
@Provider
public class RaidPermissionsController {
    private static final String OPERATOR_ROLE_NAME = "operator";
    private static final String GROUP_ADMIN_ROLE_NAME = "group-admin";
    private static final String SERVICE_POINT_USER_ROLE = "service-point-user";
    public static final String RAID_USER_ROLE = "raid-user";
    public static final String USER_RAIDS_ATTRIBUTE = "userRaids";
    public static final String ADMIN_RAIDS_ATTRIBUTE = "adminRaids";
    private final AuthenticationManager.AuthResult auth;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final KeycloakSession session;
    public RaidPermissionsController(final KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
    }

    private Cors addCorsHeaders(final String... allowedMethods) {
        log.debug("Calling Cors");
        final var cors = session.getProvider(Cors.class);

        cors.allowedOrigins(
                "http://localhost:7080",
                
                "https://app.test.raid.org.au",
                "https://app3.test.raid.org.au",
                
                "https://app.demo.raid.org.au",
                "https://app3.demo.raid.org.au",

                "https://app.stage.raid.org.au",
                "https://app3.stage.raid.org.au",

                "https://app.prod.raid.org.au");

        cors.allowedMethods(allowedMethods);
        cors.auth();

        return cors;
    }

    @OPTIONS
    @Path("/raid-user")
    public Response addRaidUserPreflight() {
        return Response.fromResponse(addCorsHeaders("POST", "DELETE")
                        .preflight()
                        .builder(Response.ok())
                        .build())
                .build();
    }

    @POST
    @Path("/raid-user")
    @SneakyThrows
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addRaidUser(RaidUserPermissionsRequest request) {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var client = auth.getClient();

        if (client.getRolesStream().anyMatch(role -> role.getName().equals("raid-permissions-admin"))) {
                addUserToRaid(request.getUserId(), request.getHandle());
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return Response.fromResponse(
                        addCorsHeaders("POST")
                                .builder(Response.ok())
                                .build()
                )
                .entity("{}")
                .build();
    }

    @DELETE
    @Path("/raid-user")
    @SneakyThrows
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeRaidUser(RaidUserPermissionsRequest request) {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var client = auth.getClient();

        if (client.getRolesStream().anyMatch(role -> role.getName().equals("raid-permissions-admin"))) {
            removeUserFromRaid(request.getUserId(), request.getHandle());
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return Response.fromResponse(
                        addCorsHeaders("POST")
                                .builder(Response.ok())
                                .build()
                )
                .entity("{}")
                .build();
    }

    @OPTIONS
    @Path("/raid-admin")
    public Response addRaidAdminPreflight() {
        return Response.fromResponse(addCorsHeaders("POST", "DELETE")
                        .preflight()
                        .builder(Response.ok())
                        .build())
                .build();
    }


    @POST
    @Path("/raid-admin")
    @SneakyThrows
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addRaidAdmin(RaidUserPermissionsRequest request) {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var currentUser = auth.getSession().getUser();

        if (currentUser.getRoleMappingsStream().anyMatch(role -> role.getName().equals("service-point-user"))) {
            final var role = auth.getSession().getRealm().getRole("raid-admin");
            if (role == null) {
                throw new IllegalStateException("'raid-admin' role not found");
            }
            final var user = session.users().getUserByUsername(auth.getSession().getRealm(), request.getUserId());

            user.grantRole(role);
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return Response.fromResponse(
                        addCorsHeaders("POST")
                                .builder(Response.ok())
                                .build()
                )
                .entity("{}")
                .build();
    }

    @DELETE
    @Path("/raid-admin")
    @SneakyThrows
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeRaidAdmin(RaidUserPermissionsRequest request) {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var currentUser = auth.getSession().getUser();

        if (currentUser.getRoleMappingsStream().anyMatch(role -> role.getName().equals("service-point-user"))) {
            final var role = auth.getSession().getRealm().getRole("raid-admin");
            if (role == null) {
                throw new IllegalStateException("'raid-admin' role not found");
            }
            final var user = session.users().getUserByUsername(auth.getSession().getRealm(), request.getUserId());

            user.deleteRoleMapping(role);
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return Response.fromResponse(
                        addCorsHeaders("POST")
                                .builder(Response.ok())
                                .build()
                )
                .entity("{}")
                .build();
    }


    @POST
    @Path("/admin-raids")
    @SneakyThrows
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addAdminRaid(AdminRaidsRequest request) {
        if (this.auth == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        final var client = auth.getClient();

        if (client.getRolesStream().anyMatch(role -> role.getName().equals("raid-permissions-admin"))) {
            final var user = session.users().getUserById(session.getContext().getRealm(), request.getUserId());

            final var adminRaids = user.getAttributeStream(ADMIN_RAIDS_ATTRIBUTE).collect(Collectors.toSet());

            adminRaids.add(request.getHandle());

            user.setAttribute(ADMIN_RAIDS_ATTRIBUTE, new ArrayList<>(adminRaids));

        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return Response.fromResponse(
                        addCorsHeaders("POST")
                                .builder(Response.ok())
                                .build()
                )
                .entity("{}")
                .build();
    }

    private void addUserToRaid(final String userId, final String handle) {
        final var user = session.users().getUserById(session.getContext().getRealm(), userId);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        final var userRaids = user.getAttributeStream(USER_RAIDS_ATTRIBUTE).collect(Collectors.toCollection(HashSet::new));
        userRaids.add(handle);
        user.setAttribute(USER_RAIDS_ATTRIBUTE, new ArrayList<>(userRaids));

        final var role = session.getContext().getRealm().getRole(RAID_USER_ROLE);
        if (role == null) {
            throw new IllegalStateException("'%s' role not found".formatted(RAID_USER_ROLE));
        }

        user.grantRole(role);
    }


    private void removeUserFromRaid(final String userId, final String handle) {
        final var user = session.users().getUserById(session.getContext().getRealm(), userId);
        final var userRaids = user.getAttributeStream(USER_RAIDS_ATTRIBUTE).collect(Collectors.toCollection(HashSet::new));
        userRaids.remove(handle);
        user.setAttribute(USER_RAIDS_ATTRIBUTE, new ArrayList<>(userRaids));
    }
}
