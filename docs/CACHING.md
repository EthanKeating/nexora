# Caching

Nexora provides two cache layers:
- L1: Caffeine (local JVM)
- L2: Redis (optional)

## Cache Modes
- `MEMORY_ONLY`: L1 only
- `MEMORY_REDIS`: L1 + L2 + pub/sub invalidation

## Default TTL
The default TTL comes from `NexoraConfig.defaultCacheTtl`. You can override it per load request.

```java
context.getRepository(Profile.class)
    .get(playerUuid)
    .cache(Duration.ofMinutes(10))
    .async();
```

## Cache Policies
The load request supports three policies:
- `cache()` or `cache(Duration)` uses cache and writes through.
- `noCache()` bypasses cache reads and writes.
- `bypassCache()` bypasses reads but still writes the result to cache.

## Invalidation
Writes and deletes invalidate local caches immediately. When Redis is enabled, an invalidation message is published on `nexora:invalidate` so other servers evict their L1 entries.
