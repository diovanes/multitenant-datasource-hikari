package com.example.datasource.multitenant;

import org.junit.Test;

import static org.junit.Assert.*;

public class TenantConfigLoaderTest {
    @Test
    public void loadExample() throws Exception {
        var cfgs = TenantConfigLoader.loadFromClasspath("tenants.yml.example");
        assertNotNull(cfgs);
        assertTrue(cfgs.containsKey("tenant1"));
        var t1 = cfgs.get("tenant1");
        assertEquals("localhost", t1.host());
        assertEquals(5432, t1.port());
        assertEquals("myuser", t1.user());
    }

    @Test
    public void loadAndVerifyTenant2() throws Exception {
        var cfgs = TenantConfigLoader.loadFromClasspath("tenants.yml.example");
        assertTrue(cfgs.containsKey("tenant2"));
        var t2 = cfgs.get("tenant2");
        assertEquals("db2.example.com", t2.host());
        assertEquals("user2", t2.user());
    }
}

