# Guia de Uso: Cache Caffeine Multi-Tenant

## 🚀 Quick Start

### Instalação

1. Adicione a dependência ao seu `pom.xml`:

```xml
<dependency>
  <groupId>com.diovanes.datasource</groupId>
  <artifactId>multitenant-datasource-hikari</artifactId>
  <version>0.1.0</version>
</dependency>
```

2. Crie um arquivo `tenants.yml` no classpath ou file system:

```yaml
tenants:
  tenant1:
    host: db1.example.com
    port: 5432
    user: user1
    password: pass1
    database: db_tenant1
    schema: public
    poolSize: 10
    connectionTimeoutMs: 30000
    
  tenant2:
    host: db2.example.com
    port: 5432
    user: user2
    password: pass2
    database: db_tenant2
    schema: public
    poolSize: 15
    connectionTimeoutMs: 30000
```

### Uso Básico

```java
import com.diovanes.datasource.multitenant.DataSourceManager;
import java.sql.Connection;

public class MyApp {
    public static void main(String[] args) throws Exception {
        // Opção 1: Carregar do classpath
        var manager = new DataSourceManager("tenants.yml");
        
        // Opção 2: Carregar do file system
        var manager = new DataSourceManager(
            "/etc/config/tenants.yml", 
            false  // não é classpath
        );
        
        // Obter conexão (datasource é cacheado automaticamente)
        var conn = manager.getConnection("tenant1");
        
        // Usar a conexão
        var stmt = conn.createStatement();
        var rs = stmt.executeQuery("SELECT * FROM users");
        // ...
        
        conn.close();
        
        // Cleanup no final da aplicação
        manager.closeAll();
    }
}
```

---

## ⚙️ Configuração Avançada

### Cache Customizado

```java
import com.diovanes.datasource.multitenant.cache.DataSourceCacheConfig;

// Configuração padrão (2h TTL, 100 max size)
var config1 = DataSourceCacheConfig.defaults();

// TTL customizado
var config2 = DataSourceCacheConfig.withExpiry(5 * 60 * 1000); // 5 min

// TTL + Size customizado
var config3 = DataSourceCacheConfig.custom(
    10 * 60 * 1000,  // 10 minutos TTL
    50               // Max 50 datasources
);

// Criar manager com cache customizado
var manager = new DataSourceManager(
    "tenants.yml",
    true,            // from classpath
    config3          // custom cache config
);
```

### Uso com Spring Boot (Opcional)

Se estiver usando Spring Boot, você pode criar um bean:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.diovanes.datasource.multitenant.DataSourceManager;
import com.diovanes.datasource.multitenant.cache.DataSourceCacheConfig;

@Configuration
public class DataSourceConfig {
    
    @Bean
    public DataSourceManager dataSourceManager() throws Exception {
        var cacheConfig = DataSourceCacheConfig.custom(
            5 * 60 * 1000,  // 5 min
            100
        );
        return new DataSourceManager(
            "tenants.yml",
            true,
            cacheConfig
        );
    }
}
```

Então injetar onde precisar:

```java
@Service
public class UserService {
    
    @Autowired
    private DataSourceManager dataSourceManager;
    
    public List<User> getUsers(String tenantId) throws SQLException {
        var conn = dataSourceManager.getConnection(tenantId);
        // ... usar conexão
        conn.close();
        return users;
    }
}
```

---

## 📊 Monitoramento

### Acessar Estatísticas

```java
var manager = new DataSourceManager("tenants.yml");

// ... usar datasources ...

// Obter provider de cache
var cacheProvider = manager.getDataSourceCache();

// Estatísticas detalhadas
String stats = cacheProvider.getDetailedStats();
System.out.println(stats);
// Output:
// Cache Stats: size=15, hits=142, misses=15, loadSuccesses=15, 
// loadFailures=0, hitRate=90.45%, avgLoadPenalty=45 ms, evictions=0

// Estatísticas raw
var cacheStats = cacheProvider.stats();
System.out.println("Hit Rate: " + (cacheStats.hitRate() * 100) + "%");
System.out.println("Size: " + cacheProvider.size());
System.out.println("Evictions: " + cacheStats.evictionCount());
```

### Logging

O projeto usa SLF4J nativo. Configure logging no `logback.xml`:

```xml
<logger name="com.diovanes.datasource.multitenant" level="DEBUG"/>
```

---

## 🔄 Padrões de Uso

### Padrão 1: Connection-per-request

```java
public List<User> getUsers(String tenantId) throws SQLException {
    var conn = dataSourceManager.getConnection(tenantId);
    try {
        return queryUsers(conn);
    } finally {
        conn.close();
    }
}
```

### Padrão 2: Obter DataSource Diretamente

```java
public List<User> getUsers(String tenantId) throws SQLException {
    var ds = dataSourceManager.getDataSource(tenantId);
    
    // Usar pool de conexões do HikariDataSource
    var conn = ds.getConnection();
    try {
        return queryUsers(conn);
    } finally {
        conn.close();
    }
}
```

### Padrão 3: Com Try-with-resources

```java
public List<User> getUsers(String tenantId) throws SQLException {
    try (var conn = dataSourceManager.getConnection(tenantId)) {
        return queryUsers(conn);
    }
}
```

### Padrão 4: Com JdbcTemplate (Spring)

```java
@Service
public class UserService {
    
