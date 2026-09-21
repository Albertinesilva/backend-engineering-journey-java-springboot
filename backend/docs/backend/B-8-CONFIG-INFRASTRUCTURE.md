# ASJCatalog Backend — B-8: Configuração e Infraestrutura

> **Fase:** B-8 — análise e documentação de configuração, banco, migrations, e-mail/templates, logging, Springdoc, Jackson/MVC, transações e assincronismo.
> **Pré-requisitos:** [B-0](B-0-BACKEND-INVENTORY.md) a [B-7](B-7-WEB-LAYER.md).
> **Alterações desta fase:** um único JavaDoc novo em `src/main` (`DscatalogApplication`), este documento e notas curtas de refinamento em B-0, B-3, B-6 e B-7. Nenhuma propriedade, migration, template, dependência, teste ou comportamento foi alterado.

Rótulos: **[FATO]** confirmado por leitura de arquivo ou por inspeção de bytecode das bibliotecas do `~/.m2` (leitura, sem executar a aplicação); **[INFERÊNCIA]** deduzida das regras de auto-configuração do Spring Boot/Hibernate/Flyway sem execução; **[HIPÓTESE]** só verificável rodando a aplicação/testes. Nenhum teste, aplicação ou e-mail foi executado. **Segredos:** nenhum valor de senha, segredo ou credencial é reproduzido neste documento; para variáveis de ambiente, verificou-se apenas *se estão definidas*, nunca o valor.

---

## 1. Objetivo e escopo
Responder como a aplicação é configurada em cada perfil e quais recursos de infraestrutura existem, sem corrigir nada. Escopo lido: `src/main/java/**/config/**` e `security/config`, `DscatalogApplication`, `application*.properties`, `META-INF/additional-spring-configuration-metadata.json`, `db/migration/**`, `import.sql`, `create.sql` (raiz), `templates/**`, `messages_*.properties`, `static/**`, `banner-dev.txt`, `pom.xml`. Segurança (B-6) e web (B-7) só são referenciadas.

## 2. Arquitetura de configuração [FATO]

| Mecanismo | Onde | Observação |
|-----------|------|------------|
| Classes `@Configuration` do projeto | 5: `SpringDocOpenApiConfig`, `MessageSourceConfig`, `SecurityBeansConfig`, `AuthorizationServerConfig`, `ResourceServerConfig` | Nenhuma usa `@Profile`; a única condição é `@ConditionalOnProperty(spring.h2.console.enabled=true)` na cadeia do H2 |
| Propriedades | `application.properties` (comum) + `application-{dev,test,prod}.properties` | Perfil ativo padrão = `test` |
| `@Value` | 8 propriedades: `security.client-id`, `security.client-secret`, `security.jwt.duration`, `cors.origins`, `frontend.url`, `backend.url`, `account.activation.token.hours`, `account.password-recovery.token.minutes` | Todas com valor padrão (`${VAR:default}`); ver §17 |
| Auto-configuração Spring Boot | datasource, JPA, Flyway, mail, Thymeleaf, Jackson, MVC, Spring Data web, transações, task executor | Não há `@EnableXxx` de infraestrutura no projeto |
| `@ConfigurationProperties` | **nenhum** | Propriedades customizadas não têm classe tipada (o JSON de metadados cobre apenas 4) |

Não existem no projeto [FATO]: `@EnableAsync`, `@EnableScheduling`, `@EnableTransactionManagement`, `@EnableJpaRepositories`, `@EntityScan`, `WebMvcConfigurer`, `ObjectMapper`/`Jackson2ObjectMapperBuilder` customizado, `@EnableSpringDataWebSupport`, `TaskExecutor`, `AsyncConfigurer`, `Formatter`, `HttpMessageConverter`, `TransactionManager`, `DataSource`, `LocalValidatorFactoryBean`.

**Precedência (regra do Spring Boot) [INFERÊNCIA forte]:** argumento de linha de comando > variáveis de ambiente (incl. `SPRING_PROFILES_ACTIVE`, `SPRING_DATASOURCE_URL`…) > `application-{perfil}.properties` > `application.properties`. Portanto `SPRING_PROFILES_ACTIVE` vence `spring.profiles.active=${APP_PROFILE:test}`; os testes com `@ActiveProfiles("test")` também vencem.

## 3. Profiles

`spring.profiles.active=${APP_PROFILE:test}` está em `application.properties` (permitido pelo Spring Boot 3 em documento não específico de perfil) [FATO]. Sem `APP_PROFILE` (nem `SPRING_PROFILES_ACTIVE`), o perfil é **`test`** [FATO].

