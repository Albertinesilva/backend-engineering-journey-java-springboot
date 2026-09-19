# ASJCatalog Backend — Inventário B-0

> **Fase:** B-0 — Baseline e Inventário (somente leitura, análise e registro).
> **Data do baseline:** 2026-09-19.
> **Branch:** `chapter-04-domain-orm`.
> **Commit de referência (HEAD no fechamento):** `ecf252a` — `feat: camada de test` (ver seção 13, nota sobre o commit).

**Convenções de rotulagem usadas neste documento**

| Rótulo | Significado |
|--------|-------------|
| **[FATO]** | Observado diretamente no código, na configuração ou na saída de um comando executado. |
| **[INFERÊNCIA]** | Conclusão razoável derivada de fatos, mas não executada/confirmada. |
| **[HIPÓTESE]** | Suspeita que precisa de verificação em fase posterior. |
| **[CONFIRMADO]** | Problema reproduzido/evidenciado por execução ou leitura inequívoca. |

Quando algo não pôde ser determinado com segurança, está escrito: *"Não foi possível determinar com segurança a partir da implementação atual."*

---

## 1. Objetivo

Registrar uma fotografia factual do backend do ASJCatalog para que as próximas fases possam documentar, com JavaDoc em português, as classes e os métodos de produção. Esta fase **não** altera código de produção, código de testes, configuração, dependências ou comportamento, **não** adiciona JavaDoc e **não** cria commits. O único artefato criado é este documento.

Escopo lido: `pom.xml`, `src/main/java` (94 arquivos), `src/main/resources` (propriedades, migrations Flyway, templates, mensagens, `import.sql`), `src/test/java` (48 arquivos), `create.sql`, `HELP.md`, `.gitignore`, `.github/`, `.vscode/`, `.mvn/`. O diretório `src/test/resources` **não existe** [FATO].

---

## 2. Stack tecnológica

### 2.1 Baseline de versões [FATO]

| Item | Valor | Fonte |
|------|-------|-------|
| Java (alvo de compilação) | 17 (`java.version=17`, `maven-compiler-plugin` com `<release>17</release>` e `-parameters`) | `pom.xml` |
| Java (JDK usado na execução) | Oracle JDK 17.0.1 LTS | `mvn -v` |
| Maven | 3.8.4 (comando `mvn`; o `mvnw` existe, mas `.mvn/wrapper/maven-wrapper.jar` está no `.gitignore`) | `mvn -v` |
| Spring Boot | 3.5.13 (`spring-boot-starter-parent`) | `pom.xml` |
| Spring Framework | 6.2.17 | `mvn dependency:list` |
| Spring Security | 6.5.9 | `mvn dependency:list` |
| Spring Authorization Server | 1.5.6 | `mvn dependency:list` |
| Spring Data JPA | 3.5.10 | `mvn dependency:list` |
| Hibernate ORM | 6.6.45.Final | `mvn dependency:list` |
| Hibernate Validator / Jakarta Validation | 8.0.3.Final / 3.0.2 | `mvn dependency:list` |
| Nimbus JOSE + JWT | 9.47 | `mvn dependency:list` |
| Jackson Databind | 2.21.2 | `mvn dependency:list` |
| Tomcat embed | 10.1.53 | `mvn dependency:list` |
| Thymeleaf | 3.1.3.RELEASE | `mvn dependency:list` |
| Jakarta Mail (Angus) | 2.0.5 | `mvn dependency:list` |
| Flyway | 11.7.2 (`flyway-core` + `flyway-database-postgresql`) | `mvn dependency:list` |
| PostgreSQL JDBC | 42.7.10 (`runtime`) | `mvn dependency:list` |
| H2 | 2.3.232 (`runtime`) | `mvn dependency:list` |
| springdoc-openapi (webmvc-ui) | 2.8.16 (versão fixada no `pom.xml`) | `pom.xml` |
| JUnit Jupiter | 5.12.2 | `mvn dependency:list` |
| Mockito | 5.17.0 | `mvn dependency:list` |
| AssertJ | 3.27.7 | `mvn dependency:list` |

### 2.2 Principais dependências declaradas no `pom.xml` [FATO]

- Starters: `web`, `data-jpa`, `validation`, `thymeleaf`, `mail`, `security`, `oauth2-resource-server`; mais `spring-security-oauth2-authorization-server`.
- Banco: PostgreSQL e H2 (ambos `runtime`); Flyway.
- Documentação de API: `springdoc-openapi-starter-webmvc-ui`.
- Ferramentas: `spring-boot-devtools` (`runtime`, `optional`).
- Testes: `spring-boot-starter-test`, `spring-security-test`. **Não há Testcontainers** nem Failsafe no `pom.xml`.
- Plugins: `spring-boot-maven-plugin`, `maven-compiler-plugin 3.14.1`, `maven-javadoc-plugin 3.6.3` (`failOnError=false`, `source=17`). O `maven-surefire-plugin` usado é o padrão do parent (3.5.5, conforme saída do build).

### 2.3 Metadados do projeto [FATO]

`groupId=com.albertsilva.dev`, `artifactId=asjcatalog`, versão `0.0.1-SNAPSHOT`, nome "ASJCatalog Backend", licença declarada no `pom.xml`: MIT. Pacote raiz: `com.albertsilva.dev.dscatalog`; classe principal `DscatalogApplication` (`@SpringBootApplication`, sem outras anotações).

---

## 3. Estrutura de pacotes

Todos os pacotes abaixo estão sob `com.albertsilva.dev.dscatalog` [FATO — estrutura real encontrada]. Contagem de arquivos de produção: **94**.

| Pacote | Arquivos | Responsabilidade aparente | Principais classes | Depende de | Consumido por |
|--------|:-------:|---------------------------|--------------------|------------|---------------|
| (raiz) | 1 | Ponto de entrada da aplicação. | `DscatalogApplication` | — | — |
| `config.documentation` | 1 | Metadados OpenAPI e esquema de segurança `Bearer JWT` (nome `security`). | `SpringDocOpenApiConfig` | springdoc | Controllers (via `@SecurityRequirement(name="security")`) |
| `config.i18n` | 1 | `MessageSource` (`classpath:messages`, UTF-8, locale padrão `pt_BR`) e `AcceptHeaderLocaleResolver`. | `MessageSourceConfig` | Spring | `ControllerExceptionHandler`, Bean Validation |
| `domain` | 1 | Contrato de identificador genérico. | `Identifiable<ID>` | — | `Product`, `ProductProjection`, `IdentifiableUtils` |
| `domain.catalog` | 2 | Entidades do catálogo. | `Category`, `Product` | JPA | repositories, mappers, services |
| `domain.user` | 2 | Entidades de usuário/papel integradas ao Spring Security. | `User` (`UserDetails`), `Role` (`GrantedAuthority`) | JPA, Spring Security | repositories, services, security |
| `domain.recovery` | 2 (+2 enums) | Tokens de ativação/recuperação e registro de e-mails enviados. | `Token`, `Email`; enums `TokenType`, `EmailStatus` | `User`, `EmailRegisterRequest`, `InvalidTokenException` | `TokenService`, `EmailService`, `AccountService` |
| `dto.category` / `dto.product` / `dto.user` / `dto.role` / `dto.email` | 19 | *Records* de entrada (com Bean Validation) e de saída. | ver 5.2 | annotations de `validation` | controllers, services, mappers |
| `mapper.category` / `product` / `user` | 3 | Conversão manual DTO ↔ entidade (`@Component`, sem MapStruct). | `CategoryMapper`, `ProductMapper`, `UserMapper` | `dto`, `domain` | services |
| `projection` | 2 | Projeções de interface para queries nativas. | `ProductProjection`, `UserDetailsProjection` | `Identifiable` | repositories, services |
| `repository` | 6 | Repositórios Spring Data JPA. | `CategoryRepository`, `ProductRepository`, `UserRepository`, `RoleRepository`, `TokenRepository`, `EmailRepository` | `domain`, `projection` | services, validators, `AuthenticatedUserService` |
| `service` | 6 | Regras de negócio e orquestração transacional. | `UserService`, `AccountService`, `CategoryService`, `ProductService`, `TokenService`, `EmailService` | repositories, mappers, `security.auth` | controllers, `AuthorizationServerConfig` (`UserService` como `UserDetailsService`) |
| `service.exception` | 5 | Exceções de negócio (`RuntimeException`). | `ResourceNotFoundException`, `DatabaseException`, `InvalidTokenException`, `PasswordUpdateException`, `AuthenticatedUserNotFoundException` | — | services, `Token`, `ControllerExceptionHandler` |
| `util` | 1 | Utilitário de reordenação por referência. | `IdentifiableUtils` | `Identifiable` | `ProductService` |
| `validation.category` / `product` / `role` / `user` | 26 | Annotations de restrição, validadores e contrato. | ver 5.6 | repositories, `AuthenticatedUserService`, `HttpServletRequest` | DTOs |
| `security.config` | 1 | Bean `PasswordEncoder` (BCrypt). | `SecurityBeansConfig` | — | `UserService`, `AccountService`, `AuthorizationServerConfig` |
| `security.auth` | 1 | Recupera o usuário autenticado a partir do `Jwt` no `SecurityContext`. | `AuthenticatedUserService` | `UserRepository` | `AccountService`, validador, SpEL em `UserController` |
| `security.userdetails` | 1 | Objeto leve (id, username, authorities) pendurado nos `details` do cliente autenticado. | `AuthenticatedUser` | — | provider `password`, `tokenCustomizer` |
| `security.oauth2.authorization.config` | 1 | Authorization Server: cliente, tokens, JWK, customização de claims. | `AuthorizationServerConfig` | `UserDetailsService`, `PasswordEncoder` | infraestrutura Spring |
| `security.oauth2.grant.password` | 3 | *Grant type* customizado `password`. | `CustomPasswordAuthenticationConverter`, `CustomPasswordAuthenticationProvider`, `CustomPasswordAuthenticationToken` | `UserDetailsService`, `OAuth2AuthorizationService`, `OAuth2TokenGenerator` | `AuthorizationServerConfig` |
| `security.oauth2.resource.config` | 1 | Resource Server: filter chains, CORS, conversor JWT → authorities. | `ResourceServerConfig` | Spring Security | infraestrutura Spring |
| `web.controller` | 4 | Endpoints REST (`/api/v1/...`) e anotações OpenAPI. | `AccountController`, `CategoryController`, `ProductController`, `UserController` | services | clientes HTTP |
| `web.exception.enums` | 1 | Códigos estáveis de erro. | `ApiErrorCode` | — | handler, `ProblemDetails` |
| `web.exception.handler` | 1 | `@RestControllerAdvice` central. | `ControllerExceptionHandler` | `MessageSource`, exceções | todos os controllers |
| `web.exception.response` | 3 | Corpo padronizado de erro. | `ProblemDetails`, `ValidationError`, `FieldMessage` | `ApiErrorCode` | handler, validators |

