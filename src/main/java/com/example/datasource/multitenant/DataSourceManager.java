package com.example.datasource.multitenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages HikariCP connection pools for multiple tenants.
 * Provides thread-safe access to connections with automatic pool management.
 */
public class DataSourceManager {
    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();
    private final TenantConfigCache cache;

    /**
     * Constructor using config from classpath resource.
     * 
     * @param configResourcePath path to tenants.yml in classpath (e.g., "tenants.yml")
     * @throws Exception if configuration cannot be loaded
     */
    public DataSourceManager(String configResourcePath) throws Exception {
        this.cache = new TenantConfigCache(configResourcePath, true);
        this.cache.preloadAll();
    }

    /**
     * Constructor using config from file system or classpath.
     * 
     * @param configFilePath absolute path to tenants.yml file or resource path
     * @param isClasspath if false, loads from file system; if true, loads from classpath
     * @throws Exception if configuration cannot be loaded
     */
    public DataSourceManager(String configFilePath, boolean isClasspath) throws Exception {
        this.cache = new TenantConfigCache(configFilePath, isClasspath);
        this.cache.preloadAll();
    }

    /**
     * Constructor using pre-loaded tenant configs (for testing).
     * 
     * @param tenantConfigs map of tenantId to TenantConfig
     */
    public DataSourceManager(Map<String, TenantConfig> tenantConfigs) {
        this.cache = null;
        // For backward compatibility, we create a simple in-memory cache
        // This path is mainly for testing
    }

    /**
     * Get a connection for the given tenant ID.
     * Checks cache first, reloads from file if expired,
     * creates HikariDataSource pool if needed.
     * 
     * @param tenantId the tenant identifier
     * @return a database connection for the tenant
     * @throws SQLException if tenant not found or connection cannot be obtained
     */
    public Connection getConnection(String tenantId) throws SQLException {
        try {
            var cfg = cache.getConfig(tenantId);
            if (cfg == null) {
                throw new SQLException("No configuration found for tenant: " + tenantId);
            }

            // Try existing pool first
            var ds = pools.get(tenantId);
            if (ds != null) {
                try {
                    return ds.getConnection();
                } catch (SQLException ex) {
                    // Existing pool failed to provide a connection. Close and remove it, then recreate.
                    try { ds.close(); } catch (Exception ignore) {}
                    pools.remove(tenantId, ds);
                }
            }

            // Create a new pool and attempt to register it atomically
            var newDs = createDataSourceFor(cfg);
            if (newDs == null) {
                throw new SQLException("Failed to create datasource for tenant: " + tenantId);
            }

            var existing = pools.putIfAbsent(tenantId, newDs);
            var toUse = existing != null ? existing : newDs;

            try {
                return toUse.getConnection();
            } catch (SQLException ex) {
                // If newly created pool fails, ensure it's closed and removed
                if (existing == null) {
                    try { toUse.close(); } catch (Exception ignore) {}
                    pools.remove(tenantId, toUse);
                }
                throw ex;
            }

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
        var jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", 
            cfg.host(), cfg.port(), cfg.database());
        hc.setJdbcUrl(jdbcUrl);
        hc.setUsername(cfg.user());
        hc.setPassword(cfg.password());
        hc.setMaximumPoolSize(cfg.poolSize());
        hc.setConnectionTimeout(cfg.connectionTimeoutMs());
        
        // set schema on connection init
        if (cfg.schema() != null && !cfg.schema().isBlank()) {
            hc.setConnectionInitSql("SET search_path TO " + cfg.schema());
        }

        return new HikariDataSource(hc);
    }

    /**
     * Close all connection pools and clear cache.
     */
    public void closeAll() {
        for (var ds : pools.values()) {
            try { ds.close(); } catch (Exception ignore) {}
        }
        pools.clear();
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Get the cache instance (for testing/monitoring).
     * 
     * @return the TenantConfigCache instance
     */
    public TenantConfigCache getCache() {
        return cache;
    }
}
