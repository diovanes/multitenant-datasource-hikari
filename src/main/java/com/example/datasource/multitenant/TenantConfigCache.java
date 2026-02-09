package com.example.datasource.multitenant;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantConfigCache {
    private static final long CACHE_EXPIRY_MS = 2 * 60 * 60 * 1000; // 2 hours
    
    private final Map<String, TenantConfig> cache = new ConcurrentHashMap<>();
    private final String configPath;
    private final boolean isClasspath;
    private long lastLoadTime = 0;
    private final Object loadLock = new Object();

    public TenantConfigCache(String configPath, boolean isClasspath) {
        this.configPath = configPath;
        this.isClasspath = isClasspath;
    }

    /**
     * Get tenant config. Checks cache first, reloads if expired, 
     * then loads missing tenant from file and caches it.
     */
    public TenantConfig getConfig(String tenantId) throws Exception {
        // Check if cache is expired, reload all if needed
        if (isCacheExpired()) {
            reloadFromFile();
        }
        
        // Check cache for this tenant
        if (cache.containsKey(tenantId)) {
            return cache.get(tenantId);
        }
        
        // Not in cache, try to load this specific tenant from file
        TenantConfig cfg = loadSingleTenantFromFile(tenantId);
        if (cfg != null) {
            cache.put(tenantId, cfg);
        }
        return cfg;
    }

    /**
     * Preload all tenants from config file into cache.
     */
    public void preloadAll() throws Exception {
        synchronized (loadLock) {
            Map<String, TenantConfig> all = isClasspath 
                ? TenantConfigLoader.loadFromClasspath(configPath)
                : TenantConfigLoader.loadFromFile(configPath);
            cache.clear();
            cache.putAll(all);
            lastLoadTime = System.currentTimeMillis();
        }
    }

    /**
     * Reload all tenants from config file (called on cache expiry).
     */
    private void reloadFromFile() throws Exception {
        synchronized (loadLock) {
            // Double-check inside lock
            if (!isCacheExpired()) {
                return;
            }
            preloadAll();
        }
    }

    /**
     * Load a single tenant from file.
     */
    private TenantConfig loadSingleTenantFromFile(String tenantId) throws Exception {
        Map<String, TenantConfig> all = isClasspath
            ? TenantConfigLoader.loadFromClasspath(configPath)
            : TenantConfigLoader.loadFromFile(configPath);
        return all.get(tenantId);
    }

    /**
     * Check if cache has expired.
     */
    private boolean isCacheExpired() {
        return System.currentTimeMillis() - lastLoadTime > CACHE_EXPIRY_MS;
    }

    /**
     * Clear cache and reset expiry timer.
     */
    public void clear() {
        synchronized (loadLock) {
            cache.clear();
            lastLoadTime = 0;
        }
    }

    /**
     * Get cache size (for testing/monitoring).
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Get cache expiry time in milliseconds.
     */
    public long getCacheExpiryMs() {
        return CACHE_EXPIRY_MS;
    }
}
