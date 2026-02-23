# Repositories

Each registered entity gets an `EntityRepository<T, K>` with sync and async operations.

## Basic Loading
```java
Optional<Profile> profile = context.getRepository(Profile.class).getNow(playerUuid);
```

## Async Loading
```java
context.getRepository(Profile.class)
    .get(playerUuid)
    .async()
    .thenAcceptSync(plugin, result -> result.ifPresent(profile -> {
        // Bukkit API usage is safe here
    }));
```

## Required Loads
```java
Profile profile = context.getRepository(Profile.class).getRequiredNow(playerUuid);
```

## Insert and Upsert
```java
Profile profile = new Profile(playerUuid);
context.getRepository(Profile.class).insertAsync(profile);

profile.setCoins(100);
context.getRepository(Profile.class).upsertAsync(profile);
```

## Delete
```java
context.getRepository(Profile.class).deleteAsync(playerUuid);
```

## Cache Policies
```java
context.getRepository(Profile.class)
    .get(playerUuid)
    .cache(Duration.ofMinutes(10))
    .async();

context.getRepository(Profile.class)
    .get(playerUuid)
    .noCache()
    .async();

context.getRepository(Profile.class)
    .get(playerUuid)
    .bypassCache()
    .async();
```

## Managed Attach
`attachNew(entity)` registers a new entity for write-through and schedules a flush.

```java
Profile profile = new Profile(playerUuid);
context.getRepository(Profile.class).attachNew(profile);
profile.setCoins(100); // debounced write-through
```
