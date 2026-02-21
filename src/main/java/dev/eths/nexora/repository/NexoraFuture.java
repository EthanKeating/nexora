package dev.eths.nexora.repository;

import dev.eths.nexora.bukkit.BukkitSchedulerBridge;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class NexoraFuture<T> {
    private final CompletableFuture<T> future;

    public NexoraFuture(CompletableFuture<T> future) {
        this.future = future;
    }

    public CompletableFuture<T> toCompletableFuture() {
        return future;
    }

    public NexoraFuture<T> thenAcceptSync(Plugin plugin, Consumer<T> consumer) {
        BukkitSchedulerBridge.thenAcceptSync(plugin, future, consumer);
        return this;
    }

    public <U> NexoraFuture<U> thenApplySync(Plugin plugin, Function<T, U> fn) {
        CompletableFuture<U> applied = BukkitSchedulerBridge.thenApplySync(plugin, future, fn);
        return new NexoraFuture<>(applied);
    }
}
