package dev.eths.nexora;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.eths.nexora.cache.CacheManager;
import dev.eths.nexora.cache.CaffeineCacheLayer;
import dev.eths.nexora.cache.EntitySerializer;
import dev.eths.nexora.cache.JacksonEntitySerializer;
import dev.eths.nexora.cache.RedisCacheLayer;
import dev.eths.nexora.cache.RedisPubSubInvalidator;
import dev.eths.nexora.entity.ManagedEntity;
import dev.eths.nexora.metadata.EntityMetadata;
import dev.eths.nexora.metadata.FieldMetadata;
import dev.eths.nexora.metadata.MetadataRegistry;
import dev.eths.nexora.repository.EntityRepository;
import dev.eths.nexora.sql.SqlDialect;
import dev.eths.nexora.sql.MySqlDialect;
import dev.eths.nexora.sql.SqliteDialect;
import dev.eths.nexora.schema.MigrationPlan;
import dev.eths.nexora.schema.SchemaSynchronizer;
import dev.eths.nexora.util.TypeConverter;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import dev.eths.nexora.CacheMode;
import dev.eths.nexora.DatabaseType;

public class NexoraContext {
    private final Plugin plugin;
    private final NexoraConfig config;
    private final MetadataRegistry registry;
    private final SqlDialect dialect;
    private final SchemaSynchronizer synchronizer;
    private final Map<Class<?>, EntityRepository<?, ?>> repositories;
    private final Set<ManagedEntity<?>> managedEntities;
    private final Logger logger;

    private HikariDataSource dataSource;
    private ExecutorService dbExecutor;
    private ScheduledExecutorService flushScheduler;
    private CacheManager cacheManager;
    private JedisPool redisPool;
    private RedisPubSubInvalidator invalidator;
    private volatile boolean closed;

    private NexoraContext(Builder builder) {
        this.plugin = builder.plugin;
        this.config = builder.config;
        this.registry = new MetadataRegistry();
        this.dialect = resolveDialect(builder.config);
        this.synchronizer = new SchemaSynchronizer(dialect);
        this.repositories = new ConcurrentHashMap<>();
        this.managedEntities = ConcurrentHashMap.newKeySet();
        this.logger = plugin != null ? plugin.getLogger() : Logger.getLogger(NexoraContext.class.getName());
        for (Class<?> entity : builder.entities) {
            registry.register(entity);
        }
    }

    public static Builder builder(Plugin plugin, NexoraConfig config) {
        return new Builder(plugin, config);
    }

