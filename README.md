# multitenant-datasource-hikari

Biblioteca Java para controlar pools de conexão por tenant usando HikariCP (Postgres) com cache inteligente.

## Features

- **Cache com expiração automática (2 horas)**: Todos os tenants são carregados em memória e revalidados a cada 2 horas.
- **Carregamento lazy de novos tenants**: Se um tenantId não estiver no cache, é carregado dinamicamente do arquivo.
- **API simples**: Obtenha `Connection` via `getConnection(tenantId)`.
- **Suporte a classpath e filesystem**: Carregue configuração de arquivo ou recurso classpath.
- **Pool de conexões por tenant**: Cada tenant possui seu próprio `HikariDataSource`.

## Dependência Maven

```xml
<dependency>
  <groupId>com.example.datasource</groupId>
  <artifactId>multitenant-datasource-hikari</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Configuração

Adicione `tenants.yml` no classpath ou passe o caminho completo.

**Exemplo `tenants.yml`:**

```yaml
tenants:
  tenant1:
    host: localhost
    port: 5432
    user: myuser
    password: mypass
    database: mydb
    schema: public
    poolSize: 10
    connectionTimeoutMs: 30000

  tenant2:
    host: db2.example.com
    port: 5432
    user: user2
    password: pass2
    database: tenant2db
    schema: public
    poolSize: 5
```

## Uso

### Carregamento de classpath:
```java
DataSourceManager mgr = new DataSourceManager("tenants.yml");
Connection conn = mgr.getConnection("tenant1");
// use connection
conn.close();
mgr.closeAll();
```

### Carregamento de arquivo (filesystem):
```java
DataSourceManager mgr = new DataSourceManager("/path/to/tenants.yml", false);
Connection conn = mgr.getConnection("tenant1");
// use connection
conn.close();
```

## Cache behavior

- **Preload inicial**: Ao criar `DataSourceManager`, todos os tenants são carregados em cache.
- **Lazy loading**: Se um tenantId não estiver em cache, é buscado no arquivo automaticamente.
- **Expiração (2h)**: A cada 2 horas, o cache é marcado como expirado e recarregado do arquivo.
- **Thread-safe**: Operações de cache são sincronizadas e seguras para ambientes multi-thread.

## Segurança

Recomenda-se usar variáveis de ambiente para senhas:

```yaml
tenants:
  tenant1:
    host: ${DB_HOST}
    port: ${DB_PORT}
    user: ${DB_USER}
    password: ${DB_PASS}
    database: ${DB_NAME}
```

Ou implementar um custom loader que resolva variáveis.
