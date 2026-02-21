package dev.eths.nexora;

import java.time.Duration;

public class NexoraConfig {
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final Duration defaultCacheTtl;
    private final long cacheMaxSize;
    private final CacheMode cacheMode;
    private final String redisHost;
    private final int redisPort;
    private final String redisPassword;
    private final String redisChannel;
    private final long autoFlushDebounceMs;
    private final int writeRetryCount;
    private final int executorThreads;
    private final MigrationMode migrationMode;
    private final DatabaseType databaseType;

    private NexoraConfig(Builder builder) {
        this.jdbcUrl = builder.jdbcUrl;
        this.username = builder.username;
        this.password = builder.password;
        this.driverClassName = builder.driverClassName;
        this.defaultCacheTtl = builder.defaultCacheTtl;
        this.cacheMaxSize = builder.cacheMaxSize;
        this.cacheMode = builder.cacheMode;
        this.redisHost = builder.redisHost;
        this.redisPort = builder.redisPort;
        this.redisPassword = builder.redisPassword;
        this.redisChannel = builder.redisChannel;
        this.autoFlushDebounceMs = builder.autoFlushDebounceMs;
        this.writeRetryCount = builder.writeRetryCount;
        this.executorThreads = builder.executorThreads;
        this.migrationMode = builder.migrationMode;
        this.databaseType = builder.databaseType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public Duration getDefaultCacheTtl() {
        return defaultCacheTtl;
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public CacheMode getCacheMode() {
        return cacheMode;
    }

    public String getRedisHost() {
        return redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public String getRedisPassword() {
        return redisPassword;
    }

    public String getRedisChannel() {
        return redisChannel;
    }

    public long getAutoFlushDebounceMs() {
        return autoFlushDebounceMs;
    }

    public int getWriteRetryCount() {
        return writeRetryCount;
    }

    public int getExecutorThreads() {
        return executorThreads;
    }

    public MigrationMode getMigrationMode() {
        return migrationMode;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public static class Builder {
        private String jdbcUrl;
        private String username;
        private String password;
        private String driverClassName;
        private Duration defaultCacheTtl = Duration.ofMinutes(5);
        private long cacheMaxSize = 10_000;
        private CacheMode cacheMode = CacheMode.MEMORY_ONLY;
        private String redisHost = "127.0.0.1";
        private int redisPort = 6379;
        private String redisPassword;
        private String redisChannel = "nexora:invalidate";
        private long autoFlushDebounceMs = 50;
        private int writeRetryCount = 2;
        private int executorThreads = 4;
        private MigrationMode migrationMode = MigrationMode.APPLY_SAFE;
        private DatabaseType databaseType = DatabaseType.AUTO;

        public Builder jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder driverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
            return this;
        }

        public Builder defaultCacheTtl(Duration defaultCacheTtl) {
            this.defaultCacheTtl = defaultCacheTtl;
            return this;
        }

        public Builder cacheMaxSize(long cacheMaxSize) {
            this.cacheMaxSize = cacheMaxSize;
            return this;
        }

        public Builder cacheMode(CacheMode cacheMode) {
            this.cacheMode = cacheMode;
            return this;
        }

        public Builder redisHost(String redisHost) {
            this.redisHost = redisHost;
            return this;
        }

        public Builder redisPort(int redisPort) {
            this.redisPort = redisPort;
            return this;
        }

        public Builder redisPassword(String redisPassword) {
            this.redisPassword = redisPassword;
            return this;
        }

        public Builder redisChannel(String redisChannel) {
            this.redisChannel = redisChannel;
            return this;
        }

        public Builder autoFlushDebounceMs(long autoFlushDebounceMs) {
            this.autoFlushDebounceMs = autoFlushDebounceMs;
            return this;
        }

        public Builder writeRetryCount(int writeRetryCount) {
            this.writeRetryCount = writeRetryCount;
            return this;
        }

        public Builder executorThreads(int executorThreads) {
            this.executorThreads = executorThreads;
            return this;
        }

        public Builder migrationMode(MigrationMode migrationMode) {
            this.migrationMode = migrationMode;
            return this;
        }

        public Builder databaseType(DatabaseType databaseType) {
            this.databaseType = databaseType;
            return this;
        }

        public NexoraConfig build() {
            if (jdbcUrl == null || jdbcUrl.isEmpty()) {
                throw new IllegalArgumentException("jdbcUrl is required");
            }
            if (driverClassName == null || driverClassName.isEmpty()) {
                DatabaseType effective = databaseType;
                if (effective == DatabaseType.AUTO) {
                    String url = jdbcUrl.toLowerCase();
                    effective = url.startsWith("jdbc:sqlite:") ? DatabaseType.SQLITE : DatabaseType.MYSQL;
                }
                driverClassName = effective == DatabaseType.SQLITE ? "org.sqlite.JDBC" : "com.mysql.cj.jdbc.Driver";
            }
            return new NexoraConfig(this);
        }
    }
}
