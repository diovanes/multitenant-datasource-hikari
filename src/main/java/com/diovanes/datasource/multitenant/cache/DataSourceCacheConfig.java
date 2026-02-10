package com.diovanes.datasource.multitenant.cache;

/**
 * Configuration for the DataSource Caffeine cache.
 * Provides customizable TTL (Time-To-Live) and maximum cache size.
 *
 * Thread-safe and immutable configuration object.
 */
public class DataSourceCacheConfig {

    private final long expireAfterWriteMs;
    private final long maxSize;
    private final boolean recordStats;

    /**
     * Create a cache configuration with custom TTL and size.
     *
     * @param expireAfterWriteMs time in milliseconds before entries expire (default: 2 hours)
     * @param maxSize maximum number of datasources to cache (default: 100)
     * @param recordStats whether to record cache statistics for monitoring
     */
    public DataSourceCacheConfig(long expireAfterWriteMs, long maxSize, boolean recordStats) {
        if (expireAfterWriteMs <= 0) {
            throw new IllegalArgumentException("expireAfterWriteMs must be positive");
        }
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        this.expireAfterWriteMs = expireAfterWriteMs;
        this.maxSize = maxSize;
        this.recordStats = recordStats;
    }

    /**
     * Create cache configuration with default values.
     * - Expiration: 2 hours
     * - Max size: 100 datasources
     * - Stats: enabled
     */
    public static DataSourceCacheConfig defaults() {
        return new DataSourceCacheConfig(2 * 60 * 60 * 1000, 100, true);
    }

    /**
     * Create cache configuration with custom TTL, default size and stats enabled.
     *
     * @param expireAfterWriteMs time in milliseconds before entries expire
     * @return new configuration
     */
    public static DataSourceCacheConfig withExpiry(long expireAfterWriteMs) {
        return new DataSourceCacheConfig(expireAfterWriteMs, 100, true);
    }

    /**
     * Create cache configuration with custom TTL and size, stats enabled.
     *
     * @param expireAfterWriteMs time in milliseconds before entries expire
     * @param maxSize maximum number of datasources to cache
     * @return new configuration
     */
    public static DataSourceCacheConfig custom(long expireAfterWriteMs, long maxSize) {
        return new DataSourceCacheConfig(expireAfterWriteMs, maxSize, true);
    }

    public long getExpireAfterWriteMs() {
        return expireAfterWriteMs;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public boolean isRecordStats() {
        return recordStats;
    }

    @Override
    public String toString() {
        return "DataSourceCacheConfig{" +
                "expireAfterWriteMs=" + expireAfterWriteMs +
                "ms (" + (expireAfterWriteMs / 60000) + " minutes)" +
                ", maxSize=" + maxSize +
                ", recordStats=" + recordStats +
                '}';
    }
}