| Aspecto | `test` (padrão) | `dev` | `prod` |
|---------|-----------------|-------|--------|
| Arquivo | `application-test.properties` (112 linhas) | `application-dev.properties` (105 linhas) | `application-prod.properties` (15 linhas) |
| Datasource | **H2 em memória** `jdbc:h2:mem:testdb`, usuário `sa`, senha vazia | **PostgreSQL** `jdbc:postgresql://localhost:5432/dscatalog`, usuário/senha por env, **sem default** | **nada definido** |
| `ddl-auto` | não definido | `none` | não definido |
| Flyway | `enabled=false` | `enabled=true`, 2 locations | não definido (padrão do Boot = habilitado) |
| Schema/dados | Hibernate (`create-drop` por padrão de banco embarcado) + `import.sql` | Flyway (V001–V011, V100–V105) | ver §4.4 |
| H2 console | `enabled=true`, `/h2-console` | não definido (ver §7: devtools) | não definido |
| `show-sql` / `format_sql` | `true` / `true` | `true` / `true` | não definido |
| Mail | comum (todos) | comum | comum |
| Springdoc | `/docs-asjcatalog(.html)`, `packagesToScan` | `/docs-dscatalog(.html)`, `packagesToScan` | **nada** (padrões do springdoc) |
| Logging | app `DEBUG`; `org.springframework.web` `DEBUG`; `…mvc.method.annotation` `TRACE`; `hibernate.SQL` `DEBUG`; `org.hibernate.orm.jdbc.bind` `TRACE`; arquivo `logs/test/dscatalog-test.log` (5 MB × 5) | app `DEBUG`; `org.springframework` `INFO`; `hibernate.SQL` `DEBUG`; `…descriptor.sql.BasicBinder` `TRACE` (nome ineficaz, §12); `org.flywaydb` `DEBUG`; arquivo `logs/dev/dscatalog-dev.log` (10 MB × 10) | app `INFO`; `org.springframework.web` `WARN`; `hibernate.SQL` `OFF`; arquivo `logs/dscatalog-prod.log` (50 MB × 30) |
| Outros | banner `banner-dev.txt`; `spring.application.name=DSCatalog`; porta 8080; Jackson `indent_output=true` | banner; nome; porta 8080; locale fixo `pt_BR` (`spring.web.*`, §14); encoding UTF-8 | — |

Combinação: o Spring carrega `application.properties` e depois o arquivo do perfil, este sobrepondo o comum. **`prod` isolado não forma configuração completa** [FATO]: sem datasource, JPA e Flyway. Propriedades obrigatórias sem valor no repositório: `POSTGRES_DATASOURCE_USER` e `POSTGRES_DATASOURCE_PASSWORD` (só em `dev`; sem elas o placeholder não resolve e o contexto não sobe) [FATO/INFERÊNCIA].

## 4. Datasource

### 4.1 Por perfil [FATO]
- **test:** `org.h2.Driver`, `jdbc:h2:mem:testdb`, `sa`, senha vazia (0 caracteres). H2 2.3.232. Sem pool configurado: HikariCP padrão do Boot (sem propriedades `spring.datasource.hikari.*`).
- **dev:** `org.postgresql.Driver` (42.7.10), URL fixa para `localhost:5432/dscatalog`; usuário/senha só por ambiente.
- **prod:** sem `spring.datasource.*`.
- Nenhum dialect explícito (`spring.jpa.database-platform` ausente): Hibernate 6.6.45 detecta pelo driver [FATO/INFERÊNCIA]. Nenhuma propriedade de pool/timeout/`transaction`.

### 4.2 `prod` sem datasource [INFERÊNCIA]
Como `com.h2database:h2` está no classpath (escopo `runtime`, não específico de teste) e não há URL, a auto-configuração do Boot tende a criar um **banco H2 embarcado em memória** com nome único; se nada externo (`SPRING_DATASOURCE_URL` etc.) for informado, a aplicação subiria com dados voláteis. Nesse caso o Flyway (habilitado por padrão) tentaria rodar as migrations padrão em `classpath:db/migration`, cuja varredura é recursiva e alcançaria `schema/` e `data/`, incluindo os usuários semeados com hash BCrypt versionado (V103). **[HIPÓTESE]** Não verificado por execução; também não se sabe se a implantação real fornece variáveis externas.

### 4.3 Segredos e valores default [FATO]
| Propriedade | Origem | Default no repositório |
|-------------|--------|------------------------|
| `security.client-secret` | `CLIENT_SECRET` | **sim** (14 caracteres, valor de exemplo) |
| `spring.mail.password` | `MAIL_PASSWORD` | **sim** (6 caracteres) |
| `spring.mail.username` | `MAIL_USERNAME` | sim (`test@gmail.com`) |
| `spring.datasource.password` (dev) | `POSTGRES_DATASOURCE_PASSWORD` | **não** |
| `spring.datasource.password` (test) | literal vazio | — |
Os defaults de segredo estão versionados (achado herdado, B-0). No ambiente de análise, `MAIL_USERNAME`, `MAIL_PASSWORD`, `POSTGRES_DATASOURCE_USER` e `POSTGRES_DATASOURCE_PASSWORD` **estão definidas**; as demais 11 variáveis da §17 **não** [FATO — só a existência foi verificada].

### 4.4 Flyway em `prod` [INFERÊNCIA]
Sem `spring.flyway.*`, valem os padrões (habilitado, `classpath:db/migration`, `validate-on-migrate=true`, `baseline-on-migrate=false`). O padrão recursivo incluiria as migrations de dados (V100+), embora o projeto tenha separado `schema/` e `data/` justamente para escolher o que roda por ambiente (só o perfil `dev` declara as duas *locations*). A decisão de rodar dados de demonstração em `prod` não está expressa em nenhum arquivo.