Recursos (`src/main/resources`): `application*.properties` (4), `messages_{pt_BR,en,es}.properties`, `db/migration/{schema,data}` (17 scripts), `templates/` (3 HTML), `static/image/` (2), `banner-dev.txt`, `import.sql`, `META-INF/additional-spring-configuration-metadata.json`.

---

## 4. Inventário de classes

Legenda de tipo: **ent** = entidade JPA; **rec** = record; **ifc** = interface; **enum**; **ann** = annotation; **cfg** = `@Configuration`; **svc** = `@Service`; **ctl** = `@RestController`; **exc** = exceção; **val** = `ConstraintValidator`.
Legenda de JavaDoc (heurística — ver seção 11): **C** = há JavaDoc de tipo; **M** = há JavaDoc em (quase) todos os métodos públicos; **m** = em parte; **–** = ausente.

### 4.1 Domain

| Classe | Tipo | Responsabilidade aparente [FATO] | Dependências principais | JD |
|--------|:----:|----------------------------------|-------------------------|:--:|
| `Identifiable<ID>` | ifc | Declara `ID getId()`. | — | – |
| `domain.catalog.Category` | ent | Tabela `tb_category`; `name` único (len 80), `description` (255), `active`; `@PrePersist` define `createdAt`, `@PreUpdate` define `updatedAt`; lado inverso `@ManyToMany(mappedBy="categories")` com `Product`. `equals/hashCode` por `id`. Não implementa `Identifiable`. | `Product` | C/m |
| `domain.catalog.Product` | ent | Tabela `tb_product`; `name` único; `description` TEXT; `price` (`Double`); `imgUrl`; `active`; dono do `@ManyToMany` `categories` via `tb_product_category`. Callbacks `prePersist`/`preUpdate` **privados**. Implementa `Identifiable<Long>`. | `Category`, `Identifiable` | C/m |
| `domain.user.User` | ent | Tabela `tb_user`; implementa `UserDetails` (`getUsername()`=`email`, `getAuthorities()`=`roles`, `isEnabled()`=`active`; demais `isXxxNonYyy` retornam `true`). `@ManyToMany roles` via `tb_user_role`; `@OneToMany tokens` (cascade ALL, orphanRemoval). Métodos de domínio `activate()`, `deactivate()`, `hasRole(String)`, `addRole(Role)`. O campo `tokens` **não possui getter**. | `Role`, `Token` | C/m |
| `domain.user.Role` | ent | Tabela `tb_role`; implementa `GrantedAuthority` (`authority` = ex.: `ROLE_ADMIN`). | Spring Security | C/m |
| `domain.recovery.Token` | ent | Tabela `tb_token`; `token` UUID único; `@ManyToOne(LAZY) user`; `createdAt` (não atualizável), `expireDate`, `disabled`, `type`. Fábricas estáticas `activationToken(User, long horas)` e `passwordRecoveryToken(User, long minutos)`; `isExpired()`, `isValid()`, `disable()`, `validate(TokenType)` (lança `InvalidTokenException` com chaves `error.token.type.invalid` / `error.token.disabled` / `error.token.expired`). Construtor padrão `protected`. | `User`, `TokenType`, `InvalidTokenException` | – |
| `domain.recovery.Email` | ent | Tabela `tb_email` (log de e-mails): `sender`, `recipient`, `content` TEXT, `createdAt`, `status`. Construtor `Email(EmailRegisterRequest)` define `createdAt=now` e `status=PENDING`. | `EmailRegisterRequest`, `EmailStatus` | – |
| `domain.recovery.enums.TokenType` | enum | `ACTIVATION`, `PASSWORD_RECOVERY` (persistido como `STRING`). | — | – |
| `domain.recovery.enums.EmailStatus` | enum | `PENDING`, `SENT`, `ERROR` (persistido como `STRING`). | — | C |

### 4.2 DTOs (todos `record`)

| Classe | Tipo | Campos / validações principais [FATO] | Anotações de classe | JD |
|--------|:----:|---------------------------------------|---------------------|:--:|
| `CategoryCreateRequest` | rec | `name` (`@NotBlank`, `@Size 3–80`, `@Pattern` letras/dígitos/espaço), `description` (`@Pattern` vazio ou 3–255) | `@CategoryCreateValid` | C |
| `CategoryUpdateRequest` | rec | Mesmos campos/regras de `CategoryCreateRequest` | `@CategoryUpdateValid` | C |
| `CategoryResponse` | rec | `id`, `name` | — | C |
| `CategoryDetailsResponse` | rec | `id`, `name`, `description`, `active` | — | C |
| `ProductCreateRequest` | rec | `name` (3–100, pattern), `description` (3–200), `price` (`@Positive`), `imgUrl` (`^(https?://).+$`), `date` (`@PastOrPresent`, `Instant`), `categoryIds` (`@NotEmpty`) | `@ProductCreateValid` | C |
| `ProductUpdateRequest` | rec | `name`, `description`, `price`, `imgUrl`, `categoryIds` (sem `date`) | `@ProductUpdateValid` | C |
| `ProductResponse` | rec | `id`, `name`, `description`, `price`, `imgUrl`, `categories: List<CategoryResponse>` | — | C |
| `ProductDetailsResponse` | rec | `ProductResponse` + `createdAt`, `updatedAt`, `active`; `categories: List<CategoryDetailsResponse>` | — | C |
| `RoleResponse` | rec | `id`, `authority` | — | C |
| `UserCreateRequest` | rec | `firstName`, `lastName` (2–80), `email` (`@ValidEmail`, `@UniqueEmail`), `password` (10–72, `@StrongPassword`), `roleIds` (`@ValidRoles`); implementa `PasswordPersonalDataCandidate` | `@PasswordPersonalData` | C |
| `UserRegisterRequest` | rec | Como `UserCreateRequest`, **sem** `roleIds` | `@PasswordPersonalData` | C |
| `UserUpdateRequest` | rec | `firstName`, `lastName`, `email` (`@ValidEmail`), `password` (`@StrongPassword`, opcional), `roleIds` (`@ValidRoles`) | `@UserUpdateValid` | C |
| `AuthenticatedUserUpdateRequest` | rec | `firstName`, `lastName` (≤100), `email` (`@Email`, ≤255, `@UniqueEmailForAuthenticatedUser`) | — | – |
| `PasswordUpdateRequest` | rec | `currentPassword`, `newPassword` (10–72, `@StrongPassword`), `confirmPassword` (10–72) | — | – |
| `PasswordResetRequest` | rec | `token` (`@NotBlank`), `password` (`@StrongPassword`) | — | – |
| `UserEmailRequest` | rec | `email` (`@NotBlank`, `@ValidEmail`) | — | – |
| `UserResponse` | rec | `id`, `firstName`, `lastName`, `email`, `roles: Set<RoleResponse>` | — | C |
| `UserDetailsResponse` | rec | `UserResponse` + `active` | — | C |
| `EmailRegisterRequest` | rec | `sender`, `recipient` (`@Email`), `content` — usado internamente para criar `Email` | — | C |

### 4.3 Mappers, projeções e utilitário

| Classe | Tipo | Responsabilidade [FATO] | JD |
|--------|:----:|-------------------------|:--:|
| `CategoryMapper` | `@Component` | `toEntity`, `updateEntity` (só sobrescreve campos não nulos), `toResponse`, `toDetailsResponse`, `toResponsePage`. | C/m |
| `ProductMapper` | `@Component` | Idem para `Product`; `toResponse`/`toDetailsResponse` expandem `categories`. Não converte `categoryIds` (isso é feito em `ProductService.syncCategories`). | C/M |
| `UserMapper` | `@Component` | `toEntity` (sobrecarga para `UserCreateRequest` e `UserRegisterRequest`, aceitando `Set<Role>`), `updateEntity(UserUpdateRequest, User)` (atualiza apenas nome, sobrenome e e-mail), `toResponse`, `toDetailsResponse`, `toResponsePage`, `toRoleResponse` (privado). | C/m |
| `ProductProjection` | ifc | `getName()`; estende `Identifiable<Long>`. Projeção da query nativa `searchProducts`. | – |
| `UserDetailsProjection` | ifc | `getId`, `getUsername`, `getPassword`, `getRoleId`, `getAuthority`, `getActive`. Projeção da query nativa `searchUserAndRolesByEmail`. | C |
| `IdentifiableUtils` | classe `final` | `reorderByReference(List<T>, List<? extends Identifiable<ID>>)`: reordena a primeira lista segundo a ordem da segunda. | – |

### 4.4 Repositories (todos `@Repository`, estendem `JpaRepository<E, Long>`)

| Interface | Métodos customizados [FATO] |
|-----------|-----------------------------|
| `CategoryRepository` | `findByNameContainingIgnoreCase(String, Pageable)`, `existsByNameIgnoreCase`, `existsByNameIgnoreCaseAndIdNot` |
| `ProductRepository` | `findByNameContainingIgnoreCase(String, Pageable)`; `searchProducts(List<Long> categoryIds, String name, Pageable)` — **native query** paginada com `countQuery`, retorna `Page<ProductProjection>`; `searchProductsWithCategories(List<Long>)` — JPQL com `JOIN FETCH obj.categories`; `existsByNameIgnoreCase`, `existsByNameIgnoreCaseAndIdNot` |
| `UserRepository` | `findByFirstNameContainingIgnoreCase(String, Pageable)`, `findByEmail`, `existsByEmailIgnoreCase`, `existsByEmailIgnoreCaseAndIdNot`; `searchUserAndRolesByEmail(String)` — **native query** (`tb_user` ⨝ `tb_user_role` ⨝ `tb_role`) → `List<UserDetailsProjection>` |
| `RoleRepository` | `findByAuthority(String)` |
| `TokenRepository` | `findByToken(String)`, `findByUserAndTypeAndDisabledFalse(User, TokenType)` |
| `EmailRepository` | nenhum (somente CRUD herdado) |

### 4.5 Services

