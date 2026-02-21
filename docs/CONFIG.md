# Configuration Guide

Nexora is configured through `NexoraConfig` with a builder API.

## MySQL / MariaDB
```java
NexoraConfig config = NexoraConfig.builder()
    .jdbcUrl("jdbc:mysql://localhost:3306/nexora")
    .username("root")
    .password("password")
    .cacheMode(CacheMode.MEMORY_REDIS)
    .redisHost("127.0.0.1")
    .redisPort(6379)
    .defaultCacheTtl(Duration.ofMinutes(5))
    .cacheMaxSize(10_000)
    .autoFlushDebounceMs(50)
    .migrationMode(MigrationMode.APPLY_SAFE)
    .databaseType(DatabaseType.MYSQL)
    .build();
```

## SQLite
```java
NexoraConfig config = NexoraConfig.builder()
    .jdbcUrl("jdbc:sqlite:plugins/Nexora/data.db")
    .cacheMode(CacheMode.MEMORY_ONLY)
    .migrationMode(MigrationMode.APPLY_SAFE)
    .databaseType(DatabaseType.SQLITE)
    .build();
```

## Cache modes
- `MEMORY_ONLY`: Local Caffeine cache only.
- `MEMORY_REDIS`: Local cache + Redis L2 and pub/sub invalidation.