## 5. JPA / Hibernate [FATO / INFERÊNCIA]
- `spring.jpa.open-in-view=false` está em `application.properties`, sob o comentário "Datasource" (rótulo enganoso) e vale para **todos** os perfis. Consequência: não há `OpenEntityManagerInViewInterceptor`; o carregamento tardio fora de um método transacional falha (`LazyInitializationException`) [INFERÊNCIA]. Todos os DTOs de resposta são montados dentro dos services `@Transactional` (B-3/B-4).
- `ddl-auto`: `none` (dev); ausente em `test`/`prod`. Para banco embarcado sem gerenciador de schema, o Boot aplica `create-drop` (regra do Boot) [INFERÊNCIA forte]; com Flyway como gerenciador, aplica `none`.
- Hibernate executa o `import.sql` da raiz do classpath **somente** quando o schema é criado (create/create-drop): logo, ele roda no perfil `test`, e é **ignorado em `dev`** (`none`) [INFERÊNCIA].
- Nenhuma validação de schema (`validate`) em nenhum perfil: divergência entre entidades e migrations não seria detectada na subida [FATO].
- SQL: `show-sql=true` (imprime no *stdout*, sem valores) + `format_sql` em `dev`/`test`; redundante com `logging.level.org.hibernate.SQL=DEBUG` (duplicação de SQL no console/arquivo) [INFERÊNCIA].
- Entidades: `columnDefinition = "TIMESTAMP WITHOUT TIME ZONE"` em vários campos; IDENTITY em todas; enums `STRING`.

## 6. Flyway [FATO]

**Configuração:** apenas em `dev` (`enabled=true`; `locations=classpath:db/migration/schema,classpath:db/migration/data`; `logging.level.org.flywaydb=DEBUG`). `flyway-core` e `flyway-database-postgresql` 11.7.2 no classpath.

| Versão | Arquivo | Efeito |
|:------:|---------|--------|
| V001 | `create_table_category` | `tb_category` (`name` varchar 80 único, `active`, timestamps) |
| V002 | `create_table_product` | `tb_product` (`price float(53)`, `name` varchar 255 único, `description` TEXT) |
| V003 | `create_table_product_category` | PK composta `(category_id, product_id)` |
| V004 | `alter_table_product_category` | 2 FKs |
| V005 | `create_table_role` | `tb_role` |
| V006 | `create_table_user` | `tb_user` (`email` único, `password` varchar 255) |
| V007 | `create_table_user_role` | PK composta |
| V008 | `alter_table_user_role` | 2 FKs |
| V009 | `create_table_token` | `token` único; `type` varchar(18) com `check (ACTIVATION, PASSWORD_RECOVERY)` |
| V010 | `alter_table_token` | FK → `tb_user` |
| V011 | `create_table_email` | `status` varchar(10) com `check (ERROR, PENDING, SENT)`; `content` TEXT |
| V100 | `insert_categories` | 15 categorias |
| V101 | `insert_products` | 25 produtos (imagens em `raw.githubusercontent.com`) |
| V102 | `insert_product_category` | 26 vínculos com ids fixos |
| V103 | `insert_user` | 2 usuários (`albert@…`, `maria@…`) com o mesmo hash BCrypt |
| V104 | `insert_role` | `ROLE_OPERATOR`, `ROLE_ADMIN` |
| V105 | `insert_user_role` | 3 vínculos com ids fixos |

- Sequência **contínua** V001–V011 (schema) e V100–V105 (dados); os prefixos não colidem. Ordem global por versão: schema antes dos dados.
- Os dados dependem de **ids gerados por IDENTITY** (1, 2, …) — dependência implícita da ordem de inserção (V102/V105 usam ids literais).
- Alguns arquivos terminam sem quebra de linha final (a concatenação simples de V10x junta linhas; irrelevante ao Flyway).
- Sem `baseline`, `out-of-order`, `placeholders` ou `clean-disabled` configurados (padrões). Não há migration de *undo*.
- **Sem índices explícitos** além das PKs e das restrições `unique` [FATO].

### 6.1 Migrations × `import.sql` × `create.sql`
- **`import.sql` × V100–V105:** os 73 `INSERT` são **idênticos** em conteúdo (conferido por comparação ordenada; as únicas diferenças foram artefatos de linhas sem quebra final). Ordem diferente: `import.sql` insere usuários e roles primeiro. Duplicação de dados de referência em dois lugares (manutenção manual).
- **`create.sql` (raiz do repositório, fora de `src`) × migrations:** as tabelas, colunas, tamanhos, unicidades e nomes das 5 FKs são iguais. Divergência: `tb_email.status` e `tb_token.type` são `enum (...)` no `create.sql` (sintaxe de DDL gerada por Hibernate para H2) e `varchar` + `check` nas migrations (compatível com PostgreSQL). O `create.sql` é o DDL exportado (as propriedades de geração de script estão comentadas em `application-test.properties`); as migrations são a versão adaptada para PostgreSQL [INFERÊNCIA]. O `create.sql` não é usado por nenhum código ou configuração [FATO].
- **Migrations × entidades:** nomes de tabela/colunas e tamanhos conferem com as anotações (`Category.name` 80; demais textos 255) — **[HIPÓTESE]** de aderência completa só verificável executando o Flyway contra PostgreSQL com `ddl-auto=validate` (não configurado).

## 7. H2 [FATO / INFERÊNCIA]
- Só o perfil `test` declara `spring.h2.console.enabled=true` (`/h2-console`). Como `test` é o **perfil padrão**, executar sem `APP_PROFILE` expõe o console H2 na porta 8080; a cadeia `@Order(1)` (`h2SecurityFilterChain`, B-6) só é criada quando a propriedade é `true`, e ela desabilita CSRF e `X-Frame-Options` e não declara regra de autorização. O login do console usa as credenciais do datasource (`sa`, senha vazia).
- `spring-boot-devtools` (dependência `runtime` opcional) traz o padrão `spring.h2.console.enabled=true` entre as propriedades de desenvolvimento (arquivo `devtools-property-defaults.properties` do jar) **[FATO]**; quando o devtools está ativo (execução por IDE/`spring-boot:run`), a condição da cadeia H2 poderia ser satisfeita também em `dev` **[INFERÊNCIA/HIPÓTESE]**. O plugin do Spring Boot exclui o devtools do JAR repackaged por padrão (`excludeDevtools=true`) e o devtools se desativa em testes [FATO/INFERÊNCIA].
- Dependência `h2` não está no escopo `test`: vai para o classpath de runtime em todos os perfis, incluindo `prod` (ver §4.2).

