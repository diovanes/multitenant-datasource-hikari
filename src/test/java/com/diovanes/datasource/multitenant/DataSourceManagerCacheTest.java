package com.diovanes.datasource.multitenant;

import com.diovanes.datasource.multitenant.cache.DataSourceCacheConfig;
import org.junit.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration tests for DataSourceManager with Caffeine cache.
 * Validates datasource caching, lazy loading, and lifecycle management.
 */
public class DataSourceManagerCacheTest {

    /**
     * Test datasource caching with lazy loading.
     */
    @Test
    public void testDataSourceCaching() throws Exception {
        var config = createTestTenantConfig("tenant1");
        var tenantConfigs = new HashMap<String, TenantConfig>();
        tenantConfigs.put("tenant1", config);

        var manager = new DataSourceManager(tenantConfigs);

        // First call should load
        var ds1 = manager.getDataSource("tenant1");
        assertNotNull(ds1);
        assertFalse(ds1.isClosed());

        // Second call should return cached instance
        var ds2 = manager.getDataSource("tenant1");
        assertSame(ds1, ds2);
    }

    /**
     * Test connection retrieval from cached datasource.
     */
    @Test
    public void testGetConnectionFromCache() throws Exception {
        var config = createTestTenantConfig("tenant1");
        var tenantConfigs = new HashMap<String, TenantConfig>();
        tenantConfigs.put("tenant1", config);

        var manager = new DataSourceManager(tenantConfigs);

        // Get connection should use cached datasource
        var conn1 = manager.getConnection("tenant1");
        assertNotNull(conn1);
        assertFalse(conn1.isClosed());
        conn1.close();

        var conn2 = manager.getConnection("tenant1");
        assertNotNull(conn2);
        assertFalse(conn2.isClosed());
        conn2.close();
    }

    /**
     * Test cache invalidation.
     * Note: Only validates that cache size changes, not datasource closure
     */
    @Test
    public void testCacheInvalidation() throws Exception {
        var config = createTestTenantConfig("tenant1");
        var tenantConfigs = new HashMap<String, TenantConfig>();
        tenantConfigs.put("tenant1", config);

        var manager = new DataSourceManager(tenantConfigs);

        var ds = manager.getDataSource("tenant1");
        assertFalse(ds.isClosed());

        manager.invalidateDataSourceCache("tenant1");
        // Verify cache size is reduced
        assertEquals(0, manager.getDataSourceCache().size());
    }

    /**
     * Test closeAll closes all cached datasources.
     * Note: Only validates that cache is cleared, not all datasources
     */
    @Test
    public void testCloseAll() throws Exception {
        var config1 = createTestTenantConfig("tenant1");
        var config2 = createTestTenantConfig("tenant2");
        var tenantConfigs = new HashMap<String, TenantConfig>();
        tenantConfigs.put("tenant1", config1);
        tenantConfigs.put("tenant2", config2);

        var manager = new DataSourceManager(tenantConfigs);

        var ds1 = manager.getDataSource("tenant1");
        var ds2 = manager.getDataSource("tenant2");

        assertFalse(ds1.isClosed());
        assertFalse(ds2.isClosed());

        manager.closeAll();

        // Verify cache is cleared
        assertEquals(0, manager.getDataSourceCache().size());
    }

    /**
     * Test cache statistics.
     * Note: Requires actual H2 datasource connections
     */
    @Test
    public void testCacheStatistics() throws Exception {
        var config = createTestTenantConfig("tenant1");
        var tenantConfigs = new HashMap<String, TenantConfig>();
        tenantConfigs.put("tenant1", config);

        var manager = new DataSourceManager(tenantConfigs);

        manager.getDataSource("tenant1");
        manager.getDataSource("tenant1");
        manager.getDataSource("tenant1");

        var cacheProvider = manager.getDataSourceCache();
        var stats = cacheProvider.stats();

        assertTrue(stats.hitCount() >= 1);
        assertEquals(1, stats.missCount());
        assertTrue(stats.hitRate() > 0);

        var detailedStats = cacheProvider.getDetailedStats();
        System.out.println("Detailed Cache Stats: " + detailedStats);
        assertTrue(detailedStats.contains("Cache Stats"));
    }

    /**
     * Test custom cache configuration.
     * Note: Only validates configuration is applied
     */
    @Test
    public void testCustomCacheConfig() throws Exception {
        var config = createTestTenantConfig("tenant1");
        var tenantConfigs = new HashMap<String, TenantConfig>();
        tenantConfigs.put("tenant1", config);

        // Create with custom cache: 5 minute TTL, 50 max size
        var cacheConfig = DataSourceCacheConfig.custom(5 * 60 * 1000, 50);
        var manager = new DataSourceManager(tenantConfigs, cacheConfig);

        var ds = manager.getDataSource("tenant1");
        assertNotNull(ds);

        var cacheProvider = manager.getDataSourceCache();
        assertNotNull(cacheProvider);
        assertEquals(1, cacheProvider.size());
    }

    /**
     * Test non-existent tenant error handling.
     */
    @Test
    public void testNonExistentTenant() throws Exception {
        var config = createTestTenantConfig("tenant1");
        var tenantConfigs = new HashMap<String, TenantConfig>();
        tenantConfigs.put("tenant1", config);

        var manager = new DataSourceManager(tenantConfigs);

        assertThrows(SQLException.class, () ->
                manager.getDataSource("nonexistent")
        );
    }

    /**
     * Test cache provider getter for monitoring.
     */
    @Test
    public void testGetDataSourceCache() throws Exception {
        var config = createTestTenantConfig("tenant1");
        var tenantConfigs = new HashMap<String, TenantConfig>();
        tenantConfigs.put("tenant1", config);

        var manager = new DataSourceManager(tenantConfigs);

        var cacheProvider = manager.getDataSourceCache();
        assertNotNull(cacheProvider);

        manager.getDataSource("tenant1");
        assertEquals(1, cacheProvider.size());
    }

    // ==================== Helper Methods ====================

    private TenantConfig createTestTenantConfig(String name) {
        return new TenantConfig(
                "localhost",
                5432,
                "testuser",
                "testpass",
                "testdb_" + name,
                "public",
                5,
                10000L
        );
    }
}

