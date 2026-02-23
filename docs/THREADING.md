# Threading

Nexora runs all DB and Redis operations off the Bukkit main thread using its internal executor. When you need to interact with the Bukkit API, use the sync helpers from `NexoraFuture`.

## Bukkit Main-Thread Callback
```java
context.getRepository(Profile.class)
    .get(playerUuid)
    .async()
    .thenAcceptSync(plugin, profileOptional -> profileOptional.ifPresent(profile -> {
        // Safe to call Bukkit APIs here
    }));
```

## Notes
- Do not call Bukkit APIs from async continuations unless you re-schedule them.
- Write-through flushes are debounced based on `autoFlushDebounceMs`.