## 8. Async [FATO / INFERÊNCIA / HIPÓTESE]
- **[FATO]** Há dois métodos `@Async` públicos em `EmailService` (`sendActivationEmailAsync`, `sendPasswordRecoveryEmailAsync`), chamados por `AccountService` (outro bean), com retorno `CompletableFuture<Void>` descartado.
- **[FATO]** Nenhuma classe usa `@EnableAsync`, `AsyncConfigurer` ou define `Executor`/`TaskExecutor` (busca em `src/main`: 0). Spring Boot auto-configura o `applicationTaskExecutor` (`TaskExecutionAutoConfiguration`) mas isso não habilita `@Async`: a anotação só é processada com `@EnableAsync` **[INFERÊNCIA forte — regra do Spring Framework]**. Não há `spring.task.execution.*`.
- **[INFERÊNCIA forte]** Sem `@EnableAsync` a anotação é ignorada: os métodos rodam **na thread da requisição**, dentro da transação de `AccountService`, com o envio SMTP bloqueando a resposta e ocorrendo antes do *commit*. As exceções são capturadas em `EmailService` (`failedFuture` descartado) — falha de envio é apenas registrada em log.
- **[NÃO GARANTIDO pela configuração]** o comportamento "assíncrono" sugerido pelo nome e pelo JavaDoc dos métodos; **[HIPÓTESE]** confirmação por execução (nome da thread no log durante `POST /accounts/register`).
- Sem propagação de contexto de segurança/transação para outra thread (não há executor com decoradores); irrelevante enquanto executa na mesma thread.
- Refina B-0 P1-4 / B-3 §9.2 (permanecem confirmados).

## 9. Transações [FATO / INFERÊNCIA]
- Sem `PlatformTransactionManager` declarado: Boot cria um `JpaTransactionManager` único a partir do `EntityManagerFactory`. `@Transactional` é ativado pela auto-configuração (`TransactionAutoConfiguration`), com proxies de classe (CGLIB, padrão do Boot) [INFERÊNCIA forte].
- Uso: 31 métodos anotados em 4 services (`AccountService` 8, `CategoryService` 7, `ProductService` 8, `UserService` 8) mais `@Transactional` no nível de classe em `TokenService`; `readOnly = true` nas leituras. **`EmailService` não é transacional.** Controllers e repositories próprios não declaram transação; os repositórios Spring Data têm transação interna (leitura para consultas) [INFERÊNCIA].
- Propagação/rollback: padrões (`REQUIRED`; rollback só em exceções não verificadas; `DataIntegrityViolationException` do commit ocorre na saída do método) — ver B-3.
- Sem timeout, isolamento ou `TransactionTemplate` customizado.
- Com `open-in-view=false` a fronteira transacional é o service (§5).

## 10. Mail [FATO / INFERÊNCIA / HIPÓTESE]
- Propriedades (todas em `application.properties`, valem em todos os perfis): `host=${MAIL_HOST:smtp.gmail.com}`, `port=${MAIL_PORT:587}`, `username=${MAIL_USERNAME:…}`, `password=${MAIL_PASSWORD:…}`, `smtp.auth=true`, `starttls.enable=true`, **`ssl.trust=smtp.gmail.com` (fixo)**, **`spring.mail.test-connection=true`**.
- **`test-connection` (achado novo, bytecode do Spring Boot 3.5.13):** `MailSenderValidatorAutoConfiguration` é condicionada a `spring.mail.test-connection` e a haver um único `JavaMailSenderImpl`; ao final da inicialização chama `testConnection()` e, se falhar, lança `IllegalStateException("Mail server is not available")` **[FATO]**. **[INFERÊNCIA]**: cada subida do contexto (aplicação em qualquer perfil e todo `@SpringBootTest`) abre conexão com o servidor SMTP configurado e **falha** se ele não estiver acessível ou recusar a autenticação. Os defaults do repositório (`test@gmail.com` + senha de exemplo) seriam recusados pelo Gmail; o baseline passou (B-0) provavelmente porque `MAIL_USERNAME`/`MAIL_PASSWORD` estão definidas no ambiente local **[HIPÓTESE — o teste de conexão não foi observado]**. Implicação para a fase de testes: os testes de contexto dependem de rede e de credencial reais.
- Remetente (`From`) fixo no código: `nao-responder@asjcatalog.com.br`; o registro em `tb_email` usa remetente fixo `asjcatalog@gmail.com`, e `content` guarda apenas o assunto (não o HTML nem o token) [FATO]. `spring.mail.username` não influi no `From` do cabeçalho (o Gmail pode reescrevê-lo) [INFERÊNCIA].
- Encoding: `MimeMessageHelper(..., "UTF-8")`; `spring.mail.default-encoding` (padrão UTF-8) [FATO].
- `backend.url` é injetado em `EmailService` e **não é usado**; `frontend.url` (padrão `http://localhost:5173`) compõe os links `/activate-account?token=` e `/reset-password?token=`.
- Envio síncrono na prática (§8); nenhum e-mail foi enviado nesta fase.

