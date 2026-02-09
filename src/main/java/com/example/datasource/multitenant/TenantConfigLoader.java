package com.example.datasource.multitenant;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class TenantConfigLoader {
    public static Map<String, TenantConfig> loadFromClasspath(String resourcePath) throws Exception {
        InputStream is = TenantConfigLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("Resource not found on classpath: " + resourcePath);
        }
        return loadFromInputStream(is);
    }

    public static Map<String, TenantConfig> loadFromFile(String filePath) throws Exception {
        try (InputStream is = new FileInputStream(filePath)) {
            return loadFromInputStream(is);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, TenantConfig> loadFromInputStream(InputStream is) {
        Yaml yaml = new Yaml();
        Object obj = yaml.load(is);
        Map<String, TenantConfig> result = new HashMap<>();
        if (!(obj instanceof Map)) return result;
        Map<String, Object> root = (Map<String, Object>) obj;

        Object tenantsObj = root.get("tenants");
        if (!(tenantsObj instanceof Map)) return result;

        Map<String, Object> tenants = (Map<String, Object>) tenantsObj;
        for (Map.Entry<String, Object> e : tenants.entrySet()) {
            String tenantId = e.getKey();
            Object v = e.getValue();
            if (!(v instanceof Map)) continue;
            Map<String, Object> map = (Map<String, Object>) v;
            TenantConfig cfg = new TenantConfig();
            if (map.containsKey("host")) cfg.setHost(String.valueOf(map.get("host")));
            if (map.containsKey("port")) cfg.setPort(Integer.parseInt(String.valueOf(map.get("port"))));
            if (map.containsKey("user")) cfg.setUser(String.valueOf(map.get("user")));
            if (map.containsKey("password")) cfg.setPassword(String.valueOf(map.get("password")));
            if (map.containsKey("database")) cfg.setDatabase(String.valueOf(map.get("database")));
            if (map.containsKey("schema")) cfg.setSchema(String.valueOf(map.get("schema")));
            if (map.containsKey("poolSize")) cfg.setPoolSize(Integer.parseInt(String.valueOf(map.get("poolSize"))));
            if (map.containsKey("connectionTimeoutMs")) cfg.setConnectionTimeoutMs(Long.parseLong(String.valueOf(map.get("connectionTimeoutMs"))));
            result.put(tenantId, cfg);
        }
        return result;
    }
}