| Classe | Responsabilidade [FATO] | Dependências | JD |
|--------|-------------------------|--------------|:--:|
| `UserService` (`UserDetailsService`) | CRUD administrativo de usuários (`search`, `findById`, `create`, `update`, `activate`, `deactivate`, `delete`) e `loadUserByUsername`, que monta um `User` parcial (id, e-mail, senha, `active`, roles) a partir de `searchUserAndRolesByEmail`. Valida existência de roles (`findRolesByIdsOrThrow`). Codifica a senha na criação/atualização. | `UserRepository`, `RoleRepository`, `UserMapper`, `PasswordEncoder` | C/M |
| `AccountService` | Ciclo de vida da conta: `register` (cria usuário inativo com `ROLE_OPERATOR`, cria token e dispara e-mail de ativação), `confirmEmail`, `requestPasswordRecovery`, `resendActivationEmail`, `resetPassword`, `updateAuthenticatedUser`, `updatePassword` (valida confirmação, senha atual e novidade), `getAuthenticatedUser`. `deactivateAccount()` lança `UnsupportedOperationException`. | `UserRepository`, `RoleRepository`, `UserMapper`, `PasswordEncoder`, `TokenService`, `EmailService`, `AuthenticatedUserService` | C/m |
| `CategoryService` | CRUD de categorias (`search`, `findById`, `create`, `update`, `activate`, `deactivate`, `delete`); `changeStatus` privado. | `CategoryRepository`, `CategoryMapper` | C/M |
| `ProductService` | CRUD de produtos; `search` (JPA por nome) e `findAllPaged(name, categoryId, pageable)` (native query + segunda consulta com `JOIN FETCH` + `reorderByReference`); `syncCategories`; `delete` converte `DataIntegrityViolationException` em `DatabaseException`. | `ProductRepository`, `CategoryRepository`, `ProductMapper`, `IdentifiableUtils` | C/M |
| `TokenService` (`@Transactional` na classe) | `createActivationToken`, `createPasswordRecoveryToken`, `disableAllActivationTokens`, `disableAllPasswordRecoveryTokens`, `findAndValidateToken`. Lê `account.activation.token.hours` e `account.password-recovery.token.minutes`. | `TokenRepository` | – |
| `EmailService` | Envio de e-mail HTML via `JavaMailSender` + Thymeleaf: `sendActivationEmail`/`sendPasswordRecoveryEmail` (síncronos) e variantes `...Async` anotadas com `@Async` retornando `CompletableFuture<Void>`. Registra cada envio em `tb_email` (`registerEmailLog`). Usa `frontend.url` e `backend.url`. | `JavaMailSender`, `SpringTemplateEngine`, `EmailRepository` | C/M |

### 4.6 Web

| Classe | Tipo | Responsabilidade | JD |
|--------|:----:|------------------|:--:|
| `AccountController` | ctl | `@RequestMapping("/api/v1/accounts")`; `@Tag("Conta")`. | – (usa `@Operation`) |
| `CategoryController` | ctl | `@RequestMapping("/api/v1/categories")`; `@Tag("Categorias")`. | C |
| `ProductController` | ctl | `@RequestMapping("/api/v1/products")`; `@Tag("Produtos")`. | C |
| `UserController` | ctl | `@RequestMapping("/api/v1/users")`; `@Tag("Usuários")`. | C |
| `ControllerExceptionHandler` | `@RestControllerAdvice` | Converte exceções em `ProblemDetails`/`ValidationError` com mensagens i18n (`MessageSource`). | C |
| `ApiErrorCode` | enum | `VALIDATION_ERROR`, `PASSWORD_UPDATE_ERROR`, `RESOURCE_NOT_FOUND`, `DATABASE_ERROR`, `CONFLICT`, `INVALID_TOKEN`, `ACCESS_DISABLED`, `ACCESS_DENIED`, `AUTHENTICATION_REQUIRED`, `INTERNAL_SERVER_ERROR` (com `@Schema`). | – |
| `ProblemDetails` | classe (`Serializable`) | `timestamp`, `status`, `code`, `error`, `message`, `path`. | C/m |
| `ValidationError` | classe | Estende `ProblemDetails`; adiciona `fieldErrors: List<FieldMessage>` e `addError`. | – |
| `FieldMessage` | rec | `fieldName`, `message`. | – |

### 4.7 Exceções de serviço

`ResourceNotFoundException`, `DatabaseException`, `InvalidTokenException`, `PasswordUpdateException`, `AuthenticatedUserNotFoundException` — todas estendem `RuntimeException` e recebem apenas uma mensagem (na prática, uma **chave i18n**, ex.: `error.user.notFound`). JD: só `ResourceNotFoundException` e `DatabaseException`.

### 4.8 Validation

| Classe | Tipo | Regra observada [FATO] |
|--------|:----:|------------------------|
| `CategoryCreateValid` / `CategoryCreateValidator` | ann / val | Nome único (`existsByNameIgnoreCase`); erro no campo `name`. |
| `CategoryUpdateValid` / `CategoryUpdateValidator` | ann / val | Nome único excluindo o próprio `id`, obtido de `HttpServletRequest` (atributo `URI_TEMPLATE_VARIABLES_ATTRIBUTE`, variável `id`). |
| `ProductCreateValid` / `ProductCreateValidator` | ann / val | Nome único; todas as `categoryIds` devem existir (`existsById`). |
| `ProductUpdateValid` / `ProductUpdateValidator` | ann / val | Nome único excluindo o próprio `id` (via URI); `categoryIds` existentes. |
| `ValidRoles` / `ValidRolesValidator` | ann (`FIELD`) / val | Todos os `roleIds` existem; conjunto nulo/vazio é válido. |
| `PasswordPersonalData` / `PasswordPersonalDataValidator` | ann (`TYPE`) / val | A senha não pode conter nome, sobrenome ou prefixo do e-mail (tokens ≥ 3 caracteres). Usa o contrato `PasswordPersonalDataCandidate`. |
| `StrongPassword` / `StrongPasswordValidator` | ann (`FIELD`) / val | Sem espaços; ≥ 1 maiúscula, 1 minúscula, 1 dígito, 1 caractere especial; não estar em lista de senhas comuns; sem sequência numérica ≥ 6 (crescente ou decrescente). |
| `UniqueEmail` / `UniqueEmailValidator` | ann / val | `existsByEmailIgnoreCase` (e-mail normalizado com `trim` + `lowercase`). |
| `UniqueEmailForAuthenticatedUser` / `UniqueEmailForAuthenticatedUserValidator` | ann / val | Unicidade excluindo o usuário autenticado (usa `AuthenticatedUserService`). Único par de validação sem teste (ver seção 12). |
| `UserUpdateValid` / `UserUpdateValidator` | ann (`TYPE`) / val | E-mail único excluindo o `id` da URI; senha (se informada) não contém dados pessoais. Duplica a lógica de `PasswordPersonalDataValidator`. |
| `ValidEmail` / `ValidEmailValidator` | ann / val | Regex de formato **e consulta DNS de registro MX** do domínio (`InitialDirContext`, `com.sun.jndi.dns.DnsContextFactory`). |
| `PasswordPersonalDataCandidate` | ifc | `firstName()`, `lastName()`, `email()`, `password()`. |

### 4.9 Security e configuração

| Classe | Tipo | Responsabilidade [FATO] | JD |
|--------|:----:|-------------------------|:--:|
| `SecurityBeansConfig` | cfg | `PasswordEncoder` → `BCryptPasswordEncoder`. | C |
| `AuthorizationServerConfig` | cfg | Ver seção 9. | C/M |
| `ResourceServerConfig` | cfg (`@EnableWebSecurity`, `@EnableMethodSecurity`) | Ver seção 9. | C/M |
| `CustomPasswordAuthenticationConverter` | `AuthenticationConverter` | Converte requisição `grant_type=password` em `CustomPasswordAuthenticationToken`; valida `username`, `password` e `scope` (parâmetros únicos). | C/M |
| `CustomPasswordAuthenticationProvider` | `AuthenticationProvider` | Autentica cliente e usuário, gera access token (JWT) **e refresh token**, salva `OAuth2Authorization`. | C/M |
| `CustomPasswordAuthenticationToken` | token | Carrega `username`, `password`, `scopes`; `AuthorizationGrantType("password")`. | C/M |
| `AuthenticatedUser` | classe | `id`, `username`, `authorities` (imutável). | C/M |
| `AuthenticatedUserService` | `@Service` | `getAuthenticatedUser()` (lê o claim `userId` do `Jwt` e carrega o `User`) e `isCurrentUser(Long)`. | C/m |
| `SpringDocOpenApiConfig` | cfg | Bean `OpenAPI`. | – |
| `MessageSourceConfig` | cfg | `MessageSource` e `AcceptHeaderLocaleResolver`. | – |
| `DscatalogApplication` | — | `main`. | – |

---

## 5. Camadas arquiteturais

### 5.1 Domain
- **Entidades (6):** `Category`, `Product`, `User`, `Role`, `Token`, `Email`.
- **Enums persistidos (2):** `TokenType`, `EmailStatus` (ambos `@Enumerated(STRING)`). `ApiErrorCode` é enum da camada web (não persistido).
- **Interface de domínio:** `Identifiable<ID>`.
- **Regras embutidas nas entidades:** `Token` (validade, tipo, fábricas de expiração), `User` (`activate/deactivate/hasRole`), callbacks de timestamps em `Category`/`Product`.
- **Relacionamentos [FATO]:** `Product` N—N `Category` (dono: `Product`, tabela `tb_product_category`); `User` N—N `Role` (dono: `User`, tabela `tb_user_role`); `User` 1—N `Token` (dono: `Token.user`, LAZY; `User.tokens` com cascade ALL e orphanRemoval); `Email` não tem relacionamento com outras entidades.
- **Observação [FATO]:** `User` e `Role` implementam interfaces do Spring Security, ou seja, o domínio conhece a camada de segurança.

### 5.2 DTO
- **Request:** 12 records (categoria 2, produto 2, usuário 7, e-mail 1 — o `EmailRegisterRequest` é de uso interno).
- **Response:** 7 records (`CategoryResponse`, `CategoryDetailsResponse`, `ProductResponse`, `ProductDetailsResponse`, `RoleResponse`, `UserResponse`, `UserDetailsResponse`) mais o corpo de erro `ProblemDetails`/`ValidationError`/`FieldMessage` (pacote `web.exception.response`).
- **DTOs de autenticação:** não há DTO Java para o token OAuth2; a resposta de `/oauth2/token` é produzida pelo Authorization Server. `PasswordResetRequest`, `UserEmailRequest`, `PasswordUpdateRequest` e `AuthenticatedUserUpdateRequest` são DTOs de conta.
- **DTOs internos:** `EmailRegisterRequest` (entrada do construtor de `Email`).

### 5.3 Repository
Ver 4.4. **Queries nativas:** `ProductRepository.searchProducts` (com `countQuery` e uso de `:categoryIds IS NULL OR ... IN :categoryIds`) e `UserRepository.searchUserAndRolesByEmail`. **JPQL:** `searchProductsWithCategories`. **Paginação:** `Page<…>`/`Pageable` em `findByNameContainingIgnoreCase` (categoria, produto), `findByFirstNameContainingIgnoreCase` (usuário) e `searchProducts`. **Filtros:** nome (`ContainingIgnoreCase`) e categorias.

### 5.4 Service
Ver 4.5. Todos os services de escrita usam `@Transactional`; `UserService.findEntityById` traz `@Transactional(readOnly = true)` em método **privado** (sem efeito prático por proxy — [INFERÊNCIA]).

### 5.5 Web
4 controllers, 1 advice, 3 classes de resposta de erro. Ver seções 6 e 7.

### 5.6 Validation
11 annotations, 11 validators, 1 contrato. Ver 4.8. Os validadores baseados em repositório (`Category*`, `Product*`, `UniqueEmail*`, `ValidRoles`) recebem beans Spring por injeção no construtor; `Category/Product/UserUpdateValidator` injetam `HttpServletRequest`.

### 5.7 Security
Ver seção 9.

