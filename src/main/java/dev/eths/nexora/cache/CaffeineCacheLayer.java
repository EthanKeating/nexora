package dev.eths.nexora.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import dev.eths.nexora.metadata.EntityMetadata;

import java.util.concurrent.TimeUnit;

public class CaffeineCacheLayer implements CacheLayer {
    private final Cache<String, CacheEntry> cache;
    private final long defaultTtlMillis;

    public CaffeineCacheLayer(long maxSize, long defaultTtlMillis) {
        this.defaultTtlMillis = defaultTtlMillis;
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfter(new Expiry<String, CacheEntry>() {
                    @Override
                    public long expireAfterCreate(String key, CacheEntry value, long currentTime) {
                        return TimeUnit.MILLISECONDS.toNanos(value.ttlMillis);
                    }

                    @Override
                    public long expireAfterUpdate(String key, CacheEntry value, long currentTime, long currentDuration) {
                        return TimeUnit.MILLISECONDS.toNanos(value.ttlMillis);
                    }

                    @Override
                    public long expireAfterRead(String key, CacheEntry value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key, EntityMetadata metadata, Class<T> type) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry == null) {
            return null;
        }
        return (T) entry.value;
    }

    @Override
    public void put(String key, EntityMetadata metadata, Object value, long ttlMillis) {
        long effectiveTtl = ttlMillis > 0 ? ttlMillis : defaultTtlMillis;
        cache.put(key, new CacheEntry(value, effectiveTtl));
    }

    @Override
    public void invalidate(String key) {
        cache.invalidate(key);
    }

    @Override
    public void close() {
        cache.invalidateAll();
    }

    private static class CacheEntry {
        private final Object value;
        private final long ttlMillis;

        private CacheEntry(Object value, long ttlMillis) {
            this.value = value;
            this.ttlMillis = ttlMillis;
        }
    }
}
