package com.example.datasource.multitenant;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class TenantConfigLoaderTest {
    @Test
    public void loadExample() throws Exception {
        Map<String, TenantConfig> cfgs = TenantConfigLoader.loadFromClasspath("tenants.yml.example");
        assertNotNull(cfgs);
        assertTrue(cfgs.containsKey("tenant1"));
        TenantConfig t1 = cfgs.get("tenant1");
        assertEquals("localhost", t1.getHost());
        assertEquals(5432, t1.getPort());
        assertEquals("myuser", t1.getUser());
    }

    @Test
    public void loadAndVerifyTenant2() throws Exception {
        Map<String, TenantConfig> cfgs = TenantConfigLoader.loadFromClasspath("tenants.yml.example");
        assertTrue(cfgs.containsKey("tenant2"));
        TenantConfig t2 = cfgs.get("tenant2");
        assertEquals("db2.example.com", t2.getHost());
        assertEquals("user2", t2.getUser());
    }
}

