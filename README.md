# Nexora ORM (Bukkit/Spigot)

Nexora is a lightweight, async-first SQL ORM for Bukkit/Spigot plugins with explicit relation includes, safe schema synchronization, and write-through managed entities.

## Features
- Annotation-based entity mapping
- Explicit relation includes, no lazy proxies
- Async DB access with Bukkit main-thread callback helpers
- L1 Caffeine cache with optional Redis L2 and pub/sub invalidation
- Safe schema synchronization on startup
- Managed entities with debounced write-through persistence
- HikariCP connection pooling

## Requirements
- Java 17
- Spigot/Paper API for Bukkit integration (compileOnly)
- MySQL/MariaDB or SQLite

## License
GPL-3.0

## Build
```bash
./gradlew build
```

## Quick Start
```java
NexoraConfig config = NexoraConfig.builder()
    .jdbcUrl("jdbc:mysql://localhost:3306/nexora")
    .username("root")
    .password("password")
    .defaultCacheTtl(Duration.ofMinutes(5))
    .cacheMaxSize(10_000)
    .cacheMode(CacheMode.MEMORY_REDIS)
    .redisHost("127.0.0.1")
    .redisPort(6379)
    .autoFlushDebounceMs(50)
    .migrationMode(MigrationMode.APPLY_SAFE)
    .databaseType(DatabaseType.AUTO)
    .build();

NexoraContext context = NexoraContext.builder(this, config)
    .register(Profile.class)
    .register(Clan.class)
    .build();

context.getRepository(Profile.class)
    .get(playerUuid)
    .with(ProfileRelations.CLAN)
    .cache(Duration.ofMinutes(10))
    .async()
    .thenAcceptSync(this, profileOptional -> profileOptional.ifPresent(profile -> {
        Bukkit.broadcastMessage("Loaded " + profile.getProfileId());
        profile.setCoins(profile.getCoins() + 5); // debounced write-through
    }));
```

## Managed Entities
Entities extend `ManagedEntity` and call `markDirty("column")` in setters to enable debounced write-through persistence.

```java
@Entity(table = "profiles")
public class Profile extends ManagedEntity<UUID> {
    @PrimaryKey
    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "coins", nullable = false, defaultValue = "0")
    private int coins;

    public void setCoins(int coins) {
        if (this.coins != coins) {
            this.coins = coins;
            markDirty("coins");
        }
    }
}
```

## Manual Save, Cache, Detach
```java
Profile profile = context.getRepository(Profile.class).getNow(playerUuid).orElseThrow();
profile.setCoins(profile.getCoins() + 100);
profile.saveNowAsync();
profile.invalidateCache();
profile.detach();
```

## Safe Schema Sync
On startup, Nexora inspects entity metadata, compares it to the live schema, and applies safe changes only. Unsafe changes (drops, type changes, NOT NULL without default) are blocked.

Rename hints:
```java
@RenamedFrom("old_column")
@Column(name = "new_column")
private String value;

@TableRenamedFrom("old_table")
@Entity(table = "new_table")
public class Profile { }
```

## Documentation
- `docs/README.md`
- `docs/CONFIG.md`
- `docs/ENTITIES.md`
- `docs/REPOSITORIES.md`
- `docs/RELATIONS.md`
- `docs/CACHING.md`
- `docs/MIGRATIONS.md`
- `docs/THREADING.md`

## Example Plugin
See:
- `src/main/java/dev/eths/nexora/example/ExamplePlugin.java`
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
