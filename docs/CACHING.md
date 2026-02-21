# Caching

Nexora provides a two-layer cache:
- L1: Caffeine (local JVM)
- L2: Redis (optional)

## Modes
- `MEMORY_ONLY`: L1 only
- `MEMORY_REDIS`: L1 + L2 + pub/sub invalidation

## Invalidation
Writes and deletes invalidate local caches immediately. If Redis is enabled,
an invalidation message is published on `nexora:invalidate` so other servers
evict their L1 entries.
