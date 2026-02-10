package com.diovanes.datasource.multitenant.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.zaxxer.hikari.HikariDataSource;

import java.util.concurrent.TimeUnit;

/**
 * Thread-safe provider for DataSource caching using Caffeine.
 * Manages HikariDataSource lifecycle with automatic expiration and size limits.
 *
 * Features:
 * - Lazy loading of datasources on first access
 * - Automatic expiration after TTL
 * - Size limit with LRU eviction policy
 * - Cache statistics tracking (if enabled)
 * - Safe cleanup on entry removal
 */
public class DataSourceCacheProvider {

    private final Cache<String, HikariDataSource> cache;
    private final DataSourceCacheConfig config;
    private final DataSourceLoader loader;

    /**
     * Functional interface for loading/creating datasources.
     */
    @FunctionalInterface
    public interface DataSourceLoader {
        /**
         * Load or create a datasource for the given tenant ID.
         *
         * @param tenantId the tenant identifier
         * @return the HikariDataSource for the tenant
         * @throws Exception if datasource cannot be created
         */
        HikariDataSource load(String tenantId) throws Exception;
    }

    /**
     * Create a DataSourceCacheProvider with default configuration.
     *
     * @param loader function to load/create datasources
     */
    public DataSourceCacheProvider(DataSourceLoader loader) {
        this(loader, DataSourceCacheConfig.defaults());
    }

    /**
     * Create a DataSourceCacheProvider with custom configuration.
     *
     * @param loader function to load/create datasources
     * @param config cache configuration
     */
    public DataSourceCacheProvider(DataSourceLoader loader, DataSourceCacheConfig config) {
        if (loader == null) {
            throw new IllegalArgumentException("loader cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }

        this.loader = loader;
        this.config = config;
        this.cache = buildCache(config);
    }

    /**
     * Build the Caffeine cache with configured settings.
     * Includes automatic cleanup of closed datasources on removal.
     */
    private Cache<String, HikariDataSource> buildCache(DataSourceCacheConfig config) {
        var builder = Caffeine.newBuilder()
                .expireAfterWrite(config.getExpireAfterWriteMs(), TimeUnit.MILLISECONDS)
                .maximumSize(config.getMaxSize())
                .removalListener((RemovalListener<String, HikariDataSource>) (tenantId, ds, cause) -> {
                    if (ds != null && !ds.isClosed()) {
                        try {
                            ds.close();
                        } catch (Exception ex) {
                            System.err.println("Error closing datasource for tenant: " + tenantId + ": " + ex.getMessage());
                        }
                    }
                });

        if (config.isRecordStats()) {
            builder.recordStats();
        }

        return builder.build();
    }

    /**
     * Get a datasource for the given tenant ID.
     * Uses lazy loading via the provided loader function.
     *
     * @param tenantId the tenant identifier
     * @return the cached or newly loaded HikariDataSource
     * @throws Exception if tenant datasource cannot be loaded
     */
    public HikariDataSource get(String tenantId) throws Exception {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId cannot be null or empty");
        }

        try {
            return cache.get(tenantId, key -> {
                try {
                    return loader.load(tenantId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load datasource for tenant: " + tenantId, ex);
        }
    }

    /**
     * Manually invalidate cache entry for a specific tenant.
     * This will trigger the removal listener (closing the datasource if open).
     *
     * @param tenantId the tenant identifier
     */
    public void invalidate(String tenantId) {
        if (tenantId != null) {
            cache.invalidate(tenantId);
        }
    }

    /**
     * Invalidate all cache entries.
     * This will close all datasources.
     */
    public void invalidateAll() {
        cache.invalidateAll();
    }

    /**
     * Get current number of entries in the cache.
     *
     * @return cache size
     */
    public long size() {
        return cache.estimatedSize();
    }

    /**
     * Get cache statistics (if enabled in config).
     *
     * @return CacheStats object with hit/miss/load information
     */
    public CacheStats stats() {
        return cache.stats();
    }

    /**
     * Get configuration for this cache provider.
     *
     * @return the DataSourceCacheConfig
     */
    public DataSourceCacheConfig getConfig() {
        return config;
    }

    /**
     * Get detailed cache information for monitoring.
     *
     * @return string representation of cache state
     */
    public String getDetailedStats() {
        var stats = stats();
        return String.format(
                "Cache Stats: size=%d, hits=%d, misses=%d, loadSuccesses=%d, loadFailures=%d, " +
                "hitRate=%.2f%%, avgLoadPenalty=%d ms, evictions=%d",
                size(),
                stats.hitCount(),
                stats.missCount(),
                stats.loadSuccessCount(),
                stats.loadFailureCount(),
                stats.hitRate() * 100,
                (long) stats.averageLoadPenalty() / 1_000_000, // convert nanos to millis
                stats.evictionCount()
        );
    }
}