    public void start() {
        if (dataSource != null) {
            return;
        }

        try {
            this.dataSource = createDataSource(false);
        } catch (AbstractMethodError e) {
            if (!isLikelyLegacySqlite()) {
                throw e;
            }
            logger.warning("SQLite legacy driver compatibility detected. Retrying Hikari with legacy-safe validation settings.");
            this.dataSource = createDataSource(true);
        }

        this.dbExecutor = Executors.newFixedThreadPool(config.getExecutorThreads(), r -> {
            Thread t = new Thread(r, "nexora-db");
            t.setDaemon(true);
            return t;
        });
        this.flushScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "nexora-flush");
            t.setDaemon(true);
            return t;
        });

        initCache();
        runSchemaSync();
        initRepositories();
    }

    private HikariDataSource createDataSource(boolean legacyFallback) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.getJdbcUrl());
        if (config.getUsername() != null) {
            hikari.setUsername(config.getUsername());
        }
        if (config.getPassword() != null) {
            hikari.setPassword(config.getPassword());
        }
        if (config.getDriverClassName() != null) {
            hikari.setDriverClassName(config.getDriverClassName());
        }
        hikari.setMaximumPoolSize(Math.max(2, config.getExecutorThreads()));
        if (legacyFallback) {
            hikari.setConnectionTestQuery("SELECT 1");
            disableJdbc4Validation(hikari);
        }
        return new HikariDataSource(hikari);
    }

    private boolean isLikelyLegacySqlite() {
        String driverClass = config.getDriverClassName();
        String url = config.getJdbcUrl();
        if (driverClass != null && driverClass.toLowerCase().contains("sqlite")) {
            return true;
        }
        return url != null && url.toLowerCase().startsWith("jdbc:sqlite:");
    }

    private void disableJdbc4Validation(HikariConfig hikari) {
        String[] methods = {"setUseJdbc4ConnectionTest", "setUseJdbc4Validation", "setJdbc4ConnectionTest"};
        for (String methodName : methods) {
            try {
                Method method = HikariConfig.class.getMethod(methodName, boolean.class);
                method.invoke(hikari, false);
                return;
            } catch (NoSuchMethodException ignored) {
                // Method not present in this HikariCP version.
            } catch (Exception ignored) {
                // Any reflection issue is non-fatal; connectionTestQuery handles this fallback.
            }
        }
    }

    private void initRepositories() {
        for (EntityMetadata metadata : registry.getAll()) {
            repositories.put(metadata.getEntityClass(), new EntityRepository<>(this, metadata, dialect));
        }
    }

    private void initCache() {
        CaffeineCacheLayer l1 = new CaffeineCacheLayer(config.getCacheMaxSize(), config.getDefaultCacheTtl().toMillis());
        if (config.getCacheMode() == CacheMode.MEMORY_REDIS) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(8);
            this.redisPool = config.getRedisPassword() == null
                    ? new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort())
                    : new JedisPool(poolConfig, config.getRedisHost(), config.getRedisPort(), 2000, config.getRedisPassword());
            EntitySerializer serializer = new JacksonEntitySerializer();
            RedisCacheLayer l2 = new RedisCacheLayer(redisPool, serializer);
            this.invalidator = new RedisPubSubInvalidator(redisPool, config.getRedisChannel(), (table, id) -> {
                String key = dev.eths.nexora.cache.CacheKey.entityKey(table, id);
                cacheManager.invalidateLocal(key);
            });
            this.cacheManager = new CacheManager(l1, l2, config.getDefaultCacheTtl().toMillis(), invalidator);
            invalidator.start();
        } else {
            this.cacheManager = new CacheManager(l1, null, config.getDefaultCacheTtl().toMillis(), null);
        }
    }

    private void runSchemaSync() {
        try (Connection connection = dataSource.getConnection()) {
            MigrationPlan plan = synchronizer.plan(connection, registry.getAll(), connection.getCatalog());
            synchronizer.apply(connection, plan, config.getMigrationMode(), logger);
        } catch (Exception e) {
            throw new IllegalStateException("Schema synchronization failed", e);
        }
    }

    public void close() {
        closed = true;
        flushAll();
        if (cacheManager != null) {
            cacheManager.close();
        }
        if (flushScheduler != null) {
            flushScheduler.shutdownNow();
        }
        if (dbExecutor != null) {
            dbExecutor.shutdown();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private void flushAll() {
        for (ManagedEntity<?> entity : new ArrayList<>(managedEntities)) {
            try {
                entity.saveNowBlocking();
            } catch (Exception e) {
                logger.warning("Failed to flush entity on shutdown: " + e.getMessage());
            }
        }
    }

    public void flushAllNow() {
        flushAll();
    }

    public boolean isClosed() {
        return closed;
    }

    public <T, K> EntityRepository<T, K> getRepository(Class<T> type) {
        @SuppressWarnings("unchecked")
        EntityRepository<T, K> repo = (EntityRepository<T, K>) repositories.get(type);
        if (repo == null) {
            throw new IllegalArgumentException("Repository not registered for " + type.getName());
        }
        return repo;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public ExecutorService getDbExecutor() {
        return dbExecutor;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public ScheduledFuture<?> scheduleFlush(ManagedEntity<?> entity) {
        if (closed) {
            return null;
        }
        return flushScheduler.schedule(() -> flushEntityNow(entity), config.getAutoFlushDebounceMs(), TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<Void> flushEntityNow(ManagedEntity<?> entity) {
        if (closed) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> flushEntityNowBlocking(entity), dbExecutor);
    }

    public void flushEntityNowBlocking(ManagedEntity<?> entity) {
        if (closed || entity == null || entity.getMetadata() == null) {
            return;
        }
        EntityMetadata metadata = entity.getMetadata();
        Set<String> dirtyColumns = entity.snapshotDirtyColumns();
        boolean isNew = entity.isNewEntity();
        if (!isNew && dirtyColumns.isEmpty()) {
            return;
        }

        List<FieldMetadata> updateFields = new ArrayList<>();
        if (isNew || dirtyColumns.isEmpty()) {
            updateFields.addAll(metadata.getFields());
        } else {
            for (String column : dirtyColumns) {
                FieldMetadata field = metadata.getFieldByColumn(column);
                if (field != null && !field.isPrimaryKey()) {
                    updateFields.add(field);
                }
            }
        }

        try (Connection connection = dataSource.getConnection()) {
            if (isNew) {
                String sql = dialect.upsertSql(metadata);
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    bindAllFields(statement, metadata, entity);
                    statement.executeUpdate();
                }
                entity.markPersisted();
            } else {
                if (updateFields.isEmpty()) {
                    return;
                }
                String sql = dialect.updateSql(metadata, updateFields);
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    bindFields(statement, updateFields, entity);
                    bindPrimaryKey(statement, metadata, entity, updateFields.size() + 1);
                    statement.executeUpdate();
                }
            }
            entity.clearDirtyColumns();
            entity.clearDirtyUnsynced();
        } catch (Exception e) {
            entity.markDirtyUnsynced();
            logger.warning("Write-through failed for " + metadata.getEntityClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public CompletableFuture<Void> upsertAsync(ManagedEntity<?> entity) {
        return CompletableFuture.runAsync(() -> flushEntityNowBlocking(entity), dbExecutor);
    }

    public void attach(Object entity, EntityMetadata metadata, boolean isNew) {
        if (!(entity instanceof ManagedEntity<?> managed)) {
            throw new IllegalArgumentException("Entity must extend ManagedEntity: " + entity.getClass().getName());
        }
        managed.attach(this, metadata, isNew);
        managedEntities.add(managed);
    }

    public String extractIdAsString(EntityMetadata metadata, Object entity) {
        try {
            Field field = metadata.getPrimaryKey().getField();
            field.setAccessible(true);
            Object value = field.get(entity);
            return value == null ? null : value.toString();
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private void bindAllFields(PreparedStatement statement, EntityMetadata metadata, Object entity) throws Exception {
        int index = 1;
        for (FieldMetadata field : metadata.getFields()) {
            Field reflection = field.getField();
            reflection.setAccessible(true);
            Object value = reflection.get(entity);
            statement.setObject(index++, TypeConverter.toDatabaseValue(value));
        }
    }

    private void bindFields(PreparedStatement statement, List<FieldMetadata> fields, Object entity) throws Exception {
        int index = 1;
        for (FieldMetadata field : fields) {
            Field reflection = field.getField();
            reflection.setAccessible(true);
            Object value = reflection.get(entity);
            statement.setObject(index++, TypeConverter.toDatabaseValue(value));
        }
    }

    private void bindPrimaryKey(PreparedStatement statement, EntityMetadata metadata, Object entity, int index) throws Exception {
        Field pkField = metadata.getPrimaryKey().getField();
        pkField.setAccessible(true);
        Object value = pkField.get(entity);
        statement.setObject(index, TypeConverter.toDatabaseValue(value));
    }

    public static class Builder {
        private final Plugin plugin;
        private final NexoraConfig config;
        private final List<Class<?>> entities = new ArrayList<>();

        public Builder(Plugin plugin, NexoraConfig config) {
            this.plugin = plugin;
            this.config = config;
        }

        public Builder register(Class<?> entity) {
            entities.add(entity);
            return this;
        }

        public NexoraContext build() {
            NexoraContext context = new NexoraContext(this);
            context.start();
            return context;
        }
    }

    private SqlDialect resolveDialect(NexoraConfig config) {
        DatabaseType type = config.getDatabaseType();
        if (type == DatabaseType.AUTO) {
            String url = config.getJdbcUrl().toLowerCase();
            if (url.startsWith("jdbc:sqlite:")) {
                type = DatabaseType.SQLITE;
            } else {
                type = DatabaseType.MYSQL;
            }
        }
        return type == DatabaseType.SQLITE ? new SqliteDialect() : new MySqlDialect();
    }
}
