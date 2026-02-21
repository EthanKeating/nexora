package dev.eths.nexora.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public final class BukkitSchedulerBridge {
    private BukkitSchedulerBridge() {
    }

    public static <T, R> CompletableFuture<R> thenApplySync(Plugin plugin, CompletableFuture<T> future, Function<T, R> fn) {
        CompletableFuture<R> result = new CompletableFuture<>();
        future.whenComplete((value, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    result.complete(fn.apply(value));
                } catch (Throwable t) {
                    result.completeExceptionally(t);
                }
            });
        });
        return result;
    }

    public static <T> CompletableFuture<Void> thenAcceptSync(Plugin plugin, CompletableFuture<T> future, Consumer<T> consumer) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        future.whenComplete((value, throwable) -> {
            if (throwable != null) {
                result.completeExceptionally(throwable);
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    consumer.accept(value);
                    result.complete(null);
                } catch (Throwable t) {
                    result.completeExceptionally(t);
                }
            });
        });
        return result;
    }
}