## 11. Thymeleaf e templates [FATO]
Sem `spring.thymeleaf.*`: padrões do Boot (`classpath:/templates/`, `.html`, modo HTML, UTF-8, cache habilitado; devtools desliga o cache) [INFERÊNCIA]. O `SpringTemplateEngine` é injetado em `EmailService`.

| Template | Consumidor | Variáveis usadas no HTML | Variáveis fornecidas pelo Java |
|----------|-----------|--------------------------|--------------------------------|
| `activate_user_by_email_template` | `EmailService.sendActivationEmail` | `nome`, `texto`, `linkConfirmacao` | `nome`, `titulo`, `texto`, `linkConfirmacao` |
| `reset_password_email_template` | `EmailService.sendPasswordRecoveryEmail` | `nome`, `texto`, `linkRedefinicaoSenha` | `nome`, `token`, `titulo`, `linkRedefinicaoSenha` |
| `reactivate_user_by_email_template` | **nenhum** | `nome`, `texto`, `linkConfirmacao` | — (sem consumidor) |

- **Achados (fato):** o template de redefinição usa `${texto}`, que o Java **não** fornece; `titulo` (ambos) e `token` (redefinição) são fornecidos e **não** são referenciados por nenhum template; `reactivate_*` não tem consumidor; o logo inline usa `cid:logo` com o arquivo `.ico` (o `.png` em `static/image` não é usado por código).
- **[INFERÊNCIA]** com `th:text="${texto}"` nulo, o texto de exemplo do protótipo é substituído por vazio, e o e-mail de recuperação sai sem o parágrafo de instrução.
- Os templates são quase idênticos (CSS/estrutura duplicados).

## 12. Logging [FATO / INFERÊNCIA]

| Fonte | Perfis | Conteúdo potencialmente sensível |
|-------|--------|----------------------------------|
| `UserController` (DEBUG do DTO inteiro) | `dev`, `test` (`com.albertsilva.dev.dscatalog=DEBUG`) | **senha em texto** nos `record` de criação/atualização de usuário (B-7) |
| `org.hibernate.SQL=DEBUG` + `show-sql=true` | `dev`, `test` | SQL com `?` (sem valores) |
| `org.hibernate.orm.jdbc.bind=TRACE` | **`test`** | valores dos parâmetros: hash de senha, e-mails, **valores de token de ativação/recuperação** e demais colunas |
| `org.hibernate.type.descriptor.sql.BasicBinder=TRACE` | `dev` | **ineficaz** (ver abaixo) |
| `org.springframework.web=DEBUG`; `…web.servlet.mvc.method.annotation=TRACE` | `test` | leitura/escrita de `@RequestBody`/resposta: em TRACE os objetos aparecem nos logs (DTOs com senha) [INFERÊNCIA — comportamento do Spring MVC] |
| stack traces do `ControllerExceptionHandler` | todos | texto de exceções/SQL, no arquivo |
| `logging.file.name` | todos | arquivos em `logs/…` relativos ao diretório de execução; `*.log` está no `.gitignore` |

- **Refino do achado de bind de SQL (B-7 §19):** no Hibernate 6.6.45 (bytecode: `BasicBinder` está em `org.hibernate.type.descriptor.jdbc`, e `JdbcBindingLogging` usa o logger `org.hibernate.orm.jdbc.bind`); a classe `org.hibernate.type.descriptor.sql.BasicBinder` **não existe** [FATO]. Logo, o nível `TRACE` de `dev` (e o `OFF` de `prod`) aponta para um nome de logger que o Hibernate 6 não emite [INFERÊNCIA forte]: em `dev` os parâmetros provavelmente **não** são registrados; em `test` são (nome correto). O padrão em `prod` (WARN) também não emite.
- Rotação: `logging.logback.rollingpolicy.max-file-size` e `max-history` definidos; sem `total-size-cap` nem `clean-history-on-start`.
- Nível raiz `WARN` nos três perfis; `dev` mantém `org.springframework=INFO`.
- devtools (quando ativo): `spring.mvc.log-resolved-exception=true` e `server.error.include-{message,stacktrace,binding-errors}=always` como padrões (afetam apenas o fluxo `/error` do Boot, pois a aplicação usa seu próprio `@RestControllerAdvice`) [FATO do jar / INFERÊNCIA do efeito].
- Combinado ao achado de B-7: em `dev`/`test` a senha em texto pode aparecer nos logs via controller; em `test` também via `bind` e via TRACE do MVC.

