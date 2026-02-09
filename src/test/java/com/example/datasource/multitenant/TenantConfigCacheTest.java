package com.example.datasource.multitenant;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class TenantConfigCacheTest {
    private TenantConfigCache cache;

    @Before
    public void setUp() {
        cache = new TenantConfigCache("tenants.yml.example", true);
    }

    @Test
    public void testPreloadAll() throws Exception {
        cache.preloadAll();
        assertTrue(cache.getCacheSize() > 0);
    }

    @Test
    public void testGetConfigFromCache() throws Exception {
        cache.preloadAll();
        TenantConfig cfg = cache.getConfig("tenant1");
        assertNotNull(cfg);
        assertEquals("localhost", cfg.getHost());
    }

    @Test
    public void testGetNonExistentTenant() throws Exception {
        cache.preloadAll();
        TenantConfig cfg = cache.getConfig("nonexistent");
        assertNull(cfg);
    }

    @Test
    public void testCacheClear() throws Exception {
        cache.preloadAll();
        assertTrue(cache.getCacheSize() > 0);
        cache.clear();
        assertEquals(0, cache.getCacheSize());
    }

    @Test
    public void testCacheExpiryMs() {
        long expiry = cache.getCacheExpiryMs();
        assertEquals(2 * 60 * 60 * 1000, expiry); // 2 hours in milliseconds
    }
}
