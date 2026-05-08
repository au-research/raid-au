package au.org.raid.inttest.service;

import au.org.raid.inttest.dto.keycloak.Group;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class AuthClient {
    private final String baseUrl;

    @SneakyThrows
    public Optional<Group> getGroup(final String token) {
        final var url = "http://localhost:8001/realms/raid/service-point";

        final var client = new OkHttpClient();

        final var request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + token)
                .url(url)
                .build();

        final var call = client.newCall(request);

        try (var response = call.execute()) {
            if (response.isSuccessful()) {
                assert response.body() != null;
                final var group = new ObjectMapper().readValue(response.body().string(), Group.class);

                return Optional.of(group);
            }
        }

        return Optional.empty();
    }

    @SneakyThrows
    public boolean revoke(final String groupId, final String userId, final String token) {
        final var url = "http://localhost:8001/realms/raid/service-point/revoke";

        final var objectMapper = new ObjectMapper();
        final var client = new OkHttpClient();

        final var requestBody = objectMapper.writeValueAsString(Map.of(
                "groupId", groupId,
                "userId", userId
        ));

        final var request = new Request.Builder()
                .put(RequestBody.create(requestBody.getBytes()))
                .addHeader("Authorization", "Bearer " + token)
                .url(url)
                .build();

        final var call = client.newCall(request);

        try (var response = call.execute()) {
            if (response.isSuccessful()) {
                return true;
            }
        }

        return false;
    }

    @SneakyThrows
    public boolean grant(final String groupId, final String userId, final String token) {
        final var url = "http://localhost:8001/realms/raid/service-point/grant";

        final var objectMapper = new ObjectMapper();
        final var client = new OkHttpClient();

        final var requestBody = objectMapper.writeValueAsString(Map.of(
                "groupId", groupId,
                "userId", userId
        ));

        final var request = new Request.Builder()
                .put(RequestBody.create(requestBody.getBytes()))
                .addHeader("Authorization", "Bearer " + token)
                .url(url)
                .build();

        final var call = client.newCall(request);

        try (var response = call.execute()) {
            if (response.isSuccessful()) {
                return true;
            }
        }

        return false;
    }
}
