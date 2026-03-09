package au.org.raid.api.repository;

import au.org.raid.db.jooq.tables.records.AccessTypeRecord;
import au.org.raid.db.jooq.tables.records.AccessTypeSchemaRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SelectConditionStep;
import org.jooq.SelectWhereStep;
import org.jooq.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifies that @Cacheable on reference data repository methods prevents repeated
 * DSLContext calls for the same arguments, and does not suppress calls for
 * different arguments.
 *
 * Uses a minimal Spring context (no DB, no security) to activate the Spring
 * proxy that backs @Cacheable. Plain Mockito (@ExtendWith(MockitoExtension))
 * operates on the raw object and bypasses the proxy, so a Spring context is
 * required to test caching behaviour.
 */
@SpringJUnitConfig(ReferenceDataRepositoryCacheTest.CacheTestConfig.class)
class ReferenceDataRepositoryCacheTest {

    @Configuration
    @EnableCaching
    @Import({AccessTypeSchemaRepository.class, AccessTypeRepository.class})
    static class CacheTestConfig {

        @Bean
        DSLContext dslContext() {
            return mock(DSLContext.class);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @Autowired
    DSLContext dslContext;

    @Autowired
    AccessTypeSchemaRepository accessTypeSchemaRepository;

    @Autowired
    AccessTypeRepository accessTypeRepository;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            final var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        reset(dslContext);
    }

    @Nested
    @DisplayName("AccessTypeSchemaRepository.findByUri")
    class FindByUriCacheTests {

        @Test
        @DisplayName("Second call with same URI returns cached result without hitting the DB")
        void secondCallWithSameUriIsCached() {
            final var uri = "https://vocabularies.coar-repositories.org/access_rights/";
            final var record = new AccessTypeSchemaRecord();

            @SuppressWarnings("unchecked")
            final var whereStep = (SelectWhereStep<AccessTypeSchemaRecord>) mock(SelectWhereStep.class);
            @SuppressWarnings("unchecked")
            final var conditionStep = (SelectConditionStep<AccessTypeSchemaRecord>) mock(SelectConditionStep.class);
            when(dslContext.selectFrom(any(Table.class))).thenReturn(whereStep);
            when(whereStep.where(any(Condition.class))).thenReturn(conditionStep);
            when(conditionStep.fetchOptional()).thenReturn(Optional.of(record));

            accessTypeSchemaRepository.findByUri(uri);
            accessTypeSchemaRepository.findByUri(uri);

            // DSLContext should only have been called once — second call served from cache
            verify(dslContext, times(1)).selectFrom(any(Table.class));
        }

        @Test
        @DisplayName("Calls with different URIs each hit the DB")
        void differentUrisEachHitDb() {
            final var uri1 = "https://vocabularies.coar-repositories.org/access_rights/";
            final var uri2 = "https://creativecommons.org/licenses/";

            @SuppressWarnings("unchecked")
            final var whereStep = (SelectWhereStep<AccessTypeSchemaRecord>) mock(SelectWhereStep.class);
            @SuppressWarnings("unchecked")
            final var conditionStep = (SelectConditionStep<AccessTypeSchemaRecord>) mock(SelectConditionStep.class);
            when(dslContext.selectFrom(any(Table.class))).thenReturn(whereStep);
            when(whereStep.where(any(Condition.class))).thenReturn(conditionStep);
            when(conditionStep.fetchOptional()).thenReturn(Optional.empty());

            accessTypeSchemaRepository.findByUri(uri1);
            accessTypeSchemaRepository.findByUri(uri2);

            verify(dslContext, times(2)).selectFrom(any(Table.class));
        }
    }

    @Nested
    @DisplayName("AccessTypeRepository.findByUriAndSchemaId")
    class FindByUriAndSchemaIdCacheTests {

        @Test
        @DisplayName("Second call with same URI and schemaId returns cached result without hitting the DB")
        void secondCallWithSameArgsIsCached() {
            final var uri = "https://vocabularies.coar-repositories.org/access_rights/c_abf2/";
            final var schemaId = 1;
            final var record = new AccessTypeRecord();

            @SuppressWarnings("unchecked")
            final var whereStep = (SelectWhereStep<AccessTypeRecord>) mock(SelectWhereStep.class);
            @SuppressWarnings("unchecked")
            final var conditionStep = (SelectConditionStep<AccessTypeRecord>) mock(SelectConditionStep.class);
            when(dslContext.selectFrom(any(Table.class))).thenReturn(whereStep);
            when(whereStep.where(any(Condition.class))).thenReturn(conditionStep);
            when(conditionStep.fetchOptional()).thenReturn(Optional.of(record));

            accessTypeRepository.findByUriAndSchemaId(uri, schemaId);
            accessTypeRepository.findByUriAndSchemaId(uri, schemaId);

            verify(dslContext, times(1)).selectFrom(any(Table.class));
        }

        @Test
        @DisplayName("Calls with same URI but different schemaId each hit the DB")
        void sameUriDifferentSchemaIdEachHitDb() {
            final var uri = "https://vocabularies.coar-repositories.org/access_rights/c_abf2/";

            @SuppressWarnings("unchecked")
            final var whereStep = (SelectWhereStep<AccessTypeRecord>) mock(SelectWhereStep.class);
            @SuppressWarnings("unchecked")
            final var conditionStep = (SelectConditionStep<AccessTypeRecord>) mock(SelectConditionStep.class);
            when(dslContext.selectFrom(any(Table.class))).thenReturn(whereStep);
            when(whereStep.where(any(Condition.class))).thenReturn(conditionStep);
            when(conditionStep.fetchOptional()).thenReturn(Optional.empty());

            accessTypeRepository.findByUriAndSchemaId(uri, 1);
            accessTypeRepository.findByUriAndSchemaId(uri, 2);

            verify(dslContext, times(2)).selectFrom(any(Table.class));
        }
    }
}
