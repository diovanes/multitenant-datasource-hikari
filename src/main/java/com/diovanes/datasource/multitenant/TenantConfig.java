package com.diovanes.datasource.multitenant;

/**
 * Configuration for a single tenant database connection (Java 21 record).
 * Immutable POJO with all parameters needed to connect to a Postgres database.
 */
public record TenantConfig(
    String host,
    int port,
    String user,
    String password,
    String database,
    String schema,
    int poolSize,
    long connectionTimeoutMs
) {
    // Compact constructor for validation
    public TenantConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host cannot be null or empty");
        }
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("user cannot be null or empty");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password cannot be null or empty");
        }
        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException("database cannot be null or empty");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (poolSize <= 0) {
            throw new IllegalArgumentException("poolSize must be positive");
        }
        if (connectionTimeoutMs <= 0) {
            throw new IllegalArgumentException("connectionTimeoutMs must be positive");
        }
    }

    /**
     * Builder-like factory method for creating TenantConfig with defaults.
     */
    public static TenantConfig builder(String host, String user, String password, String database) {
        return new TenantConfig(host, 5432, user, password, database, "public", 10, 30000L);
    }
}
