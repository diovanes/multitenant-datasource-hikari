package com.diovanes.datasource.multitenant;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads tenant configurations from YAML files (classpath or filesystem).
 * Supports YAML format with multi-tenant definitions.
 */
public class TenantConfigLoader {

    /**
     * Load tenant configurations from classpath resource.
     * 
     * @param resourcePath path to YAML resource (e.g., "tenants.yml")
     * @return map of tenantId to TenantConfig
     * @throws IllegalArgumentException if resource not found
     */
    public static Map<String, TenantConfig> loadFromClasspath(String resourcePath) throws Exception {
        var is = TenantConfigLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("Resource not found on classpath: " + resourcePath);
        }
        return loadFromInputStream(is);
    }

    /**
     * Load tenant configurations from filesystem path.
     * 
     * @param filePath absolute path to YAML file
     * @return map of tenantId to TenantConfig
     * @throws java.io.FileNotFoundException if file not found
     */
    public static Map<String, TenantConfig> loadFromFile(String filePath) throws Exception {
        try (var is = new FileInputStream(filePath)) {
            return loadFromInputStream(is);
        }
    }

    /**
     * Parse YAML input stream into tenant configurations.
     * 
     * @param is input stream containing YAML
     * @return map of tenantId to TenantConfig
     */
    @SuppressWarnings("unchecked")
    private static Map<String, TenantConfig> loadFromInputStream(InputStream is) {
        var yaml = new Yaml();
        var obj = yaml.load(is);
        var result = new HashMap<String, TenantConfig>();
        
        if (!(obj instanceof Map<?, ?> root)) return result;

        var tenantsObj = root.get("tenants");
        if (!(tenantsObj instanceof Map<?, ?> tenants)) return result;

        for (var entry : tenants.entrySet()) {
            var tenantId = (String) entry.getKey();
            if (!(entry.getValue() instanceof Map<?, ?> map)) continue;
            
            var cfg = parseTenantConfig(map);
            result.put(tenantId, cfg);
        }
        return result;
    }

    /**
     * Parse a single tenant configuration from map.
     */
    private static TenantConfig parseTenantConfig(Map<?, ?> map) {
        String host = getString(map, "host");
        int port = getInt(map, "port", 5432);
        String user = getString(map, "user");
        String password = getString(map, "password");
        String database = getString(map, "database");
        String schema = getString(map, "schema", "public");
        int poolSize = getInt(map, "poolSize", 10);
        long connectionTimeoutMs = getLong(map, "connectionTimeoutMs", 30000L);

        return new TenantConfig(host, port, user, password, database, schema, poolSize, connectionTimeoutMs);
    }

    private static String getString(Map<?, ?> map, String key) {
        var value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private static String getString(Map<?, ?> map, String key, String defaultValue) {
        var value = getString(map, key);
        return value != null ? value : defaultValue;
    }

    private static int getInt(Map<?, ?> map, String key, int defaultValue) {
        var value = map.get(key);
        return value != null ? Integer.parseInt(String.valueOf(value)) : defaultValue;
    }

    private static long getLong(Map<?, ?> map, String key, long defaultValue) {
        var value = map.get(key);
        return value != null ? Long.parseLong(String.valueOf(value)) : defaultValue;
    }
}
