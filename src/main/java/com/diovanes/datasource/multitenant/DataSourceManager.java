package com.diovanes.datasource.multitenant;

import com.diovanes.datasource.multitenant.cache.DataSourceCacheConfig;
import com.diovanes.datasource.multitenant.cache.DataSourceCacheProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Manages HikariCP connection pools for multiple tenants.
 * Provides thread-safe access to connections with automatic pool management.
 * Uses Caffeine cache for efficient datasource caching with automatic expiration.
 */
public class DataSourceManager {
    private final TenantConfigCache configCache;
    private final DataSourceCacheProvider dataSourceCache;

    /**
     * Constructor using config from classpath resource.
     * Uses default cache configuration (2 hours TTL, 100 max size).
     *
     * @param configResourcePath path to tenants.yml in classpath (e.g., "tenants.yml")
     * @throws Exception if configuration cannot be loaded
     */
    public DataSourceManager(String configResourcePath) throws Exception {
        this(configResourcePath, true, DataSourceCacheConfig.defaults());
    }

    /**
     * Constructor using config from file system or classpath.
     * Uses default cache configuration (2 hours TTL, 100 max size).
     *
     * @param configFilePath absolute path to tenants.yml file or resource path
     * @param isClasspath if false, loads from file system; if true, loads from classpath
     * @throws Exception if configuration cannot be loaded
     */
    public DataSourceManager(String configFilePath, boolean isClasspath) throws Exception {
        this(configFilePath, isClasspath, DataSourceCacheConfig.defaults());
    }

    /**
     * Constructor using config from file system or classpath with custom cache configuration.
     *
     * @param configFilePath absolute path to tenants.yml file or resource path
     * @param isClasspath if false, loads from file system; if true, loads from classpath
     * @param cacheConfig custom cache configuration
     * @throws Exception if configuration cannot be loaded
     */
    public DataSourceManager(String configFilePath, boolean isClasspath, DataSourceCacheConfig cacheConfig) throws Exception {
        this.configCache = new TenantConfigCache(configFilePath, isClasspath);
        this.configCache.preloadAll();

        // Initialize datasource cache with loader function
        this.dataSourceCache = new DataSourceCacheProvider(
                tenantId -> createDataSourceFor(configCache.getConfig(tenantId)),
                cacheConfig
        );
    }

    /**
     * Constructor using pre-loaded tenant configs (for testing).
     * 
     * @param tenantConfigs map of tenantId to TenantConfig
     */
    public DataSourceManager(Map<String, TenantConfig> tenantConfigs) {
        this(tenantConfigs, DataSourceCacheConfig.defaults());
    }

    /**
     * Constructor using pre-loaded tenant configs with custom cache config (for testing).
     *
     * @param tenantConfigs map of tenantId to TenantConfig
     * @param cacheConfig custom cache configuration
     */
    public DataSourceManager(Map<String, TenantConfig> tenantConfigs, DataSourceCacheConfig cacheConfig) {
        this.configCache = null;

        // Initialize datasource cache with in-memory tenant configs
        this.dataSourceCache = new DataSourceCacheProvider(
                tenantId -> {
                    var cfg = tenantConfigs.get(tenantId);
                    if (cfg == null) {
                        throw new RuntimeException("No configuration found for tenant: " + tenantId);
                    }
                    return createDataSourceFor(cfg);
                },
                cacheConfig
        );
    }

    /**
     * Get a connection for the given tenant ID.
     * Uses Caffeine cache to retrieve/create HikariDataSource pools efficiently.
     *
     * @param tenantId the tenant identifier
     * @return a database connection for the tenant
     * @throws SQLException if tenant not found or connection cannot be obtained
     */
    public Connection getConnection(String tenantId) throws SQLException {
        try {
            var ds = dataSourceCache.get(tenantId);
            return ds.getConnection();
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Error retrieving connection for tenant: " + tenantId, e);
        }
    }

    /**
     * Create a new HikariDataSource pool for the given tenant configuration.
     * 
     * @param cfg the tenant configuration
     * @return a new HikariDataSource
     */
    private HikariDataSource createDataSourceFor(TenantConfig cfg) {
        var hc = new HikariConfig();

        // Support both PostgreSQL and H2 (for testing)
        String jdbcUrl;
        boolean isH2 = cfg.host().contains("mem:") || (cfg.host().equals("localhost") && cfg.database().startsWith("testdb_"));

        if (isH2) {
            // H2 in-memory database for testing
            jdbcUrl = String.format("jdbc:h2:mem:%s", cfg.database());
        } else {
            // PostgreSQL
            jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s",
                cfg.host(), cfg.port(), cfg.database());
        }

        hc.setJdbcUrl(jdbcUrl);
        hc.setUsername(cfg.user());
        hc.setPassword(cfg.password());
        hc.setMaximumPoolSize(cfg.poolSize());
        hc.setConnectionTimeout(cfg.connectionTimeoutMs());
        
        // Set schema only for PostgreSQL (H2 doesn't support SET search_path)
        if (!isH2 && cfg.schema() != null && !cfg.schema().isBlank()) {
            hc.setConnectionInitSql("SET search_path TO " + cfg.schema());
        }

        return new HikariDataSource(hc);
    }

    /**
     * Close all connection pools and clear caches.
     */
    public void closeAll() {
        dataSourceCache.invalidateAll();
        if (configCache != null) {
            configCache.clear();
        }
    }

    /**
     * Get the HikariDataSource pool for the given tenant ID.
     * Uses Caffeine cache for efficient retrieval and creation.
     *
     * @param tenantId the tenant identifier
     * @return the HikariDataSource for the tenant
     * @throws SQLException if tenant not found or datasource cannot be created
     */
    public HikariDataSource getDataSource(String tenantId) throws SQLException {
        try {
            return dataSourceCache.get(tenantId);
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Error retrieving datasource for tenant: " + tenantId, e);
        }
    }

    /**
     * Invalidate a specific tenant's datasource cache entry.
     * This will close the datasource connection pool.
     *
     * @param tenantId the tenant identifier
     */
    public void invalidateDataSourceCache(String tenantId) {
        dataSourceCache.invalidate(tenantId);
    }

    /**
     * Get the datasource cache provider (for monitoring/testing).
     *
     * @return the DataSourceCacheProvider instance
     */
    public DataSourceCacheProvider getDataSourceCache() {
        return dataSourceCache;
    }

    /**
     * Get the config cache instance (for testing/monitoring).
     *
     * @return the TenantConfigCache instance
     */
    public TenantConfigCache getConfigCache() {
        return configCache;
    }
}
