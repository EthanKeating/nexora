# Configuration

Nexora is configured through `NexoraConfig` and its builder API.

## Example: MySQL or MariaDB
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

## Example: SQLite
```java
NexoraConfig config = NexoraConfig.builder()
    .jdbcUrl("jdbc:sqlite:plugins/Nexora/data.db")
    .cacheMode(CacheMode.MEMORY_ONLY)
    .migrationMode(MigrationMode.APPLY_SAFE)
    .databaseType(DatabaseType.SQLITE)
    .build();
```

## Options and Defaults
- `jdbcUrl` (required): JDBC connection string.
- `username`: DB username. Default: `null`.
- `password`: DB password. Default: `null`.
- `driverClassName`: JDBC driver class. Default: inferred from `jdbcUrl` and `databaseType`.
- `defaultCacheTtl`: Default cache TTL. Default: `5 minutes`.
- `cacheMaxSize`: L1 cache entry limit. Default: `10_000`.
- `cacheMode`: Cache mode. Default: `MEMORY_ONLY`.
- `redisHost`: Redis host. Default: `127.0.0.1`.
- `redisPort`: Redis port. Default: `6379`.
- `redisPassword`: Redis password. Default: `null`.
- `redisChannel`: Redis invalidation channel. Default: `nexora:invalidate`.
- `autoFlushDebounceMs`: Debounce delay for write-through. Default: `50`.
- `writeRetryCount`: Number of write retries. Default: `2`.
- `executorThreads`: DB executor threads. Default: `4`.
- `migrationMode`: Schema sync mode. Default: `APPLY_SAFE`.
- `databaseType`: SQL dialect. Default: `AUTO`.

## Notes
- If `driverClassName` is not set, Nexora infers it from `jdbcUrl` or `databaseType`.
- With `databaseType = AUTO`, `jdbc:sqlite:` picks SQLite, otherwise MySQL.
