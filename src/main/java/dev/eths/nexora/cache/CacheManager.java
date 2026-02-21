package dev.eths.nexora.cache;

import dev.eths.nexora.metadata.EntityMetadata;

import java.util.Objects;

public class CacheManager {
    private final CacheLayer l1;
    private final CacheLayer l2;
    private final long defaultTtlMillis;
    private final RedisPubSubInvalidator invalidator;

    public CacheManager(CacheLayer l1, CacheLayer l2, long defaultTtlMillis, RedisPubSubInvalidator invalidator) {
        this.l1 = l1;
        this.l2 = l2;
        this.defaultTtlMillis = defaultTtlMillis;
        this.invalidator = invalidator;
    }

    public <T> T get(EntityMetadata metadata, String key, Class<T> type) {
        if (l1 != null) {
            T value = l1.get(key, metadata, type);
            if (value != null) {
                return value;
            }
        }
        if (l2 != null) {
            T value = l2.get(key, metadata, type);
            if (value != null && l1 != null) {
                l1.put(key, metadata, value, defaultTtlMillis);
            }
            return value;
        }
        return null;
    }

    public void put(EntityMetadata metadata, String key, Object value, long ttlMillis) {
        long effectiveTtl = ttlMillis > 0 ? ttlMillis : defaultTtlMillis;
        if (l1 != null) {
            l1.put(key, metadata, value, effectiveTtl);
        }
        if (l2 != null) {
            l2.put(key, metadata, value, effectiveTtl);
        }
    }

    public void invalidate(EntityMetadata metadata, String key, String table, String id) {
        if (l1 != null) {
            l1.invalidate(key);
        }
        if (l2 != null) {
            l2.invalidate(key);
        }
        if (invalidator != null) {
            // Broadcast invalidation so other servers evict their L1 caches.
            invalidator.publish(table, id);
        }
    }

    public void invalidateLocal(String key) {
        if (l1 != null) {
            l1.invalidate(key);
        }
    }

    public void close() {
        if (invalidator != null) {
            invalidator.stop();
        }
        if (l1 != null) {
            l1.close();
        }
        if (l2 != null) {
            l2.close();
        }
    }
}
