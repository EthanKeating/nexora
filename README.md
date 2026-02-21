# Nexora ORM (Bukkit/Spigot)

Lightweight, async-first SQL ORM for Bukkit/Spigot plugins with explicit includes, safe schema sync, and write-through managed entities.

## Features
- Annotation-based entity mapping
- Explicit relation includes (no lazy proxies)
- Async DB access + Bukkit main-thread callback helpers
- L1 Caffeine cache + optional Redis L2 with invalidation
- Safe schema synchronization on startup
- Managed entities that auto-persist on mutation
- HikariCP connection pooling

## Gradle setup
This repository is already a complete Gradle project. To use as a dependency in another plugin, publish it to your repository or include as a submodule.

## Configuration
```java
NexoraConfig config = NexoraConfig.builder()
    .jdbcUrl("jdbc:mysql://localhost:3306/nexora")
    .username("root")
    .password("password")
    .driverClassName("com.mysql.cj.jdbc.Driver")
    .defaultCacheTtl(Duration.ofMinutes(5))
    .cacheMaxSize(10_000)
    .cacheMode(CacheMode.MEMORY_REDIS)
    .redisHost("127.0.0.1")
    .redisPort(6379)
    .autoFlushDebounceMs(50)
    .migrationMode(MigrationMode.APPLY_SAFE)
    .databaseType(DatabaseType.AUTO)
    .build();
```

SQLite example:
```java
NexoraConfig config = NexoraConfig.builder()
    .jdbcUrl("jdbc:sqlite:plugins/Nexora/data.db")
    .driverClassName("org.sqlite.JDBC")
    .cacheMode(CacheMode.MEMORY_ONLY)
    .migrationMode(MigrationMode.APPLY_SAFE)
    .databaseType(DatabaseType.SQLITE)
    .build();
```

## Startup
```java
NexoraContext context = NexoraContext.builder(plugin, config)
    .register(Profile.class)
    .register(Clan.class)
    .build();
```

## Async load + explicit include
```java
context.getRepository(Profile.class)
    .get(playerUuid)
    .with(ProfileRelations.CLAN)
    .cache(Duration.ofMinutes(10))
    .async()
    .thenAcceptSync(plugin, profileOptional -> profileOptional.ifPresent(profile -> {
        Bukkit.broadcastMessage("Loaded " + profile.getProfileId());
        profile.setCoins(profile.getCoins() + 5); // auto write-through
    }));
```

## Managed entity write-through
```java
Profile profile = new Profile(playerUuid);
context.getRepository(Profile.class).attachNew(profile);
profile.setCoins(100); // debounced async write-through
```

## Manual save, detach, cache control
```java
Profile profile = context.getRepository(Profile.class).getNow(playerUuid).orElseThrow();
profile.setCoins(profile.getCoins() + 100);
profile.saveNowAsync();
profile.invalidateCache();
profile.detach();
```

## Redis invalidation
When Redis is enabled, all writes and deletes publish invalidation messages on the `nexora:invalidate` channel, and each instance evicts from its L1 cache.

## Safe schema sync
On startup, Nexora:
1. Inspects your entity metadata
2. Compares it to the live schema
3. Applies safe changes only (create tables, add nullable/defaulted columns, create indexes)
4. Blocks unsafe changes (drops, type changes, NOT NULL without default)

Manual rename hints:
```java
@RenamedFrom("old_column_name")
@Column(name = "new_column_name")
private String name;

@TableRenamedFrom("old_table_name")
@Entity(table = "new_table_name")
public class Profile { ... }
```

## Notes for plugin developers
- DB and Redis I/O always run off the main thread.
- Use the sync bridge (`thenAcceptSync`) for Bukkit API calls.
- Mutators must call `markDirty("column")` (handled in included entity examples).

## Example entities
See:
- `src/main/java/dev/eths/nexora/example/entities/Profile.java`
- `src/main/java/dev/eths/nexora/example/entities/Clan.java`

## Shutdown
```java
@Override
public void onDisable() {
    if (context != null) {
        context.close();
    }
}
```
