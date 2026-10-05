package au.org.raid.api.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RelatedObjectSuccessCacheTest {
    private static final RelatedObjectKey KEY = new RelatedObjectKey("https://doi.org/", "https://doi.org/10.1/a");

    @Test
    @DisplayName("A recorded success is found")
    void recordedSuccessIsFound() {
        final var cache = new RelatedObjectSuccessCache(Duration.ofMinutes(1), 10);

        assertThat(cache.contains(KEY), is(false));
        cache.recordSuccess(KEY);

        assertThat(cache.contains(KEY), is(true));
        assertThat(cache.contains(new RelatedObjectKey("https://hdl.handle.net/", KEY.id())), is(false));
    }

    @Test
    @DisplayName("A zero or negative expiry or a non-positive size disables the cache")
    void disabledConfigurations() {
        for (final var cache : new RelatedObjectSuccessCache[]{
                new RelatedObjectSuccessCache(Duration.ZERO, 10),
                new RelatedObjectSuccessCache(Duration.ofMinutes(-1), 10),
                new RelatedObjectSuccessCache(Duration.ofMinutes(1), 0),
                new RelatedObjectSuccessCache(null, 10)}) {
            cache.recordSuccess(KEY);

            assertThat(cache.isEnabled(), is(false));
            assertThat(cache.contains(KEY), is(false));
        }
    }

    @Test
    @DisplayName("A null key is ignored")
    void nullKeyIgnored() {
        final var cache = new RelatedObjectSuccessCache(Duration.ofMinutes(1), 10);

        cache.recordSuccess(null);

        assertThat(cache.contains(null), is(false));
    }

    @Test
    @DisplayName("An entry past its expiry is no longer found")
    void expiredEntryIsNotFound() {
        final var nanos = new AtomicLong();
        final var cache = new RelatedObjectSuccessCache(Duration.ofMinutes(1), 10, nanos::get);
        cache.recordSuccess(KEY);

        nanos.addAndGet(Duration.ofSeconds(59).toNanos());
        assertThat(cache.contains(KEY), is(true));

        nanos.addAndGet(Duration.ofSeconds(2).toNanos());
        assertThat(cache.contains(KEY), is(false));
    }
}