    @Autowired
    private DataSourceManager dataSourceManager;
    
    public List<User> getUsers(String tenantId) {
        var ds = dataSourceManager.getDataSource(tenantId);
        var template = new JdbcTemplate(ds);
        
        return template.query(
            "SELECT * FROM users",
            (rs, rowNum) -> new User(rs.getLong("id"), rs.getString("name"))
        );
    }
}
```

---

## 🛡️ Tratamento de Erros

```java
try {
    var conn = dataSourceManager.getConnection("tenant1");
    // ... usar conexão
} catch (SQLException ex) {
    if (ex.getMessage().contains("No configuration found")) {
        // Tenant não existe
        System.err.println("Tenant não configurado");
    } else {
        // Erro de conexão
        System.err.println("Erro de conexão: " + ex.getMessage());
    }
}
```

### Invalidar Cache Manualmente

```java
// Se uma conexão falhar e você quer forçar recriação
try {
    var conn = manager.getConnection("tenant1");
    // ... use connection
} catch (SQLException ex) {
    // Invalidar cache para este tenant
    manager.invalidateDataSourceCache("tenant1");
    
    // Próxima requisição criará novo datasource
}
```

---

## 📈 Performance Tips

### 1. Configure pool size apropriadamente

```yaml
# Para reads-only
poolSize: 5

# Para mixed workload
poolSize: 10

# Para high-concurrency
poolSize: 20
```

### 2. Ajuste TTL conforme necessário

```java
// Muitas conexões? Reduzir TTL
var config = DataSourceCacheConfig.custom(
    30 * 60 * 1000,  // 30 minutos
    50
);

// Conexões estáveis? Aumentar TTL
var config = DataSourceCacheConfig.custom(
    4 * 60 * 60 * 1000,  // 4 horas
    100
);
```

### 3. Limite máximo de datasources

```java
// Aplicação pequena (5-10 tenants)
var config = DataSourceCacheConfig.custom(2 * 60 * 60 * 1000, 20);

// Aplicação média (50-100 tenants)
var config = DataSourceCacheConfig.custom(2 * 60 * 60 * 1000, 100);

// Aplicação grande (1000+ tenants)
var config = DataSourceCacheConfig.custom(2 * 60 * 60 * 1000, 500);
```

### 4. Monitorar hit rate

```java
var stats = manager.getDataSourceCache().stats();
double hitRate = stats.hitRate() * 100;

if (hitRate < 50) {
    System.warn("Hit rate baixa: " + hitRate + "%");
    // Aumentar TTL ou maxSize
}
```

---

## 🧪 Testes

### Teste Unitário com Mock

```java
@Test
public void testGetConnection() throws Exception {
    var config = new TenantConfig(
        "localhost", 5432, "user", "pass", 
        "testdb", "public", 10, 30000
    );
    
    var tenantConfigs = new HashMap<String, TenantConfig>();
    tenantConfigs.put("tenant1", config);
    
    var manager = new DataSourceManager(tenantConfigs);
    
    var conn = manager.getConnection("tenant1");
    assertNotNull(conn);
    assertFalse(conn.isClosed());
    conn.close();
}
```

### Teste de Cache

```java
@Test
public void testCaching() throws Exception {
    var manager = new DataSourceManager(tenantConfigs);
    
    var ds1 = manager.getDataSource("tenant1");
    var ds2 = manager.getDataSource("tenant1");
    
    assertSame(ds1, ds2);  // Mesma instância!
}
```

---

## 🐛 Troubleshooting

### Erro: "No configuration found for tenant"

**Causa**: Tenant não definido em `tenants.yml`

**Solução**: Adicione o tenant ao arquivo de configuração

### Erro: "Connection timeout"

**Causa**: Datasource não consegue conectar ao banco

**Solução**: 
- Verifique credenciais em `tenants.yml`
- Verifique conectividade de rede
- Aumente `connectionTimeoutMs`

### Alta latência

**Causa**: Cache hit rate baixo

**Solução**:
- Aumente `maxSize` 
- Aumente `TTL (expireAfterWriteMs)`
- Monitore com `getDetailedStats()`

### Vazamento de memória

**Causa**: Conexões não fechadas

**Solução**:
- Use try-with-resources
- Chame `conn.close()` sempre
- Chame `manager.closeAll()` no shutdown

---

## 📞 Suporte

Para mais informações:
- Consulte `CAFFEINE_CACHE_IMPLEMENTATION.md`
- Veja testes em `src/test/java`
- Javadoc das classes

---

**Versão**: 0.1.0  
**Atualizado**: Fevereiro 2026

