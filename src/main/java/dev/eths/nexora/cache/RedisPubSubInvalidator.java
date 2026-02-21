package dev.eths.nexora.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public class RedisPubSubInvalidator {
    private static final Logger logger = Logger.getLogger(RedisPubSubInvalidator.class.getName());
    private final JedisPool pool;
    private final String channel;
    private final ObjectMapper mapper;
    private final BiConsumer<String, String> onInvalidate;
    private ExecutorService executor;
    private JedisPubSub pubSub;

    public RedisPubSubInvalidator(JedisPool pool, String channel, BiConsumer<String, String> onInvalidate) {
        this.pool = pool;
        this.channel = channel;
        this.onInvalidate = onInvalidate;
        this.mapper = new ObjectMapper();
    }

    public void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "nexora-redis-pubsub");
            t.setDaemon(true);
            return t;
        });
        pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> payload = mapper.readValue(message, Map.class);
                    Object table = payload.get("table");
                    Object id = payload.get("id");
                    if (table != null && id != null) {
                        onInvalidate.accept(table.toString(), id.toString());
                    }
                } catch (Exception e) {
                    logger.warning("Failed to parse Redis invalidation message: " + e.getMessage());
                }
            }
        };

        executor.submit(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(pubSub, channel);
            }
        });
    }

    public void publish(String table, String id) {
        try (Jedis jedis = pool.getResource()) {
            String payload = mapper.writeValueAsString(Map.of("table", table, "id", id));
            jedis.publish(channel, payload);
        } catch (Exception e) {
            logger.warning("Failed to publish Redis invalidation: " + e.getMessage());
        }
    }

    public void stop() {
        if (pubSub != null) {
            try {
                pubSub.unsubscribe();
            } catch (Exception e) {
                logger.warning("Failed to unsubscribe Redis pubsub: " + e.getMessage());
            }
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}