### 5.8 Configuration / Infrastructure
`SpringDocOpenApiConfig`, `MessageSourceConfig`, `SecurityBeansConfig`, `AuthorizationServerConfig`, `ResourceServerConfig`; propriedades (seção 10); e-mail (`JavaMailSender` autoconfigurado a partir de `spring.mail.*`); Flyway; H2 console (apenas no perfil `test`). **Não há** `@EnableAsync`, `@EnableScheduling`, `@EnableJpaRepositories` ou `@ConfigurationProperties` no código de produção [FATO — busca por `grep`].

---

## 6. Fluxos principais

### 6.1 Cadeia geral de requisição [FATO]
`Controller` → `Service` → `Repository` → `Entity/Table`, com `Mapper` chamado pelo service (não pelo controller) para converter DTO ↔ entidade. Validação Bean Validation ocorre antes do controller (`@Valid @RequestBody`), e exceções de qualquer camada são convertidas por `ControllerExceptionHandler`. Autorização por método (`@PreAuthorize`) é avaliada na entrada do controller.

### 6.2 Autenticação (grant `password`) [FATO]
1. `POST /oauth2/token` (cadeia `@Order(2)` de `AuthorizationServerConfig`, `securityMatcher("/oauth2/**", "/.well-known/**")`). O cliente é autenticado por Spring (Basic com `security.client-id`/`security.client-secret`).
2. `CustomPasswordAuthenticationConverter.convert` só atua se `grant_type=password`; exige `username` e `password` únicos e monta `CustomPasswordAuthenticationToken`.
3. `CustomPasswordAuthenticationProvider.authenticate`: obtém o cliente autenticado; chama `UserService.loadUserByUsername`; compara senha (`PasswordEncoder.matches`); valida status (`isEnabled`, `isAccountNonLocked`, `isAccountNonExpired`, `isCredentialsNonExpired`); calcula escopos autorizados como a interseção entre as *authorities* do usuário e os escopos do cliente (`read`, `write`); pendura um `AuthenticatedUser(id, username, authorities)` nos `details` do `OAuth2ClientAuthenticationToken` e recria o `SecurityContext`.
4. O `OAuth2TokenGenerator` (`DelegatingOAuth2TokenGenerator` com `JwtGenerator`, `OAuth2AccessTokenGenerator`, `OAuth2RefreshTokenGenerator`) gera o access token; o `tokenCustomizer` acrescenta os claims `authorities`, `userId` e `username`. Em seguida gera o refresh token e salva a `OAuth2Authorization` em `InMemoryOAuth2AuthorizationService`.
5. Resposta: `OAuth2AccessTokenAuthenticationToken` (access + refresh token).

### 6.3 Autorização em recursos [FATO]
`rsSecurityFilterChain` (`@Order(3)`): `csrf` desabilitado; `GET` em `/api/v1/categories/**`, `/api/v1/products/**`, `/api/v1/accounts/**` e `POST` em `/api/v1/accounts/**` são `permitAll`; documentação Swagger/OpenAPI (`/docs-asjcatalog*`, `/swagger-ui/**`) `permitAll`; qualquer outra requisição exige autenticação; `oauth2ResourceServer().jwt()`. O `JwtAuthenticationConverter` lê as *authorities* do claim `authorities`, com prefixo vazio (valores como `ROLE_ADMIN` são usados como estão). As regras finas ficam em `@PreAuthorize` nos controllers (`hasRole('ADMIN')`, `hasRole('ADMIN') or hasRole('OPERATOR')`, `isAuthenticated()`, e SpEL com `@authenticatedUserService.isCurrentUser(#id)`).

### 6.4 JWT / JWK [FATO]
O par RSA (2048 bits) e o `kid` são gerados **em memória a cada inicialização** (`generateRsa()` → `JWKSource`); o `JwtDecoder` é criado a partir dessa mesma `JWKSource`. Access token autocontido (`SELF_CONTAINED`), duração `security.jwt.duration` (padrão 86400 s).

### 6.5 Refresh token [FATO / INFERÊNCIA]
- **[FATO]** O cliente registrado aceita `AuthorizationGrantType.REFRESH_TOKEN`; `refreshTokenTimeToLive = 30 dias`; `reuseRefreshTokens = false` (rotação); o provider `password` gera e salva um refresh token.
- **[INFERÊNCIA]** Não há provider customizado para `grant_type=refresh_token`; portanto o tratamento do refresh é feito pelo provider padrão do Spring Authorization Server, e o `tokenCustomizer` reaproveita o `AuthenticatedUser` guardado nos `details` do `Principal` salvo na autorização. Como a autorização fica em memória, refresh tokens não sobrevivem a reinício/instâncias diferentes.
- O `OAuth2TokenIT` cobre 4 cenários do fluxo de tokens (ver seção 12).

### 6.6 Registro e ativação de conta [FATO]
`POST /api/v1/accounts/register` → `AccountService.register`: busca `ROLE_OPERATOR` (`IllegalStateException` se ausente), mapeia `UserRegisterRequest` → `User`, codifica a senha, `deactivate()`, salva; `TokenService.createActivationToken` (UUID, validade `account.activation.token.hours`); `EmailService.sendActivationEmailAsync` (link `frontendUrl + "/activate-account?token=" + token`, template `activate_user_by_email_template`). Ativação: `GET /api/v1/accounts/activate?token=…` → `AccountService.confirmEmail` → `findAndValidateToken(token, ACTIVATION)`, `user.activate()`, `token.disable()`. Reenvio: `POST /resend-activation` → desabilita tokens de ativação ativos e gera outro (retorna silenciosamente se o e-mail não existe ou o usuário já está ativo).

### 6.7 Recuperação de senha [FATO]
`POST /api/v1/accounts/password-recovery` → `AccountService.requestPasswordRecovery(email)`: se o usuário existir, cria token `PASSWORD_RECOVERY` (validade `account.password-recovery.token.minutes`) e envia e-mail (link `frontendUrl + "/reset-password?token=" + token`); se não existir, não faz nada (resposta 204 em ambos os casos). `POST /reset-password` → `resetPassword(token, senha)`: valida o token (`PASSWORD_RECOVERY`), codifica a nova senha, desabilita o token.

### 6.8 Atualização da conta autenticada [FATO]
`PUT /api/v1/accounts/me` (dados: nome/sobrenome/e-mail) e `PATCH /api/v1/accounts/me/password` (senha) usam `AuthenticatedUserService.getAuthenticatedUser()` (claim `userId` do `Jwt`). `updatePassword` exige `newPassword == confirmPassword`, senha atual correta e senha nova diferente da atual, lançando `PasswordUpdateException` (422).

### 6.9 Envio de e-mail [FATO / INFERÊNCIA]
`EmailService` monta `MimeMessage` (multipart), renderiza o template Thymeleaf, anexa o logo inline (`cid:logo`, arquivo `.ico`), envia via `JavaMailSender` e grava um `Email` com status `PENDING`. **[FATO]** O status nunca é alterado para `SENT`/`ERROR` no código de produção. **[FATO]** Não há `@EnableAsync` no projeto, portanto **[INFERÊNCIA]** os métodos `@Async` são executados de forma síncrona na thread da requisição.

### 6.10 Tratamento de exceções [FATO]
`ControllerExceptionHandler` mapeia: `ResourceNotFoundException` e `NoResourceFoundException` → 404; `DatabaseException` → 400; `DataIntegrityViolationException` → 409; `MethodArgumentNotValidException` → 422 (`ValidationError` com `fieldErrors`); `AccessDeniedException` → 403; `InvalidTokenException` → 400; `AuthenticatedUserNotFoundException` → 401; `PasswordUpdateException` → 422; `DisabledException` → 403; `Exception` (genérico) → 500. As mensagens são resolvidas por `MessageSource` com o `Locale` da requisição; se a chave de detalhe não existir, usa-se a mensagem padrão da categoria.

### 6.11 Validação [FATO]
Anotações nos *records* (`@NotBlank`, `@Size`, `@Pattern`, `@Positive`, …) + anotações customizadas. Validators consultam repositórios (unicidade/existência) e, no caso de `@ValidEmail`, o DNS. As mensagens usam chaves `{...}` resolvidas pelo `MessageSource`.

### 6.12 Persistência [FATO]
Spring Data JPA com Hibernate; `spring.jpa.open-in-view=false`; timestamps em `Category`/`Product` via `@PrePersist`/`@PreUpdate`; esquema versionado por Flyway nos perfis `dev` (e `prod`, ver seção 10/15) e por Hibernate + `import.sql` no perfil `test` (H2 em memória, Flyway desabilitado).

---

## 7. Endpoints

Base: `/api/v1`. "Auth" descreve o requisito **efetivo lido no código** (URL rule + `@PreAuthorize`). Erros listados são os mapeados por `ControllerExceptionHandler`; 401 para token ausente/inválido em rotas autenticadas vem do Resource Server, não do handler.

### 7.1 `AccountController` — `/api/v1/accounts` (`@Tag("Conta")`)

| Método | Path | Método Java | Finalidade | Auth | Request | Response | Exceções principais |
|--------|------|-------------|------------|------|---------|----------|---------------------|
| POST | `/register` | `register` | Registra usuário inativo e envia e-mail de ativação | Pública (POST `/api/v1/accounts/**` `permitAll`) | `UserRegisterRequest` | 201 `UserResponse` | 422 validação; `MessagingException`/`IllegalStateException` → 500 (via handler genérico) |
| GET | `/activate?token=` | `activateAccount` | Confirma e-mail e ativa a conta | Pública (GET `/api/v1/accounts/**` `permitAll`) | `@RequestParam String token` | 204 | 404 (`error.token.notFound`), 400 (`InvalidTokenException`) |
| POST | `/resend-activation` | `resendActivationEmail` | Reenvia e-mail de ativação | Pública | `UserEmailRequest` | 204 | 422 |
| POST | `/password-recovery` | `requestPasswordRecovery` | Solicita recuperação de senha | Pública | `UserEmailRequest` | 204 | 422 |
| POST | `/reset-password` | `resetPassword` | Redefine senha com token | Pública | `PasswordResetRequest` | 204 | 422, 404, 400 |
| POST | `/deactivate` | `deactivateAccount` | Desativa a conta (**não implementado**) | URL `permitAll`; método `@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")` | — | 204 (declarado) | `UnsupportedOperationException` → 500 [FATO: `AccountService.deactivateAccount` lança essa exceção] |
| PUT | `/me` | `updateAuthenticatedUser` | Atualiza nome/sobrenome/e-mail do usuário autenticado | `authenticated()` (URL) + `@PreAuthorize("isAuthenticated()")` | `AuthenticatedUserUpdateRequest` | 200 `UserResponse` | 422, 401 (`AuthenticatedUserNotFoundException`) |
| PATCH | `/me/password` | `updatePassword` | Altera a senha do usuário autenticado | `authenticated()` (URL); **sem** `@PreAuthorize` e **sem** `@Operation` | `PasswordUpdateRequest` | 204 | 422 (validação ou `PasswordUpdateException`), 401 |
| GET | `/me` | `getAuthenticatedUser` | Retorna o usuário autenticado | URL `permitAll` (GET `/api/v1/accounts/**`) + `@PreAuthorize("isAuthenticated()")` | — | 200 `UserResponse` | 403 se anônimo (via `AccessDeniedException` → handler) [INFERÊNCIA], 401 |

