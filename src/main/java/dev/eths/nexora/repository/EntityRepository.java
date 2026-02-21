package dev.eths.nexora.repository;

import dev.eths.nexora.NexoraContext;
import dev.eths.nexora.cache.CacheKey;
import dev.eths.nexora.entity.ManagedEntity;
import dev.eths.nexora.metadata.EntityMetadata;
import dev.eths.nexora.metadata.FieldMetadata;
import dev.eths.nexora.metadata.RelationMetadata;
import dev.eths.nexora.sql.SqlDialect;
import dev.eths.nexora.util.TypeConverter;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class EntityRepository<T, K> {
    private static final Logger logger = Logger.getLogger(EntityRepository.class.getName());
    private final NexoraContext context;
    private final EntityMetadata metadata;
    private final SqlDialect dialect;

    public EntityRepository(NexoraContext context, EntityMetadata metadata, SqlDialect dialect) {
        this.context = context;
        this.metadata = metadata;
        this.dialect = dialect;
    }

    public Optional<T> getNow(K id) {
        return loadNow(id, List.of(), EntityLoadRequest.CachePolicy.DEFAULT, null);
    }

    public T getRequiredNow(K id) {
        return getNow(id).orElseThrow();
    }

    public EntityLoadRequest<T, K> get(K id) {
        return new EntityLoadRequest<>(this, id);
    }

    public CompletableFuture<Optional<T>> getAsync(K id) {
        return loadAsync(id, List.of(), EntityLoadRequest.CachePolicy.DEFAULT, null);
    }

    public CompletableFuture<T> getRequiredAsync(K id) {
        return getAsync(id).thenApply(optional -> optional.orElseThrow());
    }

    public void deleteNow(K id) {
        deleteByIdNow(id);
    }

    public CompletableFuture<Void> deleteAsync(K id) {
        return CompletableFuture.runAsync(() -> deleteByIdNow(id), context.getDbExecutor());
    }

    public T attachNew(T entity) {
        context.attach(entity, metadata, true);
        context.scheduleFlush((ManagedEntity<?>) entity);
        return entity;
    }

    public CompletableFuture<T> insertAsync(T entity) {
        context.attach(entity, metadata, true);
        return context.upsertAsync((ManagedEntity<?>) entity).thenApply(v -> entity);
    }

    public CompletableFuture<T> upsertAsync(T entity) {
        context.attach(entity, metadata, false);
        return context.upsertAsync((ManagedEntity<?>) entity).thenApply(v -> entity);
    }

    Optional<T> loadNow(K id, List<RelationPath<T, ?>> includes, EntityLoadRequest.CachePolicy cachePolicy, java.time.Duration ttl) {
        if (cachePolicy != EntityLoadRequest.CachePolicy.BYPASS && cachePolicy != EntityLoadRequest.CachePolicy.NO_CACHE) {
            @SuppressWarnings("unchecked")
            Class<T> type = (Class<T>) metadata.getEntityClass();
            T cached = context.getCacheManager().get(metadata, cacheKey(id), type);
            if (cached != null) {
                attachIfNeeded(cached, false);
                loadIncludes(cached, includes);
                return Optional.of(cached);
            }
        }

        Optional<T> loaded = queryByPrimaryKey(id);
        loaded.ifPresent(entity -> {
            attachIfNeeded(entity, false);
            if (cachePolicy != EntityLoadRequest.CachePolicy.NO_CACHE) {
                long ttlMillis = ttl == null ? 0 : ttl.toMillis();
                context.getCacheManager().put(metadata, cacheKey(id), entity, ttlMillis);
            }
            loadIncludes(entity, includes);
        });
        return loaded;
    }

    CompletableFuture<Optional<T>> loadAsync(K id, List<RelationPath<T, ?>> includes, EntityLoadRequest.CachePolicy cachePolicy, java.time.Duration ttl) {
        return CompletableFuture.supplyAsync(() -> loadNow(id, includes, cachePolicy, ttl), context.getDbExecutor());
    }

    private void loadIncludes(T entity, List<RelationPath<T, ?>> includes) {
        if (includes.isEmpty()) {
            return;
        }
        for (RelationPath<T, ?> path : includes) {
            RelationMetadata relation = path.getMetadata();
            Object localValue = readColumnValue(entity, relation.getLocalColumn());
            if (localValue == null) {
                continue;
            }
            EntityRepository<?, ?> targetRepo = context.getRepository(relation.getTarget());
            Optional<?> related = targetRepo.queryByColumnNow(relation.getTargetColumn(), localValue);
            related.ifPresent(target -> {
                try {
                    Field field = relation.getField();
                    field.setAccessible(true);
                    field.set(entity, target);
                } catch (IllegalAccessException e) {
                    logger.warning("Failed to set relation " + relation.getField().getName() + ": " + e.getMessage());
                }
            });
        }
    }

    private Object readColumnValue(T entity, String columnName) {
        FieldMetadata field = metadata.getFieldByColumn(columnName);
        if (field == null) {
            return null;
        }
        try {
            Field reflection = field.getField();
            reflection.setAccessible(true);
            return reflection.get(entity);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    Optional<T> queryByColumnNow(String column, Object value) {
        String sql = "SELECT * FROM " + dialect.quote(metadata.getTableName()) + " WHERE " + dialect.quote(column) + " = ? LIMIT 1";
        try (Connection connection = context.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, TypeConverter.toDatabaseValue(value));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    T entity = mapRow(rs);
                    attachIfNeeded(entity, false);
                    return Optional.of(entity);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed query for " + metadata.getTableName() + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<T> queryByPrimaryKey(K id) {
        String sql = "SELECT * FROM " + dialect.quote(metadata.getTableName()) + " WHERE " +
                dialect.quote(metadata.getPrimaryKey().getColumnName()) + " = ? LIMIT 1";
        try (Connection connection = context.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, TypeConverter.toDatabaseValue(id));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    T entity = mapRow(rs);
                    return Optional.of(entity);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to load " + metadata.getEntityClass().getSimpleName() + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    private void deleteByIdNow(K id) {
        String sql = "DELETE FROM " + dialect.quote(metadata.getTableName()) + " WHERE " +
                dialect.quote(metadata.getPrimaryKey().getColumnName()) + " = ?";
        try (Connection connection = context.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, TypeConverter.toDatabaseValue(id));
            statement.executeUpdate();
            context.getCacheManager().invalidate(metadata, cacheKey(id), metadata.getTableName(), id.toString());
        } catch (Exception e) {
            logger.warning("Failed to delete " + metadata.getEntityClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    String cacheKey(K id) {
        return CacheKey.entityKey(metadata.getTableName(), id.toString());
    }

    @SuppressWarnings("unchecked")
    private T mapRow(ResultSet rs) throws Exception {
        T entity = (T) metadata.getEntityClass().getDeclaredConstructor().newInstance();
        for (FieldMetadata field : metadata.getFields()) {
            Object value = TypeConverter.fromResultSet(rs, field.getColumnName(), field.getField().getType());
            Field reflection = field.getField();
            reflection.setAccessible(true);
            reflection.set(entity, value);
        }
        return entity;
    }

    void attachIfNeeded(T entity, boolean isNew) {
        if (entity instanceof ManagedEntity<?>) {
            ManagedEntity<?> managed = (ManagedEntity<?>) entity;
            if (!managed.isManaged()) {
                context.attach(entity, metadata, isNew);
            }
        }
    }
}
