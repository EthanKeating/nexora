package dev.eths.nexora.entity;

import dev.eths.nexora.NexoraContext;
import dev.eths.nexora.cache.CacheKey;
import dev.eths.nexora.metadata.EntityMetadata;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

public abstract class ManagedEntity<K> {
    private transient NexoraContext context;
    private transient EntityMetadata metadata;
    private transient boolean managed;
    private transient boolean newEntity;
    private transient boolean dirtyUnsynced;
    private transient final Object dirtyLock = new Object();
    private transient Set<String> dirtyColumns = new HashSet<>();
    private transient ScheduledFuture<?> pendingFlush;

    protected void markDirty(String columnName) {
        if (!managed || context == null || metadata == null) {
            return;
        }
        synchronized (dirtyLock) {
            dirtyColumns.add(columnName);
        }
        String id = context.extractIdAsString(metadata, this);
        if (id != null) {
            context.getCacheManager().put(metadata, CacheKey.entityKey(metadata.getTableName(), id), this, 0);
        }
        // Debounced write-through keeps rapid setter bursts from spamming the DB.
        scheduleFlush();
    }

    private void scheduleFlush() {
        if (!managed || context == null) {
            return;
        }
        if (pendingFlush != null && !pendingFlush.isDone()) {
            pendingFlush.cancel(false);
        }
        pendingFlush = context.scheduleFlush(this);
    }

    public CompletableFuture<Void> saveNowAsync() {
        if (!managed || context == null) {
            return CompletableFuture.completedFuture(null);
        }
        return context.flushEntityNow(this);
    }

    public void saveNowBlocking() {
        if (!managed || context == null) {
            return;
        }
        context.flushEntityNowBlocking(this);
    }

    public void invalidateCache() {
        if (!managed || context == null || metadata == null) {
            return;
        }
        String id = context.extractIdAsString(metadata, this);
        if (id != null) {
            context.getCacheManager().invalidate(metadata, CacheKey.entityKey(metadata.getTableName(), id), metadata.getTableName(), id);
        }
    }

    public void detach() {
        managed = false;
        context = null;
        metadata = null;
        dirtyColumns = new HashSet<>();
    }

    public boolean isManaged() {
        return managed;
    }

    public boolean isDirtyUnsynced() {
        return dirtyUnsynced;
    }

    public void markDirtyUnsynced() {
        dirtyUnsynced = true;
    }

    public void clearDirtyUnsynced() {
        dirtyUnsynced = false;
    }

    public EntityMetadata getMetadata() {
        return metadata;
    }

    public Set<String> drainDirtyColumns() {
        synchronized (dirtyLock) {
            Set<String> copy = new HashSet<>(dirtyColumns);
            dirtyColumns.clear();
            return copy;
        }
    }

    public Set<String> snapshotDirtyColumns() {
        synchronized (dirtyLock) {
            return new HashSet<>(dirtyColumns);
        }
    }

    public void clearDirtyColumns() {
        synchronized (dirtyLock) {
            dirtyColumns.clear();
        }
    }

    public void addDirtyColumns(Set<String> columns) {
        synchronized (dirtyLock) {
            dirtyColumns.addAll(columns);
        }
    }

    public boolean hasDirtyColumns() {
        synchronized (dirtyLock) {
            return !dirtyColumns.isEmpty();
        }
    }

    public boolean isNewEntity() {
        return newEntity;
    }

    public void markPersisted() {
        newEntity = false;
    }

    public void attach(NexoraContext context, EntityMetadata metadata, boolean newEntity) {
        this.context = context;
        this.metadata = metadata;
        this.managed = true;
        this.newEntity = newEntity;
    }
}
