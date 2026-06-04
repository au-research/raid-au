# ORCID Integration Client Stub

## Problem

The `OrcidIntegrationClient` makes real HTTP calls to the ORCID Integration API
(`orcid.{env}.raid.org`) for two operations:

1. `POST /raid-update` — notify the ORCID integration of contributor changes
2. `POST /find-contributor-by-orcid` — look up a contributor's authentication status

There is no stub for this client. Local development and integration tests require
either a running instance of the ORCID Integration API or a mock server on
`localhost:1080`. The existing stub pattern (used for ROR, DOI, GeoNames, etc.)
provides a proven alternative.

## Existing Stub Pattern

Each external service stub in `au.org.raid.api.service.stub` follows this pattern:

- **Extends** the real service/client class
- **Constructor** accepts `Long delayMilliseconds` and passes `null` to the
  parent for HTTP dependencies
- **Overrides** each public method to return canned responses after a simulated delay
- **Uses** constants from `InMemoryStubTestData` to trigger specific error scenarios
- **Wired** via a `@Bean` method in `ExternalPidService` that checks
  `StubProperties.{service}.isEnabled()`

## Proposed Changes

### 1. `OrcidIntegrationClientStub`

**File**: `raid-api/src/main/java/au/org/raid/api/service/stub/OrcidIntegrationClientStub.java`

Extends `OrcidIntegrationClient`, overrides both methods:

```java
package au.org.raid.api.service.stub;

import au.org.raid.api.dto.ContributorLookupResponse;
import au.org.raid.api.dto.RaidListenerMessage;
import au.org.raid.api.service.OrcidIntegrationClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static au.org.raid.api.service.stub.InMemoryStubTestData.NONEXISTENT_TEST_ORCID;
import static au.org.raid.api.service.stub.InMemoryStubTestData.SERVER_ERROR_TEST_ORCID;

@Slf4j
public class OrcidIntegrationClientStub extends OrcidIntegrationClient {

    private final Long delayMilliseconds;

    public OrcidIntegrationClientStub(final Long delayMilliseconds) {
        super(null, null, null);
        this.delayMilliseconds = delayMilliseconds;
    }

    @Override
    @SneakyThrows
    public void post(final RaidListenerMessage message) {
        log.info("stub: simulating POST /raid-update");
        simulateDelay();
        log.info("stub: raid-update accepted for handle {}",
                message.getRaid().getIdentifier().getId());
    }

    @Override
    @SneakyThrows
    public Optional<ContributorLookupResponse> findByOrcid(final String orcid) {
        log.info("stub: simulating POST /find-contributor-by-orcid for {}", orcid);
        simulateDelay();

        if (NONEXISTENT_TEST_ORCID.equals(orcid)) {
            log.info("stub: returning empty for nonexistent test ORCID");
            return Optional.empty();
        }

        if (SERVER_ERROR_TEST_ORCID.equals(orcid)) {
            throw new RuntimeException(
                    "stub: simulated server error for " + orcid);
        }

        return Optional.of(ContributorLookupResponse.builder()
                .orcid(orcid)
                .status("AUTHENTICATED")
                .name("Stub Contributor")
                .build());
    }

    @SneakyThrows
    private void simulateDelay() {
        final var start = Instant.now();
        Thread.sleep(delayMilliseconds);
        final var duration = Duration.between(start, Instant.now());
        log.debug("stub delay: {}.{}s",
                duration.getSeconds(), duration.getNano());
    }
}
```

**Design decisions**:

- `post()` is a fire-and-forget no-op — the real handler publishes to SNS
  topics, which is irrelevant locally.
- `findByOrcid()` returns `AUTHENTICATED` by default, matching the most common
  happy-path scenario. Tests can use the special ORCIDs from
  `InMemoryStubTestData` to trigger not-found or error cases.
- The `super(null, null, null)` call passes nulls for `RestTemplate`,
  `OrcidIntegrationProperties`, and `HttpEntityFactory` since the stub never
  delegates to the parent.

