# 🛠️ Comandos Úteis - Projeto DataSource Multi-Tenant Cache

## Compilação e Build

### Compilar o projeto
```bash
cd /Users/diovaneschumann/git/datasource-lib
mvn clean compile
```

### Executar todos os testes
```bash
mvn clean test
```

### Gerar JAR
```bash
mvn clean package
```

### Build completo (compile + test + package)
```bash
mvn clean install
```

### Gerar com skip de testes (rápido)
```bash
mvn clean package -DskipTests
```

---

## Teste e Qualidade

### Executar apenas testes unitários
```bash
mvn test -Dtest=DataSourceCacheProviderTest
```

### Executar apenas testes de integração
```bash
mvn test -Dtest=DataSourceManagerCacheTest
```

### Ver relatório de testes
```bash
cat target/surefire-reports/TEST-*.xml
```

### Verificação completa (compile + test + static analysis)
```bash
mvn clean verify
```

---

## Desenvolvimento

### Compilar em watch mode (Maven 3.9+)
```bash
mvn -f pom.xml clean compile
# Depois editar e compilar de novo
```

### Ver dependências do projeto
```bash
mvn dependency:tree
```

### Checar por dependências desatualizadas
```bash
mvn versions:display-dependency-updates
```

---

## Inspecionar Artefatos

### Listar arquivos no JAR
```bash
jar tf target/multitenant-datasource-hikari-0.1.0.jar
```

### Extrair JAR
```bash
cd target && jar xf multitenant-datasource-hikari-0.1.0.jar
```

### Tamanho do JAR
```bash
ls -lh target/multitenant-datasource-hikari-0.1.0.jar
```

---

## Localizar Arquivos

### Encontrar todas as classes de teste
```bash
find src/test -name "*Test.java"
```

### Encontrar todas as classes da biblioteca
```bash
find src/main -name "*.java"
```

### Verificar estrutura de diretórios
```bash
tree src/
```

---

## Inspeção de Código

### Compilar com warnings detalhados
```bash
mvn clean compile -X
```

### Ver linhas de código (LOC)
```bash
find src -name "*.java" | xargs wc -l | tail -1
```

### Listar classes e métodos
```bash
find src -name "*.java" -type f | xargs grep -n "public\|class\|interface"
```

---

## Documentação

### Gerar Javadoc
```bash
mvn javadoc:javadoc
# Output: target/site/apidocs/
```

### Abrir Javadoc em navegador
```bash
open target/site/apidocs/index.html
```

---

## Clean e Reset

### Limpar build artifacts
```bash
mvn clean
```

### Reset completo (remove target/)
```bash
rm -rf target/
```

### Reset com reconstrução
```bash
mvn clean install
```

---

## Troubleshooting

### Mostrar stack trace completo
```bash
mvn clean test -e
```

### Debug detalhado
```bash
mvn clean test -X
```

### Limpar cache Maven
```bash
rm -rf ~/.m2/repository
mvn clean install
```

### Verificar versão do Maven
```bash
mvn -version
```

---

## Métricas e Relatórios

### Gerar relatório de testes
```bash
mvn test
cat target/surefire-reports/TEST-*.txt
```

### Ver estatísticas do cache (durante testes)
```bash
mvn test | grep "Cache Stats"
```

### Contar testes
```bash
mvn test | grep "Tests run:"
```

---

## Dicas Úteis

### Executar um teste específico com debug
```bash
mvn test -Dtest=DataSourceCacheProviderTest#testLazyLoading -e
```

### Compilar sem testes
```bash
mvn compile -DskipTests
```

### Manter Maven rodando (para IDE)
```bash
mvn process-resources
```

### Ver tempo de build
```bash
mvn clean install -Dorg.slf4j.simpleLogger.defaultLogLevel=info
```

---

## IDE Integration

### Atualizar dependências (Eclipse/IntelliJ)
```bash
mvn eclipse:eclipse
# ou
mvn idea:idea
```

### Limpar IDE caches
```bash
rm -rf .classpath .project
mvn clean eclipse:eclipse
```

---

## Deploy

### Instalar JAR localmente
```bash
mvn install
```

### Instalar com fontes
```bash
mvn install -Dsources
```

### Deploy para repositório remoto
```bash
mvn deploy
# Requer configuração em settings.xml
```

---

## Monitoramento em Tempo Real

### Ver logs de compilação
```bash
mvn clean compile 2>&1 | tee compile.log
```

### Executar testes com saída colorida
```bash
mvn test -e
```

### Mostrar apenas erros
```bash
mvn test 2>&1 | grep -i error
```

---

## Checklist Pré-Deploy

```bash
# 1. Limpar build anterior
mvn clean

# 2. Compilar
mvn compile

# 3. Executar testes
mvn test

# 4. Gerar relatório
mvn surefire-report:report

# 5. Gerar JAR
mvn package

# 6. Verificar JAR
ls -lh target/*.jar
jar tf target/multitenant-datasource-hikari-0.1.0.jar | head -20

# 7. Instalar localmente
mvn install

echo "✅ Pronto para deploy!"
```

---

## Comando All-in-One

### Build, teste e verifica completo
```bash
mvn clean verify -DskipTests=false -e && \
echo "✅ BUILD SUCCESSFUL" || echo "❌ BUILD FAILED"
```

---

## Referência Rápida

| Comando | Descrição |
|---------|-----------|
| `mvn clean` | Remove build artifacts |
| `mvn compile` | Compila código-fonte |
| `mvn test` | Executa testes |
| `mvn package` | Cria JAR |
| `mvn install` | Instala JAR localmente |
| `mvn clean install` | Clean + compile + test + package + install |
| `mvn -version` | Mostra versão do Maven |
| `mvn help:active-profiles` | Mostra perfis ativos |
| `mvn dependency:tree` | Mostra árvore de dependências |
| `mvn enforcer:enforce` | Verifica regras (se configurado) |

---

**Última atualização**: Fevereiro 2026

