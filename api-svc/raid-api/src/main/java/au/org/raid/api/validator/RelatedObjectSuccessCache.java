package au.org.raid.api.validator;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Remembers related objects whose resolver check succeeded (RAID-935), so a save retried after a
 * 503 does not re-check the links the first attempt already confirmed, and so each retry makes
 * progress. Only successes go in: a "uri not found" or an unavailable resolver is never
 * recorded, so a link that is archived later is checked again.
 * <p>
 * Per instance and in memory: another API instance, or a restart, starts empty. That is
 * acceptable because a retry reaching a cold instance still confirms the links it checks.
 * Bounded by size and by expiry. A non-positive expiry or maximum size disables the cache.
 * <p>
 * Applies to related objects only (DOI, Handle, RRID, Web Archive), not to contributors or
 * organisations.
 */
public class RelatedObjectSuccessCache {
    private static final Logger log = LoggerFactory.getLogger(RelatedObjectSuccessCache.class);

    private final Cache<RelatedObjectKey, Boolean> cache;

    public RelatedObjectSuccessCache(final Duration expireAfterWrite, final long maximumSize) {
        this(expireAfterWrite, maximumSize, Ticker.systemTicker());
    }

    /** For tests: lets a fake {@link Ticker} drive expiry. */
    RelatedObjectSuccessCache(final Duration expireAfterWrite, final long maximumSize, final Ticker ticker) {
        if (expireAfterWrite == null || expireAfterWrite.isZero() || expireAfterWrite.isNegative()
                || maximumSize <= 0) {
            this.cache = null;
            log.info("Related object success cache is disabled (expire-after-write={}, maximum-size={})",
                    expireAfterWrite, maximumSize);
        } else {
            this.cache = Caffeine.newBuilder()
                    .expireAfterWrite(expireAfterWrite)
                    .maximumSize(maximumSize)
                    .ticker(ticker)
                    .build();
            log.info("Related object success cache is enabled (expire-after-write={}, maximum-size={})",
                    expireAfterWrite, maximumSize);
        }
    }

    public boolean isEnabled() {
        return cache != null;
    }

    public boolean contains(final RelatedObjectKey key) {
        return cache != null && key != null && cache.getIfPresent(key) != null;
    }

    public void recordSuccess(final RelatedObjectKey key) {
        if (cache != null && key != null) {
            cache.put(key, Boolean.TRUE);
        }
    }
}