## 13. Springdoc / OpenAPI [FATO / INFERÊNCIA]
- Dependência `springdoc-openapi-starter-webmvc-ui` 2.8.16 (versão explícita, fora do BOM do Boot); Swagger UI 5.32.0.
- Beans: `SpringDocOpenApiConfig.openAPI()` (título "ASJCatalog API", versão "v1", esquema HTTP bearer `security`, **sem** `servers`, sem `tags` globais).
- Propriedades por perfil (`springdoc.swagger-ui.path`, `springdoc.api-docs.path`, `springdoc.packagesToScan`) existem em `dev` e `test`; em `prod` não há nenhuma. Não há `springdoc.api-docs.enabled=false` nem `springdoc.swagger-ui.enabled=false` em nenhum perfil, então o springdoc está **ativo em todos**, inclusive `prod` [FATO].
- Disponibilidade e segurança (`ResourceServerConfig.DOCUMENTATION_OPENAPI` = `/docs-asjcatalog`, `/docs-asjcatalog/**`, `/docs-asjcatalog.html`, `/swagger-ui/**`): `test` funcional; `dev` — UI carrega (`/swagger-ui/**` liberado) mas `/docs-dscatalog` e `/docs-dscatalog.html` **não** estão liberados; `prod` — `/swagger-ui/**` liberado, JSON padrão (`/v3/api-docs`) protegido [INFERÊNCIA].
- **Metadados divergentes** [FATO]: licença **Apache 2.0** (OpenAPI) × **MIT** (`pom.xml`); e-mail de contato: OpenAPI `albertinesilva,17@gmail.com` (vírgula) × `pom.xml` `albertinesilva@.17gmail.com` (ponto deslocado) — **ambos malformados e diferentes entre si**; `pom.xml` organização "Personal Project".
- Inconsistências de contrato já documentadas em B-7 §16 (GET de catálogo marcado como protegido, `PATCH /me/password` sem OpenAPI, `deactivate` 204×500, etc.) — não repetidas.
- Nome do aplicativo: `spring.application.name=DSCatalog` (dev/test) × artefato `asjcatalog` × título "ASJCatalog API" × caminho `/docs-dscatalog` em `dev` (vestígios do nome anterior) [FATO].

## 14. Jackson / MVC / locale / encoding [FATO / INFERÊNCIA]
- **Sem configuração explícita** de Jackson: nenhum `ObjectMapper`/`Jackson2ObjectMapperBuilder`, módulo, `@JsonFormat`, `@JsonInclude`, estratégia de nomes; única propriedade `spring.jackson.*` é `serialization.indent_output=true` **apenas em `test`**. Valem os padrões do Boot (Jackson 2.21.2): datas como texto ISO-8601 (`WRITE_DATES_AS_TIMESTAMPS` desligado), **campos desconhecidos no JSON ignorados** (`FAIL_ON_UNKNOWN_PROPERTIES` desligado) [INFERÊNCIA — padrões do Boot]. Consequência: corpo com campo desconhecido/typo é aceito silenciosamente.
- **Sem configuração MVC explícita**: nenhum `WebMvcConfigurer`, conversor, `Formatter` ou interceptor; `spring.mvc.*` ausente. Recursos estáticos padrão (`classpath:/static/**`) — `static/image/*` é servido em `/image/**` e, na cadeia `@Order(3)` (`anyRequest().authenticated()`), exige autenticação [INFERÊNCIA].
- **Locale (achado novo):** `MessageSourceConfig` declara um bean `localeResolver` (`AcceptHeaderLocaleResolver`, padrão `pt_BR`). A auto-configuração do MVC só cria o seu `localeResolver` se **não** existir bean com esse nome; portanto `spring.web.locale-resolver=fixed` e `spring.web.locale=pt_BR` de `application-dev.properties` **não têm efeito** sobre o resolvedor efetivo [INFERÊNCIA forte]. O idioma vem do `Accept-Language` em todos os perfis, com fallback `pt_BR`.
- **`MessageSource`** (`ReloadableResourceBundleMessageSource`): basename `classpath:messages`, UTF-8, `fallbackToSystemLocale=false`, padrão `pt_BR`; 3 arquivos com 70 chaves cada; **não há `messages.properties` sem sufixo**. O bean chamado `messageSource` substitui o da auto-configuração. Divergência herdada: a chave `error.auth.userId.claim.notFound` só existe em `pt_BR` (en/es têm `error.auth.username.claim.notFound`) [FATO]; para pedidos em en/es a mensagem cai para o texto em português por fallback [INFERÊNCIA]. Por busca textual, apenas 1 chave não é referenciada pelo código Java (`error.auth.authentication.notFound`) [FATO]. Sem `cacheSeconds` (cache permanente do resource bundle).
- **Encoding:** `server.servlet.encoding.*` só em `dev` (UTF-8 é o padrão do Boot); `project.build.sourceEncoding=UTF-8`; mail e `messages` em UTF-8.

## 15. Serialização de `Page` [FATO / INFERÊNCIA]
Os endpoints de listagem retornam `Page<…>` (um `PageImpl` construído por `map`). Não há `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` nem `PagedModel`. No Spring Data Commons 3.5.10, `SpringDataJacksonConfiguration.PageModule` registra um modificador que **emite um aviso** ("Serializing PageImpl instances as-is is not supported, meaning that there is no guarantee about the stability of the resulting JSON structure!") quando um `PageImpl` é serializado [FATO — texto do bytecode]. **[INFERÊNCIA]** a resposta continua serializada como `content`, `pageable`, `totalElements`, `totalPages`, `size`, `number`, `sort`, `first`, `last`, `numberOfElements`, `empty`, e o aviso aparece nos logs (nível `WARN`, visível em todos os perfis) na primeira serialização; **[HIPÓTESE]** estrutura exata e momento do aviso. O Springdoc descreve o schema de `Page` via reflexão, sem configuração própria. `Pageable`: padrões (`page`, `size` = 20, `sort`, máximo 2000), sem `spring.data.web.*`.

## 16. Resources, templates e mensagens [FATO]

