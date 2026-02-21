package dev.eths.nexora.cache;

import dev.eths.nexora.metadata.EntityMetadata;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisCacheLayer implements CacheLayer {
    private final JedisPool pool;
    private final EntitySerializer serializer;

    public RedisCacheLayer(JedisPool pool, EntitySerializer serializer) {
        this.pool = pool;
        this.serializer = serializer;
    }

    @Override
    public <T> T get(String key, EntityMetadata metadata, Class<T> type) {
        try (Jedis jedis = pool.getResource()) {
            String payload = jedis.get(key);
            if (payload == null) {
                return null;
            }
            return serializer.deserialize(metadata, payload, type);
        }
    }

    @Override
    public void put(String key, EntityMetadata metadata, Object value, long ttlMillis) {
        String payload = serializer.serialize(metadata, value);
        try (Jedis jedis = pool.getResource()) {
            if (ttlMillis > 0) {
                int ttlSeconds = Math.max(1, (int) (ttlMillis / 1000));
                jedis.setex(key, ttlSeconds, payload);
            } else {
                jedis.set(key, payload);
            }
        }
    }

    @Override
    public void invalidate(String key) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(key);
        }
    }

    @Override
    public void close() {
        pool.close();
    }
}