### 7.2 `CategoryController` — `/api/v1/categories` (`@Tag("Categorias")`)

| Método | Path | Método Java | Finalidade | Auth | Request | Response | Exceções |
|--------|------|-------------|------------|------|---------|----------|----------|
| POST | `` | `create` | Cria categoria | `hasRole('ADMIN') or hasRole('OPERATOR')` | `CategoryCreateRequest` | 201 + `Location` `CategoryResponse` | 422, 403, 401 |
| GET | `` | `findAll` | Lista paginada, filtro `name` opcional | Pública (GET `permitAll`) | `name` (opc.), `Pageable` | 200 `Page<CategoryResponse>` | — |
| GET | `/{id}` | `findById` | Detalhe | Pública | `id` | 200 `CategoryDetailsResponse` | 404 |
| PATCH | `/{id}` | `update` | Atualiza | ADMIN ou OPERATOR | `CategoryUpdateRequest` | 200 `CategoryResponse` | 404, 422 |
| PATCH | `/{id}/activate` | `activate` | Ativa | ADMIN ou OPERATOR | — | 204 | 404 |
| PATCH | `/{id}/deactivate` | `deactivate` | Desativa | ADMIN ou OPERATOR | — | 204 | 404 |
| DELETE | `/{id}` | `delete` | Remove | ADMIN ou OPERATOR | — | 204 | 404; `DataIntegrityViolationException` → 409 |

### 7.3 `ProductController` — `/api/v1/products` (`@Tag("Produtos")`)

| Método | Path | Método Java | Finalidade | Auth | Request | Response | Exceções |
|--------|------|-------------|------------|------|---------|----------|----------|
| POST | `` | `create` | Cria produto | ADMIN ou OPERATOR | `ProductCreateRequest` | 201 + `Location` `ProductResponse` | 422, 404 (categoria) |
| GET | `` | `findAll` | Lista paginada; parâmetros `name` (padrão `""`) e `categoryIds` (padrão `"0"`, lista separada por vírgula) | Pública | `name`, `categoryIds`, `Pageable` | 200 `Page<ProductResponse>` (usa `ProductService.findAllPaged`) | `NumberFormatException` (id não numérico) → 500 [INFERÊNCIA] |
| GET | `/{id}` | `findById` | Detalhe | Pública | `id` | 200 `ProductDetailsResponse` | 404 |
| **PUT** | `/{id}` | `update` | Atualiza | ADMIN ou OPERATOR | `ProductUpdateRequest` | 200 `ProductResponse` | 404, 422 |
| PATCH | `/{id}/activate` | `activate` | Ativa | ADMIN ou OPERATOR | — | 204 | 404 |
| PATCH | `/{id}/deactivate` | `deactivate` | Desativa | ADMIN ou OPERATOR | — | 204 | 404 |
| DELETE | `/{id}` | `delete` | Remove | ADMIN ou OPERATOR | — | 204 | 404, 400 (`DatabaseException`) |

### 7.4 `UserController` — `/api/v1/users` (`@Tag("Usuários")`)

| Método | Path | Método Java | Finalidade | Auth | Request | Response | Exceções |
|--------|------|-------------|------------|------|---------|----------|----------|
| POST | `` | `create` | Cria usuário (ativo) | `hasRole('ADMIN')` | `UserCreateRequest` | 201 + `Location` `UserResponse` | 422, 404 (roles) |
| GET | `` | `findAll` | Lista paginada, filtro `firstName` | ADMIN | `firstName` (opc.), `Pageable` | 200 `Page<UserResponse>` | — |
| GET | `/{id}` | `findById` | Detalhe | `hasRole('ADMIN') OR (hasRole('OPERATOR') AND @authenticatedUserService.isCurrentUser(#id))` | `id` | 200 `UserDetailsResponse` | 404, 403 |
| PUT | `/{id}` | `update` | Atualiza (nome, e-mail, roles, senha opcional) | `hasRole('ADMIN') OR (hasRole('OPERATOR') AND #id == authentication.principal.id)` | `UserUpdateRequest` | 200 `UserResponse` | 404, 422, 403 |
| PATCH | `/{id}/activate` | `activate` | Ativa | ADMIN | — | 204 | 404 |
| PATCH | `/{id}/deactivate` | `deactivate` | Desativa | ADMIN | — | 204 | 404 |
| DELETE | `/{id}` | `delete` | Remove | ADMIN | — | 204 | 404; 409 |

### 7.5 Endpoints de infraestrutura
- `POST /oauth2/token` (Authorization Server; grants `password` e `refresh_token`) e `/.well-known/**` (metadados do Authorization Server, padrão do Spring).
- OpenAPI/Swagger: perfil `test` → `/docs-asjcatalog` e `/docs-asjcatalog.html`; perfil `dev` → `/docs-dscatalog` e `/docs-dscatalog.html`; `/swagger-ui/**`.
- H2 console (`/h2-console`) — cadeia `@Order(1)`, existe somente se `spring.h2.console.enabled=true` (perfil `test`).

---

## 8. Modelo de dados

### 8.1 Entity → Tabela → Migration [FATO]

| Entidade | Tabela | Migration de criação | Migration de FKs |
|----------|--------|----------------------|------------------|
| `Category` | `tb_category` | `V001__create_table_category.sql` | — |
| `Product` | `tb_product` | `V002__create_table_product.sql` | — |
| (`Product`↔`Category`) | `tb_product_category` | `V003__create_table_product_category.sql` | `V004__alter_table_product_category.sql` |
| `Role` | `tb_role` | `V005__create_table_role.sql` | — |
| `User` | `tb_user` | `V006__create_table_user.sql` | — |
| (`User`↔`Role`) | `tb_user_role` | `V007__create_table_user_role.sql` | `V008__alter_table_user_role.sql` |
| `Token` | `tb_token` | `V009__create_table_token.sql` | `V010__alter_table_token.sql` |
| `Email` | `tb_email` | `V011__create_table_email.sql` | — |

Dados iniciais (`db/migration/data`): `V100` (15 categorias), `V101` (25 produtos), `V102` (26 vínculos produto–categoria), `V103` (2 usuários — `albert@gmail.com`, `maria@gmail.com`, com hash BCrypt), `V104` (2 roles: `ROLE_OPERATOR`, `ROLE_ADMIN`), `V105` (3 vínculos usuário–role). O Flyway usa `locations=classpath:db/migration/schema,classpath:db/migration/data` (perfil `dev`).

### 8.2 Campos, IDs e constraints [FATO]

| Tabela | ID | Colunas relevantes / constraints |
|--------|----|----------------------------------|
| `tb_category` | `bigint generated by default as identity` | `name varchar(80) not null unique`; `description varchar(255)`; `active boolean not null`; `created_at`/`updated_at` `TIMESTAMP WITHOUT TIME ZONE` |
| `tb_product` | idem | `name varchar(255) not null unique`; `description TEXT`; `price float(53)`; `img_url varchar(255)`; `active boolean not null`; timestamps |
| `tb_product_category` | PK composta (`category_id`, `product_id`) | FKs para `tb_category` e `tb_product` (nomes gerados pelo Hibernate: `FK5r4sbavb4nkd9xpl0f095qs2a`, `FKgbof0jclmaf8wn2alsoexxq3u`) |
| `tb_role` | identity | `authority varchar(255)` (sem `unique`/`not null`) |
| `tb_user` | identity | `email varchar(255) not null unique`; `first_name`, `last_name`, `password` `varchar(255)` sem `not null`; `active boolean not null` |
| `tb_user_role` | PK composta (`role_id`, `user_id`) | FKs para `tb_role` e `tb_user` |
| `tb_token` | identity | `token varchar(255) not null unique`; `user_id bigint not null` (FK → `tb_user`); `created_at`, `expire_date` `not null`; `disabled boolean not null`; `type varchar(18) not null check (type in ('ACTIVATION','PASSWORD_RECOVERY'))` |
| `tb_email` | identity | `sender`, `recipient` `varchar(255) not null`; `content TEXT not null`; `created_at not null`; `status varchar(10) not null check (status in ('ERROR','PENDING','SENT'))` |

- **Índices explícitos:** nenhum além de PKs e `UNIQUE` (`tb_category.name`, `tb_product.name`, `tb_user.email`, `tb_token.token`) [FATO — nenhum `CREATE INDEX` nas migrations].
- **Arquivo `create.sql` (raiz):** contém DDL gerado por Hibernate (usa `status enum (...)` em vez de `varchar + check`, sintaxe MySQL/H2), portanto **não é idêntico** ao schema das migrations. Sua origem e uso não foram determinados (o gerador em `application-test.properties` está comentado).
- **`import.sql`:** carregado apenas no perfil `test` (Hibernate + H2); inclui 2 usuários, 2 roles, vínculos, categorias e produtos.

---

## 9. Segurança

### 9.1 Visão geral [FATO]
Duas `SecurityFilterChain` principais (+ uma opcional para H2) no mesmo contexto, com **Authorization Server e Resource Server na mesma aplicação**:

| Ordem | Bean | Escopo | Função |
|:----:|------|--------|--------|
| 1 | `h2SecurityFilterChain` | `PathRequest.toH2Console()` | CSRF e `frameOptions` desabilitados. Só existe se `spring.h2.console.enabled=true`. |
| 2 | `asSecurityFilterChain` | `/oauth2/**`, `/.well-known/**` | Authorization Server (`OAuth2AuthorizationServerConfigurer`) + conversor/provider `password` + `oauth2ResourceServer().jwt()`. |
| 3 | `rsSecurityFilterChain` | demais rotas | Resource Server JWT; CORS; regras `permitAll`/`authenticated`. |

### 9.2 Authorization Server (`AuthorizationServerConfig`) [FATO]
- Cliente único **em memória** (`InMemoryRegisteredClientRepository`), `clientId`/`clientSecret` de `security.client-id`/`security.client-secret` (padrões `myclientid`/`myclientsecret`; segredo codificado com o `PasswordEncoder`), escopos `read` e `write`, grants `password` (customizado) e `refresh_token`.
- `TokenSettings`: access token `SELF_CONTAINED`, TTL `security.jwt.duration` (s); refresh TTL 30 dias; `reuseRefreshTokens=false`.
- `OAuth2AuthorizationService` e `OAuth2AuthorizationConsentService` **em memória**.
- `OAuth2TokenGenerator` delegando (JWT + access opaco + refresh); `OAuth2TokenCustomizer<JwtEncodingContext>` adiciona claims `authorities`, `userId`, `username` (apenas para `access_token`).
- `JWKSource` com par RSA-2048 gerado no *startup*; `JwtDecoder` = `OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)`.
- `AuthorizationServerSettings` com valores padrão (endpoints padrão).