### 2. `StubProperties` — add `orcidIntegration`

The existing `orcid` property controls the *URI validator* stub. Add a separate
property for the integration client.

**File**: `raid-api/src/main/java/au/org/raid/api/config/properties/StubProperties.java`

Add a new inner class and field:

```java
private OrcidIntegration orcidIntegration;

@Data
public static class OrcidIntegration {
    private boolean enabled;
    private Long delay;
}
```

### 3. `application.yaml` — add config

Under `raid.stub`, add:

```yaml
raid:
  stub:
    orcid-integration:
      enabled: true
      delay: 150
```

### 4. Bean wiring in `ExternalPidService`

Add a new `@Bean` method:

```java
@Bean
@Primary
public OrcidIntegrationClient orcidIntegrationClient(
        StubProperties stubProperties,
        RestTemplate restTemplate,
        OrcidIntegrationProperties orcidIntegrationProperties,
        HttpEntityFactory httpEntityFactory
) {
    if (stubProperties.getOrcidIntegration() != null
            && stubProperties.getOrcidIntegration().isEnabled()) {
        log.warn("using the in-memory ORCID integration client");
        return new OrcidIntegrationClientStub(
                stubProperties.getOrcidIntegration().getDelay());
    }

    return new OrcidIntegrationClient(
            restTemplate, orcidIntegrationProperties, httpEntityFactory);
}
```

**Note**: `OrcidIntegrationClient` is currently annotated `@Component`, which
means Spring auto-registers it as a bean. The `@Bean @Primary` method in
`ExternalPidService` will take precedence when the stub is enabled, but the
`@Component` annotation should be removed from `OrcidIntegrationClient` to
avoid having two beans of the same type when the stub is disabled. Alternatively,
keep `@Component` and use `@ConditionalOnProperty` on the bean method — but
removing `@Component` is more consistent with how the other services
(`RorService`, `DoiService`, etc.) work (none are annotated `@Component`).

### 5. Remove `@Component` from `OrcidIntegrationClient`

Remove the `@Component` annotation from `OrcidIntegrationClient` so that bean
creation is fully controlled by `ExternalPidService`:

```java
@Slf4j
@RequiredArgsConstructor
public class OrcidIntegrationClient {
    // ...
}
```

### 6. Unit tests for the stub

**File**: `raid-api/src/test/java/au/org/raid/api/service/stub/OrcidIntegrationClientStubTest.java`

Cover three scenarios:

| Test case | Input ORCID | Expected result |
|---|---|---|
| Happy path | Any valid ORCID | `Optional` with status `AUTHENTICATED` |
| Not found | `NONEXISTENT_TEST_ORCID` | `Optional.empty()` |
| Server error | `SERVER_ERROR_TEST_ORCID` | `RuntimeException` thrown |
| Post accepted | Any message | No exception, returns void |

## Summary of files to change

| File | Change |
|---|---|
| `service/stub/OrcidIntegrationClientStub.java` | **New** — stub implementation |
| `config/properties/StubProperties.java` | Add `OrcidIntegration` inner class and field |
| `config/bean/ExternalPidService.java` | Add `orcidIntegrationClient()` bean method |
| `service/OrcidIntegrationClient.java` | Remove `@Component` annotation |
| `src/main/resources/application.yaml` | Add `raid.stub.orcid-integration` config |
| `service/stub/OrcidIntegrationClientStubTest.java` | **New** — unit tests |

## OpenAPI Endpoint Coverage

Mapping from the `orcid-integration-api.openapi.yaml` endpoints to stub behaviour:

| Endpoint | Operation | Stub behaviour |
|---|---|---|
| `POST /raid-update` | `processRaidUpdate` | `post()` — logs and returns, no-op |
| `POST /find-contributor-by-orcid` | `findContributorByOrcid` | `findByOrcid()` — returns canned `ContributorLookupResponse` |
| `GET /oauth` | `orcidOAuthCallback` | Not called by `OrcidIntegrationClient` — browser redirect flow, out of scope |