| Recurso | Tamanho/observação | Consumidor |
|---------|--------------------|------------|
| `application*.properties` (4) | ver §3 | Spring |
| `META-INF/additional-spring-configuration-metadata.json` | descreve 4 propriedades (`security.client-id`, `security.client-secret`, `security.jwt.duration` como `Long`, `cors.origins`); o código lê `security.jwt.duration` como `Integer`; ignora `frontend.url`, `backend.url`, `account.*` | somente IDE |
| `banner-dev.txt` | 16 linhas | `dev` e `test` (`spring.banner.location`) |
| `db/migration/schema` (11) e `data` (6) | §6 | Flyway (`dev`) |
| `import.sql` | 73 inserts | Hibernate no `test` |
| `templates/*.html` (3) | §11 | `EmailService` (2 de 3) |
| `messages_{pt_BR,en,es}.properties` | 70 chaves | `MessageSource` |
| `static/image/logo-ASJ-Catalog-favicon.{ico,png}` | `.ico` usado como logo inline do e-mail; `.png` sem consumidor | `EmailService` |
| `create.sql` (raiz do repositório) | DDL exportado | nenhum |

Sem `src/test/resources`: os testes usam os arquivos de `src/main/resources` [FATO, B-0].

## 17. Variáveis de ambiente e configuração externa [FATO]

| Variável | Propriedade | Default | Consumo |
|----------|-------------|---------|---------|
| `APP_PROFILE` | `spring.profiles.active` | `test` | Spring |
| `CLIENT_ID` / `CLIENT_SECRET` | `security.client-id` / `client-secret` | sim / sim | `AuthorizationServerConfig` |
| `JWT_DURATION` | `security.jwt.duration` | `86400` s (24 h) | `AuthorizationServerConfig` (`Integer`) |
| `CORS_ORIGINS` | `cors.origins` | `http://localhost:3000,http://localhost:5173` | `ResourceServerConfig` (`split(",")`, **sem `trim`**) |
| `BACKEND_URL` | `backend.url` | `http://localhost:8080` | injetado e não usado |
| `FRONTEND_URL` | `frontend.url` | `http://localhost:5173` | links dos e-mails |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | `spring.mail.*` | sim (Gmail, 587) | `JavaMailSender` |
| `ACTIVATION_TOKEN_HOURS` | `account.activation.token.hours` | `24` | `TokenService` |
| `PASSWORD_RECOVER_TOKEN_MINUTES` | `account.password-recovery.token.minutes` | sim | `TokenService` |
| `POSTGRES_DATASOURCE_USER` / `…_PASSWORD` | `spring.datasource.*` (**só `dev`**) | **não** | Spring |

Nenhuma propriedade é lida por `Environment`/`System.getenv` no código; só via `@Value` e auto-configuração. Não há `.env`, Docker/Compose, `application.yml` ou `bootstrap.*` [FATO]. A ausência de `trim` em `cors.origins` significa que uma lista com espaço após a vírgula geraria um padrão com espaço (que não casaria a origem) [INFERÊNCIA].

## 18. Dependências de infraestrutura [FATO]
Spring Boot 3.5.13 (parent), Java 17 (`release 17`, `-parameters` ativo no `maven-compiler-plugin` 3.14.1), Hibernate 6.6.45, Spring Data Commons 3.5.10, Spring Security OAuth2 Authorization Server 1.5.6 / Resource Server 6.5.9, Flyway 11.7.2, PostgreSQL 42.7.10, H2 2.3.232 (**runtime**, todos os perfis), Thymeleaf 3.1.3, Jakarta Mail (Angus), Springdoc 2.8.16, Jackson 2.21.2, devtools 3.5.13 (`runtime`, `optional`), `maven-javadoc-plugin` 3.6.3 (`failOnError=false`, não vinculado ao build). Sem JaCoCo, sem Actuator (não há endpoint de saúde/métricas), sem cache, sem métricas.
`-parameters` é o que permite `#id` no SpEL de `@PreAuthorize` (B-7).

## 19. Achados confirmados (fatos)
1. Perfil padrão `test` (H2 + console + logs DEBUG/TRACE) quando `APP_PROFILE` não é informado.
2. `prod` só define logging; sem datasource/JPA/Flyway.
3. `spring.mail.test-connection=true` em todos os perfis; o Boot lança `IllegalStateException` se o SMTP falhar.
4. `spring.jpa.open-in-view=false` em todos os perfis.
5. Flyway habilitado só em `dev`; migrations V001–V011 + V100–V105 contínuas; `import.sql` equivale aos V100–V105.
6. Sem `@EnableAsync`, executor, `@EnableTransactionManagement`, config de Jackson/MVC/Page.
7. Template de redefinição usa `${texto}` não fornecido; `titulo`/`token` fornecidos e não usados; `reactivate_*` sem consumidor; `.png` sem uso.
8. `backend.url` injetado e não usado.
9. Metadados divergentes (licença; e-mail de contato malformado nos dois arquivos, de formas diferentes).
10. Logger de bind do `dev` (`…descriptor.sql.BasicBinder`) não existe no Hibernate 6.6.45; o de `test` (`org.hibernate.orm.jdbc.bind`) é o efetivo.
11. `create.sql` difere das migrations somente em `enum` × `varchar`+`check`.
12. `spring.web.locale-resolver`/`locale` em `dev` são sobrepostos pelo bean `localeResolver`.
13. Springdoc habilitado em todos os perfis; caminhos liberados só para o perfil `test`.
14. Sem índices explícitos e sem `ddl-auto=validate`.
15. `H2` em `runtime` em todos os perfis; devtools traz `spring.h2.console.enabled=true` como padrão.