### 9.3 Grant `password` customizado [FATO]
Ver seção 6.2. Erros: `OAuth2AuthenticationException` com `INVALID_REQUEST` (parâmetros ausentes/duplicados), `INVALID_CLIENT`, `INVALID_GRANT` (credenciais inválidas: mensagem "Invalid credentials"; conta não ativada; bloqueada/expirada), `SERVER_ERROR` (falha ao gerar tokens).

### 9.4 Resource Server (`ResourceServerConfig`) [FATO]
- `@EnableWebSecurity`, `@EnableMethodSecurity`.
- Listas: `DOCUMENTATION_OPENAPI` = `/docs-asjcatalog`, `/docs-asjcatalog/**`, `/docs-asjcatalog.html`, `/swagger-ui/**`; `PUBLIC_GET_ENDPOINTS` = `/api/v1/categories/**`, `/api/v1/products/**`, `/api/v1/accounts/**`; `PUBLIC_POST_ENDPOINTS` = `/api/v1/accounts/**`.
- `JwtAuthenticationConverter`: claim `authorities`, prefixo `""`.
- CORS: origens de `cors.origins` (lista separada por vírgula, `allowedOriginPatterns`), métodos `POST, GET, PUT, DELETE, PATCH`, `allowCredentials=true`, headers `Authorization` e `Content-Type`; um `CorsFilter` registrado com `HIGHEST_PRECEDENCE`. Observação: `OPTIONS` não está na lista de métodos permitidos [FATO] (a implicação para *preflight* não foi verificada).

### 9.5 Usuário autenticado [FATO]
`AuthenticatedUserService.getAuthenticatedUser()` exige `Authentication.getPrincipal()` do tipo `Jwt`, lê o claim `userId` (> 0) e busca o `User` no repositório; caso contrário lança `AuthenticatedUserNotFoundException` (chaves `error.auth.invalid.principal`, `error.auth.userId.claim.notFound`, `error.auth.user.notFound`) → 401.

### 9.6 Roles e authorities [FATO]
`ROLE_OPERATOR` e `ROLE_ADMIN` (dados iniciais). `User.getAuthorities()` devolve as `Role`. As authorities entram no JWT como lista de strings e voltam como `GrantedAuthority` sem prefixo adicional. `hasRole('ADMIN')` compara com `ROLE_ADMIN`.

---

## 10. Configuração

### 10.1 Arquivos [FATO]

| Arquivo | Conteúdo |
|---------|----------|
| `application.properties` | Perfil ativo `${APP_PROFILE:test}`; `spring.jpa.open-in-view=false`; `security.client-id`, `security.client-secret`, `security.jwt.duration`; `cors.origins`; `backend.url`, `frontend.url`; `spring.mail.*` (padrões `smtp.gmail.com:587`, `test@gmail.com`/`123456`, `smtp.auth`, `starttls.enable`, `ssl.trust=smtp.gmail.com`, **`spring.mail.test-connection=true`**); `account.activation.token.hours`, `account.password-recovery.token.minutes`. |
| `application-dev.properties` | Nome `DSCatalog`; porta 8080; banner `banner-dev.txt`; locale fixo `pt_BR`; **PostgreSQL** `jdbc:postgresql://localhost:5432/dscatalog` com `${POSTGRES_DATASOURCE_USER}` / `${POSTGRES_DATASOURCE_PASSWORD}` (sem valor padrão); `ddl-auto=none`; `show-sql=true`; **Flyway habilitado** com as duas *locations*; springdoc em `/docs-dscatalog(.html)`; logs (`com.albertsilva.dev.dscatalog=DEBUG`, Hibernate SQL, `BasicBinder=TRACE`); arquivo `logs/dev/dscatalog-dev.log` (10 MB, 10 históricos). |
| `application-test.properties` | Nome `DSCatalog`; porta 8080; banner; **H2 em memória** `jdbc:h2:mem:testdb` (`sa`, senha vazia); console H2 habilitado em `/h2-console`; **Flyway desabilitado**; springdoc em `/docs-asjcatalog(.html)`; logs detalhados; `logs/test/dscatalog-test.log` (5 MB, 5 históricos). Não define `ddl-auto` (padrão do Spring Boot para banco embarcado: `create-drop`) [INFERÊNCIA]. |
| `application-prod.properties` | Somente logging (níveis `WARN`/`INFO`, SQL `OFF`) e arquivo `logs/dscatalog-prod.log` (50 MB, 30 históricos). **Não define datasource, JPA nem Flyway** [FATO]. |
| `META-INF/additional-spring-configuration-metadata.json` | Descreve `security.client-id`, `security.client-secret`, `security.jwt.duration`, `cors.origins`. |
| `messages_{pt_BR,en,es}.properties` | 70 chaves cada (ver 15, item sobre divergência de chave). |
| `templates/*.html` | `activate_user_by_email_template`, `reset_password_email_template`, `reactivate_user_by_email_template` (esta última **não é referenciada** por nenhuma classe de produção [FATO — busca por `grep`]). |
| `import.sql` | Dados iniciais para o perfil `test`. |

### 10.2 Variáveis de ambiente lidas [FATO]
`APP_PROFILE`, `CLIENT_ID`, `CLIENT_SECRET`, `JWT_DURATION`, `CORS_ORIGINS`, `BACKEND_URL`, `FRONTEND_URL`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `ACTIVATION_TOKEN_HOURS`, `PASSWORD_RECOVER_TOKEN_MINUTES`, `POSTGRES_DATASOURCE_USER`, `POSTGRES_DATASOURCE_PASSWORD` (esta última só no perfil `dev`).

### 10.3 Configuração de testes [FATO]
Não há `src/test/resources`; os testes usam `application-test.properties` de `src/main/resources` (perfil `test` é o padrão). Os `@WebMvcTest` declaram `@ActiveProfiles("test")` e `@TestPropertySource("spring.h2.console.enabled=false")`; os `@SpringBootTest` (via `AbstractIT`) herdam o perfil padrão `test`.

---

## 11. Documentação existente

> **Atualização (fase B-1):** a tabela de cobertura desta seção reflete o estado **anterior** à B-1. Após a B-1, todo o pacote `domain` (incluindo `Token`, `Email`, `TokenType`, `Identifiable`) possui JavaDoc; ver [B-1-DOMAIN-LAYER.md](B-1-DOMAIN-LAYER.md). As demais camadas permanecem como descrito abaixo.

### 11.1 Artefatos [FATO]
- **README:** não existe (só o `HELP.md` gerado pelo Spring Initializr, ignorado no `.gitignore`).
- **Documentação de arquitetura/segurança/configuração:** não existe; o diretório `docs/` **não existia** antes desta fase.
- **OpenAPI:** existe via anotações (`@Tag`, `@Operation`, `@ApiResponses`, `@Schema`) nos controllers e `SpringDocOpenApiConfig`; documentação em português. O `PATCH /me/password` **não** possui `@Operation`.
- **JavaDoc:** existente e **em português**, porém parcial (ver 11.2). O `maven-javadoc-plugin` está configurado com `failOnError=false`; a geração do site JavaDoc não foi executada nesta fase.
- **Comentários:** presentes em `application-*.properties` (português), migrations (títulos), e blocos `// @formatter:off` e `// -----` no provider/config.

### 11.2 Cobertura de JavaDoc (heurística) [FATO/INFERÊNCIA]
Medição por script de leitura (conta métodos `public`/`protected` cujo bloco anterior termina em `*/`; **inclui construtores e cabeçalhos de `record`**, portanto é aproximada): **≈ 169 de ≈ 313** (~54%).

| Situação | Classes |
|----------|---------|
| **Bem documentadas (tipo + métodos)** | `AuthorizationServerConfig`, `ResourceServerConfig`, `CustomPassword*` (3), `AuthenticatedUser`, `ProductService`, `CategoryService` (7/8), `UserService` (8/9), `EmailService`, `ProductMapper`, validators de `Category/Product`, `UniqueEmail*`, `UserUpdateValidator`, `ValidEmailValidator`, annotations de `validation` |
| **Parcial** | `Category`, `Product`, `User`, `Role` (getters/setters e `hashCode` sem JD), `AccountService` (5/10), `UserMapper` (2/6), `AuthenticatedUserService` (2/3), `ProblemDetails` (12/15), `ValidRolesValidator` (1/2) |
| **Sem JavaDoc algum** | `Token`, `Email`, `TokenType`, `TokenService`, `AccountController`, `ControllerExceptionHandler` (só o tipo), `ValidationError`, `FieldMessage`, `ApiErrorCode` (usa `@Schema`), `IdentifiableUtils`, `Identifiable`, `ProductProjection`, `EmailRepository`, `SpringDocOpenApiConfig`, `MessageSourceConfig`, `DscatalogApplication`, exceções `AuthenticatedUserNotFoundException`/`InvalidTokenException`/`PasswordUpdateException`, DTOs `AuthenticatedUserUpdateRequest`, `PasswordResetRequest`, `PasswordUpdateRequest`, `UserEmailRequest`, `PasswordPersonalDataValidator`, `StrongPasswordValidator` |
| **Controllers `Category/Product/User`** | JavaDoc apenas no tipo; métodos documentados via `@Operation` (não JavaDoc) |

Observações para as próximas fases (sem ação nesta): há JavaDoc de qualidade variável (alguns só com `@return`/`@param`); a convenção de idioma já é o português.

---

## 12. Estrutura da camada de testes

> **SOMENTE LEITURA nesta fase.** Nenhum arquivo em `src/test/**` foi alterado.

### 12.1 Visão geral [FATO]
- **48** arquivos `.java` em `src/test/java` (~9,2 mil linhas): 41 classes de teste + 4 *factories* + `AbstractIT` + `TokenUtil` + `DscatalogApplicationTests`.
- **Total de testes executáveis:** **376** = 283 (execução padrão `mvn test`) + 93 (classes `*IT`, que **não** entram no `mvn test` padrão — ver 12.4).
- Estilo: JUnit 5 com `@DisplayName` e **`@Nested`** (135 ocorrências); Mockito com `MockitoExtension` (18 classes; 62 `@Mock`, 16 `@InjectMocks`); `@MockitoBean` (14) nos `@WebMvcTest`; `@WithMockUser` (17); `@Transactional` (7, nos ITs); `AssertJ`/assertions do JUnit.
- Padrão Arrange/Act/Assert com comentários.
- **Sem** Testcontainers, **sem** `@Disabled`/`@Ignore` (busca por `grep` retornou 0).
- Banco de testes: **H2 em memória**, perfil `test` (Hibernate gera o schema; `import.sql` carrega dados).

### 12.2 Estrutura por tipo

