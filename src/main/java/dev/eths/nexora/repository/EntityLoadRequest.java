package dev.eths.nexora.repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class EntityLoadRequest<T, K> {
    enum CachePolicy {
        DEFAULT,
        NO_CACHE,
        BYPASS
    }

    private final EntityRepository<T, K> repository;
    private final K id;
    private final List<RelationPath<T, ?>> includes = new ArrayList<>();
    private CachePolicy cachePolicy = CachePolicy.DEFAULT;
    private Duration cacheTtl;

    EntityLoadRequest(EntityRepository<T, K> repository, K id) {
        this.repository = repository;
        this.id = id;
    }

    public EntityLoadRequest<T, K> with(RelationPath<T, ?> relation) {
        includes.add(relation);
        return this;
    }

    public EntityLoadRequest<T, K> cache(Duration ttl) {
        this.cacheTtl = ttl;
        this.cachePolicy = CachePolicy.DEFAULT;
        return this;
    }

    public EntityLoadRequest<T, K> cache() {
        this.cachePolicy = CachePolicy.DEFAULT;
        return this;
    }

    public EntityLoadRequest<T, K> noCache() {
        this.cachePolicy = CachePolicy.NO_CACHE;
        return this;
    }

    public EntityLoadRequest<T, K> bypassCache() {
        this.cachePolicy = CachePolicy.BYPASS;
        return this;
    }

    public NexoraFuture<Optional<T>> async() {
        CompletableFuture<Optional<T>> future = repository.loadAsync(id, includes, cachePolicy, cacheTtl);
        return new NexoraFuture<>(future);
    }

    public Optional<T> sync() {
        return repository.loadNow(id, includes, cachePolicy, cacheTtl);
    }
}