## 20. Inferências
Subida em `prod` sem variáveis externas usa H2 embarcado e Flyway padrão (§4.2); `@Async` executa de forma síncrona (§8); `test-connection` falharia sem SMTP/credencial válidos (§10); `create-drop` no perfil `test`; `import.sql` ignorado em `dev`; bind de SQL ineficaz em `dev`; aviso de `PageImpl` na serialização; campos JSON desconhecidos ignorados; e-mail de recuperação sem parágrafo; `static/image/**` exige autenticação; fallback de mensagens en/es para português; devtools pode habilitar o console H2 em `dev`.

## 21. Hipóteses (dependem de execução)
Efeito real de `test-connection` sem credenciais/rede; comportamento de `prod` sem variáveis (H2 embarcado + Flyway com dados semeados); nome da thread do envio de e-mail; conteúdo e momento do aviso de `Page`; aderência migrations × entidades (`ddl-auto=validate`); ativação do console H2 em `dev` com devtools; preflight CORS com cabeçalhos extras (B-7); valores realmente registrados em `logs/` em cada perfil.

## 22. Pontos para B-9 (revisão arquitetural)
- Estratégia de perfis: `test` como padrão e `prod` incompleto; onde vivem as configurações de produção (variáveis externas × arquivo).
- Coerência de segredos/defaults versionados; política de logs (senha em DEBUG, bind, TRACE do MVC).
- Dependência de SMTP para subir o contexto (acoplamento infraestrutura → ciclo de vida da aplicação).
- Async: intenção × configuração; e-mail dentro de transação; envio antes do commit.
- Fonte única para dados semeados (`import.sql` × V100–V105 × `create.sql`); `create.sql` na raiz sem uso.
- Contratos de identidade (`userId` × `jti` × `sub`) — tema próprio, já mapeado em B-6/B-7.
- OpenAPI por perfil, metadados (licença/contato), nome do produto (DSCatalog × ASJCatalog).
- Serialização de `Page` estável (DTO/`PagedModel`), política de campos JSON desconhecidos, locale/mensagens en/es.
- Observabilidade ausente (Actuator/health) e ausência de `@ConfigurationProperties`.

## 23. Pontos para a futura fase de testes
Subida do contexto por perfil (`test`, `dev`+PostgreSQL/Testcontainers, `prod`); efeito de `test-connection` e isolamento de e-mail (mock de `JavaMailSender`/`spring.mail.test-connection=false` por teste — **decisão da fase de testes**); Flyway sobre PostgreSQL e `ddl-auto=validate`; equivalência `import.sql` × migrations; `@Async` (thread) e comportamento transacional do envio; render dos templates (variáveis ausentes, `reactivate`); Springdoc por perfil (caminhos públicos/protegidos); serialização de `Page` e aviso; locale (`Accept-Language` en/es, chave `userId`); logging (senha/token em `test`); CORS (lista com espaços, preflight); H2 console por perfil; `TokenService` com propriedades ausentes.

## 24. JavaDoc adicionado
| Arquivo | Alteração |
|---------|-----------|
| `DscatalogApplication` | Class doc (varredura, perfil padrão, recursos não habilitados) e doc de `main`; era a única classe de infraestrutura sem JavaDoc. |
| `SpringDocOpenApiConfig`, `MessageSourceConfig`, `SecurityBeansConfig`, `AuthorizationServerConfig`, `ResourceServerConfig` | Já documentados em B-6/B-7; conferidos e **não alterados** nesta fase. |

## 25. O que deliberadamente não foi feito
Nenhuma alteração em `src/test/**`, `pom.xml`, `src/main/resources/**`, migrations, templates, propriedades, logging, CORS, OpenAPI, dependências ou comportamento; nenhum teste, aplicação ou e-mail executado; nenhum commit. Riscos listados acima são **registrados**, não corrigidos.

## 26. Revisão dos achados herdados (B-0 a B-7)

| Achado | Situação | Evidência nova |
|--------|----------|----------------|
| `@Async` sem `@EnableAsync` | **Confirmado** (ausência); execução síncrona = inferência forte | §8 |
| `open-in-view` | **Confirmado**: `false` em todos os perfis | §5 |
| Bind de SQL em logs | **Parcialmente refutado/refinado**: efetivo só em `test`; nome de logger de `dev`/`prod` não existe no Hibernate 6.6 | §12 |
| Flyway | **Confirmado**: só `dev`; `prod` = padrão do Boot (inferência) | §4.4/§6 |
| Springdoc por perfil | **Confirmado**; `prod` sem propriedades | §13 |
| H2 | **Confirmado** e ampliado: console habilitado no perfil padrão; devtools | §7 |
| Configuração de produção | **Confirmado**: incompleta; H2 embarcado é inferência | §3/§4.2 |
| CORS | **Confirmado**: única origem; lista por env sem `trim` (novo) | §17 |
| Serialização de `Page` | **Confirmado**: sem config; aviso da biblioteca | §15 |
| Propriedades JWT | **Confirmado**; `security.jwt.duration` `Integer` × metadados `Long` | §16 |
| Datasource | **Confirmado**: `dev` por env sem default; `test` H2 | §4 |
| Logging (senha) | **Confirmado** e ampliado (TRACE do MVC em `test`) | §12 |
| `test-connection` (B-0: hipótese) | **Confirmado no bytecode**; efeito real = hipótese | §10 |
| Metadados pom × OpenAPI | **Confirmado** e detalhado (contato malformado nos dois) | §13 |
| Template `reactivate` sem uso | **Confirmado** (+ `texto` ausente na redefinição, novo) | §11 |