| Tipo | Anotação-base | Classes |
|------|---------------|---------|
| **Domain** | JUnit puro | `CategoryTest` (6), `ProductTest` (6), `UserTest` (10) |
| **Repository** | `@DataJpaTest` | `CategoryRepositoryTest` (10), `EmailRepositoryTest` (8), `ProductRepositoryTest` (9), `RoleRepositoryTest` (2), `TokenRepositoryTest` (4), `UserRepositoryTest` (14) |
| **Service (unitário)** | `MockitoExtension` | `AccountServiceTest` (13), `CategoryServiceTest` (16), `EmailServiceTest` (6), `ProductServiceTest` (15), `TokenServiceTest` (9), `UserServiceTest` (14) |
| **Controller** | `@WebMvcTest` + `@Import({ControllerExceptionHandler, ResourceServerConfig})` + `@MockitoBean` | `AccountControllerTest` (13), `CategoryControllerTest` (10), `ProductControllerTest` (9), `UserControllerTest` (12) |
| **Exception handler** | — | `ControllerExceptionHandlerTest` (2) |
| **Validation** | `MockitoExtension` | `CategoryCreate/UpdateValidatorTest` (3/4), `ProductCreate/UpdateValidatorTest` (4/4), `ValidRolesValidatorTest` (5), `PasswordPersonalDataValidatorTest` (10), `StrongPasswordValidatorTest` (12), `UniqueEmailValidatorTest` (5), `UserUpdateValidatorTest` (11), `ValidEmailValidatorTest` (11) |
| **Security (unitário)** | `MockitoExtension` | `AuthenticatedUserServiceTest` (8), `CustomPasswordAuthenticationConverterTest` (9), `CustomPasswordAuthenticationProviderTest` (7) |
| **Integração (`*IT`)** | `AbstractIT` = `@SpringBootTest` + `@AutoConfigureMockMvc` | `OAuth2TokenIT` (4), `ResourceServerAuthorizationIT` (7), `CategoryServiceIT` (13), `ProductServiceIT` (11), `UserServiceIT` (15), `CategoryControllerIT` (14), `ProductControllerIT` (14), `UserControllerIT` (15) |
| **Contexto** | `@SpringBootTest` | `DscatalogApplicationTests` (1) |

### 12.3 Fixtures e utilitários [FATO]
- `factory/{CategoryFactory, ProductFactory, RoleFactory, UserFactory}`: métodos estáticos que criam entidades, requests e responses (ex.: `ProductFactory.createProduct()` usa o construtor sem `createdAt`/`updatedAt`).
- `utils/TokenUtil` (`@Component`): obtém access token via `POST /oauth2/token` (grant `password`, Basic auth) para os ITs.
- Os ITs de autorização usam os usuários semeados por `import.sql` (`albert@gmail.com` = `ROLE_OPERATOR`; `maria@gmail.com` = `ROLE_OPERATOR` + `ROLE_ADMIN`).

### 12.4 Observação sobre execução [FATO]
O `pom.xml` não configura Surefire `includes` nem Failsafe; o Surefire padrão executa `*Test`, `Test*`, `*Tests`, `*TestCase` — portanto as 8 classes `*IT` **não rodam** em `mvn test`. Foram executadas separadamente apenas para diagnóstico (`mvn test -Dtest='*IT'`).

### 12.5 Mapa produção × testes (heurística por nome/referência; sem julgamento de cobertura)

**Com teste dedicado:** `Category`, `Product`, `User` (domain); os 6 repositories; os 6 services; os 4 controllers; `ControllerExceptionHandler`; `AuthenticatedUserService`; `CustomPasswordAuthenticationConverter`, `CustomPasswordAuthenticationProvider`; 10 dos 11 validators.

**Sem teste com o nome da classe (podem ser exercitadas indiretamente):**
`Token`, `Email`, `Role` (domain; `Role` só via repository/ITs), os 3 mappers (exercitados por services/ITs), `IdentifiableUtils` (exercitada por `ProductService` — o `ProductServiceTest` mocka o repositório), `CustomPasswordAuthenticationToken`, `AuthenticatedUser`, `AuthorizationServerConfig`/`ResourceServerConfig`/`SecurityBeansConfig` (exercitadas pelos ITs e `@WebMvcTest`), `SpringDocOpenApiConfig`, `MessageSourceConfig`, `UniqueEmailForAuthenticatedUserValidator` (**sem qualquer referência nos testes**), DTOs, exceções, `ProblemDetails`/`ValidationError`/`FieldMessage`/`ApiErrorCode`, `Identifiable`, projections (sem referência direta em testes).

---

## 13. Baseline de build

Comando: `mvn -B -DskipTests clean package` (Maven 3.8.4, JDK 17.0.1, Windows 11).

| Item | Resultado [FATO] |
|------|------------------|
| Resultado | **BUILD SUCCESS** (exit 0), 11,9 s |
| Compilação | 94 arquivos de produção e 48 de teste compilados com `javac` (`release 17`, `-parameters`) |
| Warnings | Nenhuma linha `WARNING` no log do build |
| Erros | Nenhum |

**Nota sobre o estado do repositório.** No início da sessão, `git status` mostrava os arquivos de teste novos como *untracked* e o HEAD era `d8eaaf4`. Durante a execução (às 16:54:03 do dia do baseline) apareceu o commit `ecf252a "feat: camada de test"`, autoria **Albertinesilva**, contendo exatamente 22 arquivos de teste — **esse commit não foi criado por esta fase** (nenhum comando `git commit` foi executado). Ao final da fase, `git status` reporta *working tree clean* antes da criação deste documento.

**Efeitos colaterais dos comandos de baseline (arquivos ignorados pelo Git):** recriação de `target/` (`clean package`, relatórios do Surefire) e escrita em `logs/test/dscatalog-test.log` pelos testes (perfil `test`). Nenhum arquivo versionado foi alterado.

---

## 14. Baseline de testes

Comando: `mvn -B test` (padrão) e, separadamente, `mvn -B test -Dtest='*IT' -Dsurefire.failIfNoSpecifiedTests=false`.

| Execução | Total | Passando | Falhando (failures) | Errors | Skipped |
|----------|:----:|:--------:|:-------------------:|:------:|:-------:|
| `mvn test` (padrão) | **283** | 278 | **5** | 0 | 0 |
| `*IT` (diagnóstico) | **93** | 88 | **5** | 0 | 0 |
| **Somadas** | **376** | 366 | **10** | 0 | 0 |

`mvn test` termina com **BUILD FAILURE** (exit 1) por causa das 5 falhas da execução padrão. Tempo total: 28,5 s (padrão).

### 14.1 Falhas na execução padrão (5) [FATO — mensagens do Surefire]

| # | Teste | Mensagem |
|---|-------|----------|
| 1 | `ProductTest.productShouldInstantiateCorrectly` (linha 29) | `expected: not <null>` — o teste exige `getCreatedAt()` não nulo após apenas instanciar o produto |
| 2 | `ProductControllerTest.findAllShouldReturnPage` | `Status expected:<200> but was:<500>` |
| 3 | `ProductControllerTest.findAllShouldReturnFilteredPage` | `Status expected:<200> but was:<500>` |
| 4 | `ProductControllerTest.updateShouldReturnUpdatedProductWhenIdExists` | `Status expected:<200> but was:<500>` |
| 5 | `ProductControllerTest.updateShouldReturnNotFoundWhenIdDoesNotExist` | `Status expected:<404> but was:<500>` |

**Causas observadas nos logs (diagnóstico, sem alteração):**
- **#1** — `Product.createdAt`/`updatedAt` só são preenchidos no `@PrePersist` (método privado); `ProductFactory.createProduct()` usa o construtor sem esses campos. Divergência entre o teste e o comportamento da entidade.
- **#2 e #3** — o log registra `NullPointerException: Cannot invoke "Page.getTotalElements()" because "response" is null` em `GET /api/v1/products`: o `ProductController.findAll` chama `productService.findAllPaged(...)`, mas o teste configura o mock de `productService.search(...)`; o mock de `findAllPaged` devolve `null`.
- **#4 e #5** — o log registra `HttpRequestMethodNotSupportedException: Request method 'PATCH' is not supported` seguido de `Unexpected error - path: /api/v1/products/…`: o teste usa `PATCH`, enquanto `ProductController.update` é mapeado como `PUT`; a exceção é engolida pelo handler genérico `Exception` e devolvida como **500** (em vez de 405).

### 14.2 Falhas nas classes `*IT` (5) [FATO]

| Teste | Mensagem | Causa observada |
|-------|----------|-----------------|
| `CategoryControllerIT.insertShouldCreateCategoryAndReturnCreated` | `No value at JSON path "$.description"` | `CategoryResponse` só contém `id` e `name` |
| `CategoryControllerIT.updateShouldUpdateCategoryWhenIdExists` | `No value at JSON path "$.description"` | idem |
| `ProductControllerIT.insertShouldCreateProductWithValidData` | `No value at JSON path "$.date"` | `ProductResponse` não tem `date` |
| `ProductControllerIT.updateShouldReturnProductResponseWhenIdExists` | `Status expected:<200> but was:<500>` | teste usa `PATCH` em `/products/{id}`; controller usa `PUT` |
| `ProductControllerIT.updateShouldReturnNotFoundWhenIdDoesNotExist` | `Status expected:<404> but was:<500>` | idem |

### 14.3 Classes de teste afetadas
`ProductTest` (1), `ProductControllerTest` (4), `CategoryControllerIT` (2), `ProductControllerIT` (3). Todas as demais classes: 0 falhas, 0 errors, 0 skipped.

### 14.4 Ruído nos logs [FATO]
O perfil `test` usa `logging.level.org.springframework.web=DEBUG` e `...mvc.method.annotation=TRACE`; o log do Maven da execução padrão tem ~5,1 mil linhas. Os logs de DEBUG imprimem `UserCreateRequest[... password=...]` (ver problema P1-3).

---

## 15. Problemas encontrados

Classificação sem correção. **Não foi aplicada nenhuma alteração.**

### P0 — Bloqueador
Nenhum. O projeto compila e o contexto Spring sobe nos testes (`DscatalogApplicationTests`, ITs).

### P1 — Relevante

