package dev.eths.nexora.cache;

import dev.eths.nexora.metadata.EntityMetadata;

public interface CacheLayer {
    <T> T get(String key, EntityMetadata metadata, Class<T> type);

    void put(String key, EntityMetadata metadata, Object value, long ttlMillis);

    void invalidate(String key);

    void close();
}