| ID | Local | Problema | Evidência | Rótulo |
|----|-------|----------|-----------|--------|
| P1-1 | `pom.xml` / camada de testes | 10 testes falham (5 em `mvn test`, 5 em `*IT`); o build de testes padrão termina em BUILD FAILURE. | Seção 14 | CONFIRMADO |
| P1-2 | `ProductController.update` × `ProductControllerTest`/`IT` | Divergência de verbo HTTP: a produção usa `PUT /products/{id}`; testes usam `PATCH`. (Categoria usa `PATCH`, usuário e produto usam `PUT`.) Não determinado com segurança qual é a intenção. | Controllers (`@PutMapping` vs `patch(...)` nos testes) | CONFIRMADO (divergência); intenção não determinada |
| P1-3 | `UserController.create` (l. 70) e `UserController.update` (l. 136) | Registro em log (nível DEBUG) do *record* inteiro; `UserCreateRequest` e `UserUpdateRequest` têm o campo `password`, e o `toString()` de `record` o inclui. (Os mesmos padrões de log do request completo em `CategoryController` l. 69/132, `ProductController` l. 70/139, `CategoryService` l. 160 e `ProductService` l. 225 não expõem campo sensível — apenas registrado por consistência.) | Linha do log de teste: `UserCreateRequest[firstName=Pedro, ..., password=JAVA!@#ResTIc18, roleIds=[1]]` | CONFIRMADO (senha em log DEBUG; ativo nos perfis `dev` e `test`) |
| P1-4 | `EmailService` / aplicação | `@Async` sem `@EnableAsync` em qualquer classe; os métodos `...Async` provavelmente executam de forma síncrona. | Busca por `EnableAsync` em `src/main`: 0 ocorrências | Fato + INFERÊNCIA |
| P1-5 | `ValidEmailValidator` | Validação de e-mail faz consulta DNS (MX) durante a validação do request (`@ValidEmail` em `UserCreateRequest`, `UserRegisterRequest`, `UserUpdateRequest`, `UserEmailRequest`): depende de rede/DNS; falha de rede = e-mail "inválido". | `InitialDirContext` + `DnsContextFactory`; `catch (Exception) → false` | CONFIRMADO (código); impacto operacional é INFERÊNCIA |
| P1-6 | `application-prod.properties` | O perfil `prod` não define datasource, JPA nem Flyway (só logging). Com `APP_PROFILE=prod` o comportamento de banco depende exclusivamente de propriedades/variáveis externas não versionadas; e a presença de H2 e PostgreSQL no classpath poderia fazer o Spring escolher o H2 embarcado. | Arquivo lido | Fato + HIPÓTESE (comportamento em runtime não executado) |
| P1-7 | `UserController.update` (SpEL `#id == authentication.principal.id`) | O principal da requisição é um `Jwt`; `Jwt.getId()` retorna o claim `jti` (String), não o claim `userId`. A regra pode nunca autorizar um `OPERATOR` a editar o próprio usuário via `PUT /users/{id}`. Os testes verificam apenas o `GET /users/{id}` do próprio operador (`isCurrentUser`). | Leitura de `UserController`, `AuthorizationServerConfig` (claims) e `ResourceServerAuthorizationIT` | HIPÓTESE — precisa de verificação |
| P1-8 | Segurança/infra (`AuthorizationServerConfig`) | Cliente OAuth2, autorizações e consentimentos **em memória** e par JWK RSA gerado a cada inicialização: tokens (inclusive refresh, TTL 30 dias) deixam de valer a cada reinício e não funcionam com mais de uma instância. Segredo do cliente com padrão `myclientsecret` se `CLIENT_SECRET` não for definido. | Código e `application.properties` | Fato (comportamento); severidade dependente do uso pretendido |

### P2 — Observação

| ID | Local | Observação | Evidência |
|----|-------|------------|-----------|
| P2-1 | `ControllerExceptionHandler.handleGeneric` | `HttpRequestMethodNotSupportedException` (e outras exceções do Spring MVC não tratadas explicitamente) caem no handler genérico e viram **500** em vez de 405/400. | Logs dos testes #4/#5 |
| P2-2 | `Email` / `EmailService` | O `status` de `Email` é sempre `PENDING`; o log é gravado com o `content` fixo ("Confirmação de Cadastro"/"Redefinição de Senha") e o remetente `asjcatalog@gmail.com`, diferente do `From` da mensagem (`nao-responder@asjcatalog.com.br`). `SENT`/`ERROR` nunca são atribuídos. | `EmailService.registerEmailLog` |
| P2-3 | `AccountService.deactivateAccount` / `AccountController.deactivateAccount` | Endpoint `POST /api/v1/accounts/deactivate` publicado e documentado, mas o service lança `UnsupportedOperationException`. | `AccountService` l. ~280 |
| P2-4 | Templates | `reactivate_user_by_email_template.html` não é usado. `reset_password_email_template.html` referencia `${texto}`, que `EmailService.sendPasswordRecoveryEmail` não define (o texto padrão do template pode ser sobrescrito por vazio — comportamento do Thymeleaf não executado). | Busca por `grep`; `EmailService` |
| P2-5 | `ResourceServerConfig` (`PUBLIC_GET_ENDPOINTS`) | `GET /api/v1/accounts/**` é `permitAll` no nível de URL, inclusive `GET /me` (que depende de `@PreAuthorize("isAuthenticated()")`); um anônimo receberia 403 (via `AccessDeniedException`) em vez de 401 [INFERÊNCIA]. `POST /deactivate` também é `permitAll` no nível de URL e protegido só por `@PreAuthorize`. `PATCH /me/password` depende só da regra `anyRequest().authenticated()` e não tem `@PreAuthorize`. | Controllers e `ResourceServerConfig` |
| P2-6 | Documentação OpenAPI × perfil | Os caminhos liberados (`/docs-asjcatalog*`) só coincidem com o perfil `test`; no perfil `dev` os caminhos configurados são `/docs-dscatalog*`, que não estão na lista `permitAll` [INFERÊNCIA: exigirão autenticação]. | `application-dev.properties` × `ResourceServerConfig` |
| P2-7 | `messages_*.properties` | A chave usada no código é `error.auth.userId.claim.notFound`; existe em `messages_pt_BR` mas em `messages_en`/`messages_es` a chave correspondente é `error.auth.username.claim.notFound`. | `diff` das chaves |
| P2-8 | `pom.xml` / `SpringDocOpenApiConfig` | E-mails de contato com erros de digitação (`albertinesilva@.17gmail.com` no `pom.xml`; `albertinesilva,17@gmail.com` no OpenAPI); licença MIT no `pom.xml` × Apache 2.0 no OpenAPI. | Arquivos lidos |
| P2-9 | `ProductService.findAllPaged` | `categoryId` é dividido por `,` e convertido com `Long::parseLong` sem tratamento (valor não numérico → exceção → 500 [INFERÊNCIA]); quando `"0"` é enviado, usa lista vazia — o comportamento da query nativa com `:categoryIds IS NULL OR ... IN :categoryIds` para lista vazia não foi verificado [HIPÓTESE]. | `ProductService`, `ProductRepository` |
| P2-10 | `ProductService.search` | Método público sem uso em produção (o controller usa `findAllPaged`); os testes de controller mockam `search`. | Busca por `grep` |
| P2-11 | `TokenService.disableAllPasswordRecoveryTokens` | Público e sem uso em produção. | Busca por `grep` |
| P2-12 | `UserService` (`@Transactional(readOnly=true)` em método privado) | Anotação sem efeito prático em métodos privados chamados internamente (proxy). | Código |
| P2-13 | `PasswordPersonalDataValidator` × `UserUpdateValidator` | Lógica de "senha não contém dados pessoais" duplicada. Também `addErrors` repetido em vários validators. | Código |
| P2-14 | `Category` × `Product` | Inconsistência de modelagem: `Category` não implementa `Identifiable`, `Product` sim; `Category.prePersist` é público, `Product.prePersist` é privado; `Product.price` é `Double` (coluna `float(53)`) para valor monetário. | Código |
| P2-15 | `create.sql` (raiz) | Script DDL na raiz divergente do schema das migrations (tipos `enum`), sem documentação de uso. | `create.sql` × migrations |
| P2-16 | `.github/`, `.vscode/` | Scripts `recordToolUse.*` de hooks de ferramenta de upgrade Java; não têm relação com o backend. | Listagem |
| P2-17 | `Token.getDisabled()` | Getter booleano com nome `getDisabled` (e não `isDisabled`), diferente de `User.isActive()`. | Código |

### OBSERVAÇÃO
- `SpringBootTest` `DscatalogApplicationTests` passou, portanto o contexto sobe no perfil `test`; o perfil `dev` (PostgreSQL + Flyway) e `prod` **não foram executados** nesta fase.
- `spring.mail.test-connection=true` está em `application.properties`; o efeito prático em cada perfil não foi verificado [HIPÓTESE].
- Dados semeados (`V103`, `import.sql`) incluem usuários com hash BCrypt conhecido no repositório; a decisão de rodar `V10x` (dados) fora de ambientes de desenvolvimento não foi determinada (`prod` não define Flyway).
- `User` não expõe `tokens`; `Email` não referencia usuário.
- **Não foi possível determinar com segurança a partir da implementação atual:** a intenção de `create.sql`; o uso pretendido de `reactivate_user_by_email_template`; o comportamento em runtime do refresh token com o `tokenCustomizer` (nenhum teste de `grant_type=refresh_token` foi identificado — `OAuth2TokenIT` tem 4 testes, cujo conteúdo detalhado não foi analisado nesta fase).

---

## 16. Pontos que precisam ser compreendidos nas próximas fases

1. **Semântica de `Token`/`TokenService`/`AccountService`**: validade, desativação e ordem das operações (base para o JavaDoc de conta).
2. **Fluxo OAuth2 completo** (`AuthorizationServerConfig` ↔ `CustomPasswordAuthenticationProvider` ↔ `tokenCustomizer`): como `AuthenticatedUser` chega ao claim, e como o refresh reaproveita esses dados.
3. **`ResourceServerConfig`**: interação `permitAll` por URL × `@PreAuthorize` × handler de exceções (401 vs 403).
4. **Semântica do SpEL** em `UserController` (`isCurrentUser` vs `authentication.principal.id`).
5. **Regras de negócio implícitas em validators** (unicidade, normalização `trim + lowercase`, dependência do `HttpServletRequest` para o `id`, DNS/MX).
6. **`ProductService.findAllPaged`** (duas consultas + `reorderByReference`) e a query nativa com `categoryIds`.
7. **Convenções de i18n**: chaves de mensagem passadas como texto de exceção e resolvidas no handler.
8. **Estado real do JavaDoc**: definir padrão único (idioma, uso de `@param/@return/@throws`, tratamento de getters/setters, `record`s, annotations e testes de exemplo) antes de documentar em massa.
9. **Diferenças de perfil** (`test` × `dev` × `prod`) e origem do schema (Flyway × Hibernate).
10. **Camada de testes**: causas das 10 falhas listadas na seção 14 (a serem tratadas somente após a documentação do backend, conforme combinado).

---

## 17. Itens explicitamente fora do escopo desta fase

- Qualquer alteração em `src/main/**` ou `src/test/**` (código, testes, factories, mocks, configurações de teste).
- Criação/atualização/remoção de JavaDoc.
- Correção de bugs, de testes falhando ou de inconsistências listadas na seção 15.
- Refactoring, alteração de comportamento, de endpoints ou de contratos de API; sugestões de nova API.
- Atualização de dependências, plugins ou versões; alteração de `pom.xml`, `application*.properties`, migrations, templates ou mensagens.
- Execução do perfil `dev` (PostgreSQL/Flyway) e `prod`; geração do JavaDoc via `maven-javadoc-plugin`; cobertura de testes (JaCoCo não configurado).
- Criação de commits ou *push* (o commit `ecf252a` presente no HEAD **não** foi feito por esta fase).
- Avanço para a fase B-1 ou para a documentação JavaDoc.
