# ASJCatalog Backend — B-9: Revisão Arquitetural Consolidada

> **Fase:** B-9 — correlação entre camadas. Última fase de **conhecimento** antes da análise dos testes.
> **Base:** [B-0](B-0-BACKEND-INVENTORY.md), [B-1](B-1-DOMAIN-LAYER.md), [B-2](B-2-REPOSITORY-LAYER.md), [B-3](B-3-SERVICE-LAYER.md), [B-4](B-4-DTO-MAPPER-LAYER.md), [B-5](B-5-VALIDATION-LAYER.md), [B-6](B-6-SECURITY-OAUTH2-LAYER.md) (o arquivo real chama-se `B-6-SECURITY-OAUTH2-LAYER.md`), [B-7](B-7-WEB-LAYER.md), [B-8](B-8-CONFIG-INFRASTRUCTURE.md), conferidos contra o código quando a conclusão depende dele.
> **Alterações desta fase:** somente este documento. Nenhum código, teste, propriedade, migration, template, dependência ou JavaDoc foi alterado; nenhum teste ou aplicação foi executado; nenhum commit.

## 0. Como ler este documento

Cada descoberta recebe **uma** das quatro classificações, sem gradação numérica, sem ranking e sem severidade:

| Classificação | Significado |
|---------------|-------------|
| **CONFIRMADO** | Demonstrado diretamente por código, configuração, bytecode das bibliotecas ou histórico do git. |
| **INFERÊNCIA FORTE** | Consequência muito provável de fatos confirmados (regra do framework), **não executada**. |
| **HIPÓTESE** | Só se comprova executando (teste/aplicação). |
| **QUESTÃO DE CONTRATO** | Pode ser intencional; exige decisão de projeto antes de qualquer correção. Não é chamada de bug. |

Uma mesma descoberta pode ter duas classificações quando o **fato** é confirmado e a **intenção** é uma decisão (ex.: "CONFIRMADO + QUESTÃO DE CONTRATO").

**Nomes de endpoints do prompt × código.** Cinco nomes citados no pedido não coincidem com o código; usei os do código (fato): `POST /accounts/activate` → **`GET /accounts/activate?token=`**; `POST /accounts/recovery` → **`POST /accounts/password-recovery`**; `POST /accounts/reset` → **`POST /accounts/reset-password`**; `PATCH /accounts/me` → **`PUT /accounts/me`**; e existem ainda `POST /accounts/resend-activation` e `PATCH /accounts/me/password`.

**Evidências novas desta fase** (além da leitura dos documentos): leitura de código-fonte e testes; bytecode de `InMemoryOAuth2AuthorizationService` (Authorization Server 1.5.6); histórico do git dos controllers/DTOs/testes.

---

## 1. O backend como um todo

### 1.1 Visão sistêmica [CONFIRMADO]

```
                        ┌───────────────────────── um único processo Spring Boot 3.5.13 / Java 17 ─────────────────────────┐
 Cliente ─ HTTP ─▶  CorsFilter (HIGHEST_PRECEDENCE)
                    ├─ SecurityFilterChain @Order(1)  H2 console   (só se spring.h2.console.enabled=true → perfil test)
                    ├─ SecurityFilterChain @Order(2)  /oauth2/**, /.well-known/**  → Authorization Server (password + refresh_token; JWT RS256 em memória)
                    └─ SecurityFilterChain @Order(3)  demais rotas → Resource Server (JWT) + regras de URL
                              ▼
                    DispatcherServlet → HandlerMapping → resolução de argumentos (@PathVariable, @RequestBody, Pageable)
                              ▼        └─ Bean Validation (@Valid) — validators consultam DB / DNS / claim do JWT / HttpServletRequest
                    Proxy de segurança de método (@PreAuthorize)
                              ▼
                    Controller (sem lógica) ─▶ Service (@Transactional) ─▶ Repository (JPA / nativa / projeção) ─▶ H2 (test) | PostgreSQL (dev)
                                                     │  └─▶ Mapper (entidade ⇄ DTO)          ▲ Flyway só em dev
                                                     └─▶ EmailService (Thymeleaf + JavaMail/SMTP + tb_email)
                    Exceção ─▶ ControllerExceptionHandler (@RestControllerAdvice) ─▶ ProblemDetails / ValidationError
```

### 1.2 Contratos entre as camadas [CONFIRMADO, salvo indicação]

| De → Para | Contrato real | Observação |
|-----------|---------------|------------|
| Cliente → Controller | 30 endpoints em `/api/v1`; JSON; DTOs `record`; `Pageable` do Spring Data Web | 11 públicos por URL (7 de conta + 4 leituras de catálogo; 2 deles, `GET /accounts/me` e `POST /accounts/deactivate`, dependem só de `@PreAuthorize`) |
| Controller → Service | 1 chamada por endpoint; passa DTO, `id` e `Pageable`; recebe DTO de resposta ou `Page<DTO>` | Controllers não usam mapper nem repository |
| Service → Repository | Spring Data (`findById`, `getReferenceById`, derivadas, 1 JPQL, 2 nativas, 2 projeções) | Service é a fronteira transacional |
| Service → Mapper | manual (`@Component`); só services chamam | Mapper percorre coleções tardias dentro da transação |
| Validator → Repository / Security / MVC | 8 validators consultam repositories; 1 usa `AuthenticatedUserService`; 3 leem `HttpServletRequest` | Validação executa **antes** do service |
| Security → Service | `UserService.loadUserByUsername` (login); `AuthenticatedUserService` (claim `userId` → `UserRepository`) | Duas identidades distintas (ver §2) |
| Exceção → HTTP | 11 handlers; sem `ResponseEntityExceptionHandler` | Exceções do Spring MVC caem no handler genérico (500) |
| Config → Todos | perfil `test` padrão; defaults versionados; auto-configuração do Boot | Não há `@ConfigurationProperties`, `@EnableAsync` nem config de Jackson/MVC |

### 1.3 Diferença estrutural entre ambientes [CONFIRMADO]
Os testes e o perfil padrão usam **H2 + Hibernate (`create-drop`) + `import.sql`**; o perfil `dev` usa **PostgreSQL + Flyway**. O caminho Flyway/PostgreSQL **nunca** é exercitado pela suíte (sem Testcontainers; sem `src/test/resources`; perfil `test`). O `prod` não define datasource (B-8).

---

## 2. Identidade e segurança

### 2.1 Identificadores [CONFIRMADO]

| Identificador | O que é | Origem | Tipo |
|---------------|---------|--------|------|
| `User.id` | chave primária do usuário de negócio | banco (IDENTITY) | `Long` |
| **`userId`** (claim) | `User.id` no instante do login | `CustomPasswordAuthenticationProvider` → `tokenCustomizer` | `Long` |
| `username` (claim) | e-mail **digitado** no login (não o gravado) | parâmetro `username` | `String` |
| **`sub`** | nome do principal do token do cliente = **id do cliente OAuth2** | `JwtGenerator` | `String` |
| **`jti`** | UUID do JWT; exposto por `Jwt.getId()` | `JwtGenerator` | `String` |
| `authorities` (claim) | nomes das roles (`ROLE_ADMIN`, `ROLE_OPERATOR`) no login | `tokenCustomizer` | lista de `String` |
| `scope` | interseção authorities × `{read, write}` = vazia; claim ausente | `JwtGenerator` | — |
| `principalName` da autorização OAuth2 | id do cliente (não do usuário) | provider | `String` |
| `authentication.getName()` | `sub` (cliente) | `JwtAuthenticationToken` | `String` |
| `authentication.principal` | o próprio `Jwt` | `JwtAuthenticationToken` | `Jwt` |
| e-mail (`User.email`) | `username` do login; chave de recuperação/reenvio | banco | `String` |

**Respostas às perguntas 1–8:**

1. **Usuário de negócio:** `User.id`, transportado no claim **`userId`** (CONFIRMADO).
2. **JWT:** `jti` (CONFIRMADO).
3. **Cliente OAuth2:** `sub` e o `principalName` da autorização (CONFIRMADO; `sub` = cliente é INFERÊNCIA FORTE sobre o valor, confirmada em bytecode a origem `principal.getName()`).
4. **Quem usa o quê:**

| Camada | Identificador usado |
|--------|---------------------|
| Login (`UserService.loadUserByUsername` + provider) | e-mail digitado → `User` parcial (`id`, hash, `active`, roles) |
| Emissão de claims | `User.id` → `userId`; e-mail digitado → `username`; roles → `authorities` |
| Resource Server (`JwtAuthenticationConverter`) | claim `authorities` (prefixo vazio) |
| `@PreAuthorize("hasRole(...)")` | authorities do JWT |
| `@PreAuthorize` em `GET /users/{id}` | `@authenticatedUserService.isCurrentUser(#id)` → claim **`userId`** + `findById` |
| `@PreAuthorize` em `PUT /users/{id}` | `#id == authentication.principal.id` → **`Jwt.getId()` = `jti`** |
| `AccountService` ("me") e `UniqueEmailForAuthenticatedUserValidator` | claim **`userId`** → `UserRepository.findById` |
| Refresh token | autorização em memória; `principalName` = cliente |
| Logs / auditoria | nenhum identificador de usuário é registrado por controllers (só e-mail em alguns INFO) |

5. **Confusão de identificadores:** **sim, em um ponto — `PUT /users/{id}`** compara `Long #id` com `jti` (String). É a única regra que usa `principal.id`; todo o resto usa `userId` (CONFIRMADO por leitura e bytecode). Fora dele, `sub` e `username` **não** são usados para identificar usuário; `username` (digitado) pode diferir do e-mail gravado quando a caixa/espaços diferem, mas nenhum código de produção o lê (CONFIRMADO).
6. **`principal.id` × `userId`:** **semanticamente incompatíveis** (token × usuário) (CONFIRMADO). Efeito: `OPERATOR` provavelmente **não** passa a cláusula (INFERÊNCIA FORTE); a resposta HTTP exata é HIPÓTESE; `ADMIN` passa por `hasRole('ADMIN')` (curto-circuito do `OR`).
7. **Fluxo de OPERATOR em `/users/{id}`:** `GET /users/{id}` — coerente (`isCurrentUser` por `userId`; validado por `ResourceServerAuthorizationIT` verde no baseline). `PUT /users/{id}` — **incoerente** com o `GET` e com o `@Operation` ("ADMIN ou OPERATOR para o próprio usuário"): o `OPERATOR` provavelmente é negado. Como existe `PUT /accounts/me` (identidade por `userId`), a cláusula do `OPERATOR` no `PUT /users/{id}` pode ser vestigial — **QUESTÃO DE CONTRATO** (intenção).
8. **Roles × authorities × scopes:** `role` e `authority` são **o mesmo texto** (`ROLE_ADMIN` gravado no banco; `hasRole('ADMIN')` acrescenta `ROLE_`); **scopes** (`read`/`write`) existem no cliente registrado mas são **nominais**: a autorização nunca os usa e, para as roles atuais, o token não carrega `scope` (CONFIRMADO/INFERÊNCIA FORTE). Portanto os três conceitos **não** se sobrepõem funcionalmente: só `authorities` decide acesso.

### 2.2 Ciclo de vida: o que acontece quando o estado do usuário muda

Base: o `JwtDecoder` valida assinatura/expiração e **não consulta o banco**; as authorities são um *snapshot* do login; o Authorization Server usa `InMemoryOAuth2AuthorizationService` e o provider padrão de refresh não relê o usuário; não há código de revogação/logout (B-6).

| Evento | Novo login | Access token já emitido (até `exp`, padrão 24 h) | Refresh token (30 d, rotação) | Endpoints "me" | Classificação |
|--------|-----------|--------------------------------------------------|-------------------------------|----------------|---------------|
| **Usuário desativado** (`PATCH /users/{id}/deactivate`) | recusado (`isEnabled()`) — CONFIRMADO | continua aceito e com as mesmas authorities (nenhum código verifica `active` no decoder) | continua emitindo novos access tokens (provider padrão não relê o usuário) | `AuthenticatedUserService` **não verifica `active`** ⇒ continuam funcionando — CONFIRMADO | **CONFIRMADO** (código) + **INFERÊNCIA FORTE** (refresh) |
| **Senha alterada** (`PATCH /me/password`, `reset-password`) | exige a nova senha | continua válido | continua válido (nada invalida a autorização) | inalterado | **INFERÊNCIA FORTE** |
| **Roles alteradas** (`PUT /users/{id}`) | novas roles | authorities antigas até `exp` | novos access tokens reemitem as authorities **do login** (o `tokenCustomizer` lê `AuthenticatedUser` guardado na autorização) | inalterado | **INFERÊNCIA FORTE** |
| **Usuário excluído** | falha (`invalid_grant`) | `@PreAuthorize` por role continua passando; `/me` falha com `AuthenticatedUserNotFoundException` → 401 (usuário não achado) | provavelmente continua emitindo tokens | 401 | **INFERÊNCIA FORTE** |
| **Reinício da aplicação** | funciona | **todos** os tokens deixam de valer (chave RSA e `kid` novos) | perdidos (autorizações em memória) | — | **INFERÊNCIA FORTE** |
| **Múltiplas instâncias** | cada instância tem chave e autorizações próprias | tokens de uma não valem em outra | idem | — | **INFERÊNCIA FORTE** |

Nenhuma correção é proposta; cada linha depende de decisões da §8 (revogação, TTL, política de desativação).

### 2.3 Achados de identidade/segurança de contrato adicionais

- **Cadastro público concede privilégio de escrita no catálogo** — `POST /accounts/register` cria usuário com **`ROLE_OPERATOR`** (inativo); após ativar por e-mail, o usuário passa em `hasRole('ADMIN') or hasRole('OPERATOR')`, isto é, pode criar/atualizar/ativar/desativar/excluir **produtos e categorias**. CONFIRMADO no código (`AccountService.register`, regras em `Category/ProductController`); a **intenção** é QUESTÃO DE CONTRATO. A única barreira antes de virar `OPERATOR` é a posse de um e-mail com MX válido e o clique no link.
- **Enumeração de e-mails:** `POST /register` (público) falha com 422 e a mensagem de e-mail já cadastrado (`@UniqueEmail`), enquanto `password-recovery` e `resend-activation` respondem sempre 204 — políticas opostas para o mesmo tipo de dado. CONFIRMADO (código/handler); QUESTÃO DE CONTRATO. Diferença de tempo de resposta no login (senha só é conferida se o usuário existe): HIPÓTESE.
- **Sem limitação de taxa** em endpoints públicos que fazem DNS, banco e SMTP (`register`, `resend-activation`, `password-recovery`): CONFIRMADO (ausência no código/configuração); impacto INFERÊNCIA FORTE (custo por requisição; envio de e-mails a terceiros).
- **Segredo de cliente e senha SMTP com default versionado** e perfil padrão `test` com console H2 (B-8): CONFIRMADO; QUESTÃO DE CONTRATO (política de defaults).

---

## 3. Fluxos ponta a ponta

Modelo pedido:

```
HTTP → Controller → Argument resolution/Validation → Authorization → Service → Transaction → Repository → Entity/Projection → Mapper → DTO → HTTP
```

**Desvios do modelo que valem para todos os fluxos** [CONFIRMADO/INFERÊNCIA FORTE]:
1. Antes do controller há **três cadeias de segurança** e o CORS; a autorização por **URL** acontece antes de tudo; a autorização por **método** (`@PreAuthorize`) acontece **depois** da validação (INFERÊNCIA FORTE — ordem do Spring MVC: argumentos → proxy de método).
2. A transação abre no **service**, não no controller; validators e `@PreAuthorize` (via `AuthenticatedUserService`) consultam o banco **fora** de qualquer transação de negócio.
3. Mapper e conversão para DTO ocorrem **dentro** da transação do service (por isso `open-in-view=false` não quebra as respostas).
4. Erros não seguem uma única saída: filtro de segurança (401 sem corpo padrão), Authorization Server (JSON OAuth2), `ControllerExceptionHandler` (`ProblemDetails`).

### 3.1 Catálogo

| Fluxo | Acesso (URL / método) | Argumentos e validação | Service / transação | Repository | Resposta e desvios |
|-------|----------------------|------------------------|---------------------|------------|--------------------|
| **`GET /products`** | pública / nenhuma | `Pageable`; `name` (padrão `""`), `categoryIds` (`String`, padrão `"0"`); **sem** validação | `findAllPaged` (RO) | `searchProducts` (nativa, projeção `ProductProjection`) → `searchProductsWithCategories` (JPQL `JOIN FETCH`) → `IdentifiableUtils.reorderByReference` | `Page<ProductResponse>` (`PageImpl`) 200. **Desvios:** duas consultas + contagem; `INNER JOIN` exclui produtos sem categoria; `active` ignorado; `categoryIds` não numérico ⇒ `NumberFormatException` ⇒ 500; segunda consulta roda mesmo com lista vazia (HIPÓTESE no PostgreSQL); `sort` só por `id`/`name` (HIPÓTESE de erro para outras colunas) |
| **`POST /products`** | autenticada / ADMIN ou OPERATOR | `@Valid ProductCreateRequest` + `ProductCreateValidator` (nome único, `categoryIds` existentes: N consultas) | `create` (RW): mapper → `active=true` → `syncCategories` (`findAllById`, compara contagem) → `save` (IDENTITY ⇒ `INSERT` imediato) → `toResponse` | `ProductRepository`, `CategoryRepository` | 201 + `Location`. **Desvios:** `date` validado e **descartado**; preço opcional; validators consultam banco **antes** do `@PreAuthorize` |
| **`PUT /products/{id}`** | autenticada / ADMIN ou OPERATOR | `Long id`; `@Valid ProductUpdateRequest` + `ProductUpdateValidator` (lê `id` de `HttpServletRequest`) | `update` (RW): `getReferenceById` em `try` → `updateEntity` (só campos não nulos) → `categoryIds` (nulo mantém; vazio limpa) → `save` | idem | 200 `ProductResponse`; 404 por `EntityNotFoundException`. **Desvio:** `PATCH` no mesmo caminho ⇒ 405 ⇒ **500** (handler genérico) |
| **`PATCH /products/{id}/activate` / `deactivate`** | autenticada / ADMIN ou OPERATOR | só `id` | `activate/deactivate` (RW): `findById` → altera flag; **sem `save`** (dirty checking) | `ProductRepository.findById` | 204; 404 se ausente. **Desvio:** o flag não influencia nenhuma consulta |
| **`GET /categories`** | pública / nenhuma | `Pageable`, `name` opcional | `search` (RO): vazio ⇒ `findAll`; senão `findByNameContainingIgnoreCase` | `CategoryRepository` | `Page<CategoryResponse(id,name)>` 200; sem N+1 |
| **`PATCH /categories/{id}`** | autenticada / ADMIN ou OPERATOR | `@Valid CategoryUpdateRequest` + `CategoryUpdateValidator` | `update` (RW): `getReferenceById` em `try`; campos não nulos | idem | 200 `CategoryResponse(id, name)` (sem `description`) |

### 3.2 Usuários (todos exigem autenticação por URL)

| Fluxo | Autorização (método) | Validação | Service / transação | Repository / resposta |
|-------|----------------------|-----------|---------------------|------------------------|
| **`GET /users`** | ADMIN | `Pageable`, `firstName` | `search` (RO): `findAll` ou `findByFirstNameContainingIgnoreCase` (só `firstName`) | `Page<UserResponse>` 200; `UserMapper` percorre `roles` de cada usuário ⇒ N+1 provável (INFERÊNCIA) |
| **`POST /users`** | ADMIN | `@Valid UserCreateRequest`: `@ValidEmail` (**DNS**), `@UniqueEmail`, `@StrongPassword`, `@PasswordPersonalData`, `@ValidRoles` | `create` (RW): `findRolesByIdsOrThrow` → mapper → `encode` → `activate()` → `save` | 201 + `Location`. **Desvios:** e-mail gravado **cru**; `roleIds` vazio/nulo cria usuário **sem roles** (que não consegue autenticar); DTO logado em DEBUG com a senha |
| **`PUT /users/{id}`** | ADMIN **ou** (OPERATOR e `#id == authentication.principal.id`) | `@Valid UserUpdateRequest` + `UserUpdateValidator` (e-mail único excluindo `id`; senha sem dados pessoais) | `update` (RW): `getReferenceById`; nome/e-mail sem checar nulos; `roleIds` nulo mantém, vazio limpa; senha opcional; **não** altera `active` | 200 `UserResponse`. **Desvio:** para `OPERATOR` a cláusula compara `Long` com `jti` (§2) |
| **`PATCH /users/{id}/activate` / `deactivate`** | ADMIN | só `id` | flag idempotente; sem `save` | 204. **Desvio:** não invalida tokens (§2.2) |
| `GET /users/{id}` (extra) | ADMIN ou (OPERATOR e `isCurrentUser(#id)`) | — | `findById` (RO) | 200 `UserDetailsResponse` |

### 3.3 Conta

| Fluxo | Acesso (URL / método) | Validação | Service / transação | Efeitos e desvios |
|-------|-----------------------|-----------|---------------------|-------------------|
| **`POST /accounts/register`** | pública | `@Valid UserRegisterRequest` (mesmas regras de e-mail/senha) | `register` (RW): `ROLE_OPERATOR` → mapper → `encode` → `deactivate()` → `save` → `TokenService.createActivationToken` → **`EmailService.sendActivationEmailAsync`** → `toResponse` | 201 sem `Location`. **Desvios:** e-mail (SMTP) **dentro** da transação (§6); usuário nasce `OPERATOR` (§2.3) |
| **`GET /accounts/me`** | pública por URL **+** `isAuthenticated()` | — | `getAuthenticatedUser` (RO): claim `userId` → `findById` → DTO | 200. **Desvio:** anônimo tende a 403, não 401; não verifica `active` |
| **`PUT /accounts/me`** | autenticada por URL **+** `isAuthenticated()` (redundante) | `@Valid` + `@UniqueEmailForAuthenticatedUser` | `updateAuthenticatedUser` (RW): **único ponto que normaliza** (`trim`; e-mail `lowercase`) → `save` | 200 |
| **`PATCH /accounts/me/password`** | autenticada por URL (sem `@PreAuthorize`, sem OpenAPI) | `@Valid PasswordUpdateRequest` | `updatePassword` (RW): confirmação → senha atual → nova ≠ atual → `encode`; sem `save` | 204; `PasswordUpdateException` ⇒ 422; não invalida tokens |
| **`GET /accounts/activate?token=`** | pública | `token` obrigatório | `confirmEmail` (RW): `findAndValidateToken` → `user.activate()` → `token.disable()`; sem `save` | 204; **`GET` que altera estado**; 404/400 |
| **`POST /accounts/resend-activation`** | pública | `@Valid UserEmailRequest` | desabilita tokens antigos, cria novo, envia e-mail | 204 **sempre**; e-mail dentro da transação |
| **`POST /accounts/password-recovery`** | pública | `@Valid UserEmailRequest` | token `PASSWORD_RECOVERY` + e-mail; 204 sempre; não desabilita tokens anteriores; não checa `active` | 204 |
| **`POST /accounts/reset-password`** | pública | `@Valid PasswordResetRequest` (`password` sem `@Size`, sem dados pessoais) | `resetPassword` (RW): valida token → `encode` → `token.disable()` → `save` | 204; não interage com OAuth2 |
| **`POST /accounts/deactivate`** | pública por URL **+** `hasAnyRole('ADMIN','OPERATOR')` | — | `deactivateAccount` **sem `@Transactional`**: `UnsupportedOperationException` | **500** (autenticado); anônimo tende a 403 |

---

## 4. Validação × autorização × filtros de segurança

Ordem proposta (INFERÊNCIA FORTE, comportamento padrão do Spring; nenhuma execução): **URL (filtros) → conversão de `@PathVariable`/`@RequestParam` → desserialização do `@RequestBody` → Bean Validation (`@Valid`, com I/O) → `@PreAuthorize` → controller → service.**

| Cenário | Resultado esperado | Classificação |
|---------|-------------------|---------------|
| Anônimo em rota protegida por URL (com ou sem corpo) | 401 do filtro de bearer; **nenhuma** validação roda | INFERÊNCIA FORTE |
| Autenticado **sem** a role, corpo **inválido** | **422** (validação vem antes do `@PreAuthorize`) | INFERÊNCIA FORTE |
| Autenticado sem a role, corpo **válido** | validators executam consultas (banco/DNS) e só depois **403** | INFERÊNCIA FORTE |
| `@PathVariable` não numérico (`/products/abc`) | `MethodArgumentTypeMismatchException` antes da autorização e antes do controller ⇒ **500** (sem handler) | INFERÊNCIA FORTE |
| `@RequestParam` inválido em rota pública (`categoryIds=abc`) | `NumberFormatException` no service ⇒ **500** | CONFIRMADO (caminho) + INFERÊNCIA FORTE (HTTP) |
| Corpo ausente/JSON ilegível | `HttpMessageNotReadableException` ⇒ **500** | INFERÊNCIA FORTE |
| Autenticado, `Content-Type` inválido | 415 ⇒ **500** | INFERÊNCIA FORTE |
| Anônimo em URL pública + método protegido (`/accounts/me`, `/accounts/deactivate`) | `AccessDeniedException` capturada pelo advice ⇒ **403** (não 401) | INFERÊNCIA FORTE |
| Anônimo em rota pública com corpo inválido (`register`) | 422 com `fieldErrors`, após DNS/banco | INFERÊNCIA FORTE |
| Autenticado, id de `PUT` inexistente | `@Valid` (unicidade ignora ausência) → service → 404 | INFERÊNCIA FORTE |
| Parâmetros `page`/`size`/`sort` inválidos | tratados pelo resolvedor do Spring Data Web (padrões); valores negativos/`sort` inválido | HIPÓTESE |

Nada disso está coberto por testes de integração de controller com role insuficiente + corpo inválido (candidato à §11).

---

## 5. Contrato HTTP consolidado

| # | Divergência / ponto | Fato | Classificação |
|---|---------------------|------|---------------|
| 1 | `PUT /products/{id}` × testes `PATCH` | Controller: `@PutMapping` desde `6027fc2` (2026-06-19, "synchronize API contracts"); testes com `patch(...)` datam de `6854d30` (2026-05-15) e nunca foram atualizados. Resultado observado: 500 | **CONFIRMADO** (divergência) + **QUESTÃO DE CONTRATO** (qual verbo é o desejado). Categorias usam `PATCH`; usuários, `PUT` |
| 2 | `POST /accounts/deactivate` | `UnsupportedOperationException` ⇒ 500; OpenAPI declara 204 | **CONFIRMADO** + **QUESTÃO DE CONTRATO** (implementar ou remover) |
| 3 | 405 → 500 | Sem `ResponseEntityExceptionHandler`; log do baseline mostra `HttpRequestMethodNotSupportedException` seguido de `Unexpected error` | **CONFIRMADO** (por execução no baseline) |
| 4 | 415, JSON inválido, parâmetro ausente, tipo de argumento → 500 | Mesma causa (`handleGeneric`) | **INFERÊNCIA FORTE** |
| 5 | 401 × 403 | 401 do filtro (sem `ProblemDetails`); 403 via advice; anônimo em "URL pública + método" ⇒ 403 | **INFERÊNCIA FORTE** + **QUESTÃO DE CONTRATO** (política) |
| 6 | Formatos de erro | quatro formatos: `ProblemDetails`, `ValidationError` (422), JSON OAuth2 (`/oauth2/token`), resposta do filtro bearer (cabeçalho, sem corpo) | **CONFIRMADO** (3 primeiros) / **INFERÊNCIA** (4º) |
| 7 | `ProblemDetails` próprio (`timestamp,status,code,error,message,path`) ≠ RFC 7807 / `ProblemDetail` do Spring | classe própria | **CONFIRMADO** + **QUESTÃO DE CONTRATO** |
| 8 | `ValidationError` | uma entrada por violação de campo; sem valor rejeitado; erros globais ignorados (nenhum validator os produz) | **CONFIRMADO** |
| 9 | `GET /accounts/activate` altera estado | `GET` com efeito colateral (ativa conta, desabilita token) | **CONFIRMADO** + **QUESTÃO DE CONTRATO** |
| 10 | Status de integridade | excluir **produto** relacionado ⇒ `DatabaseException` ⇒ **400**; excluir **categoria** com produtos ⇒ violação de FK no commit ⇒ `DataIntegrityViolationException` ⇒ **409** | **CONFIRMADO** (mapeamentos) + **INFERÊNCIA FORTE** (momento) + **QUESTÃO DE CONTRATO** (uniformidade) |
| 11 | `Location` | só nos 3 `POST` de criação; `register` (201) não envia; `Location` não é exposto ao CORS | **CONFIRMADO** |
| 12 | Respostas de criação/atualização de categoria e produto | `CategoryResponse(id, name)` (sem `description`/`active`, desde `f8ceb98`); `ProductResponse` sem `date` (desde `6027fc2`) | **CONFIRMADO** + **QUESTÃO DE CONTRATO** (campos esperados; testes esperam `description` e `date`) |
| 13 | `GET /products` usa `findAllPaged`; teste unitário mocka `search` | `findAllPaged` no controller desde `e82cee2` (2026-06-02); teste mocka `search` desde `6854d30` | **CONFIRMADO** |
| 14 | OpenAPI × comportamento | GET de catálogo marcados como protegidos mas públicos; `PATCH /me/password` sem anotação; `deactivate` 204×500; códigos 400 declarados e não produzidos; `PUT /users/{id}` promete OPERATOR | **CONFIRMADO** (B-7 §16) |
| 15 | Serialização de `Page` | `PageImpl` direto; a biblioteca emite aviso de estrutura instável | **CONFIRMADO** (aviso) / estrutura JSON **HIPÓTESE** |
| 16 | Endpoints públicos | 11 endpoints públicos por URL (7 de conta + 4 de catálogo), dos quais `GET /accounts/me` e `POST /deactivate` só são protegidos por método | **CONFIRMADO** |

---

## 6. Transações e persistência

### 6.1 Onde há e onde não há transação [CONFIRMADO]
- **Com transação:** todos os métodos de `Product/Category/User/AccountService` (RO nas leituras) e `TokenService` (classe inteira).
- **Sem transação:** `UserService.loadUserByUsername`, `AccountService.deactivateAccount`, **todo o `EmailService`**, `AuthenticatedUserService` (usa a transação própria dos métodos de repositório), validators e `@PreAuthorize`.
- **Sem efeito prático:** `@Transactional(readOnly = true)` em `UserService.findEntityById`, método **privado** (CONFIRMADO: método privado; INFERÊNCIA FORTE: proxy não intercepta).
- Um único `JpaTransactionManager` auto-configurado; sem timeout/isolamento; rollback só para exceções não verificadas (padrões).

### 6.2 Exceções que só aparecem no commit [INFERÊNCIA FORTE]
Com IDENTITY, o `INSERT` de `save(novaEntidade)` é imediato — violações de `UNIQUE` na **criação** surgem dentro do método (a checagem prévia por validator é uma otimização, não garantia; B-5). Em **atualizações e exclusões**, o SQL é emitido no *flush* do commit, **depois** do retorno do método:

| Operação | Risco no commit | Tratamento |
|----------|-----------------|-----------|
| `CategoryService.delete` com produtos vinculados | violação de FK | nenhuma no service ⇒ `DataIntegrityViolationException` ⇒ handler **409** |
| `ProductService.delete` | `try/catch` em torno de `delete` | pode **não** interceptar a violação de commit (HIPÓTESE); `DatabaseException` ⇒ 400 |
| `UserService.delete` | FKs de `tb_token`/`tb_user_role` tratadas por cascade/JPA | sem tratamento; não impede excluir o próprio usuário nem o último administrador (CONFIRMADO: ausência de código) |
| `update` de nome/e-mail duplicados por corrida | `UNIQUE` no flush | ⇒ 409 (validação prévia não é atômica) |
| `activate/deactivate/confirmEmail/updatePassword` | dirty checking sem `save` | dependem da transação ativa; falha só no commit |

### 6.3 Carregamento tardio com `open-in-view=false` [CONFIRMADO/INFERÊNCIA FORTE]
Mapper e DTOs são montados dentro das transações dos services; `AccountService.confirmEmail/resetPassword` acessam `token.getUser()` (LAZY) dentro da transação. `AuthenticatedUserService`, o validator e o SpEL só usam `getId()` do `User` devolvido (detached) — seguro. Não foi encontrado acesso tardio fora de transação (leitura de código; execução não realizada).

### 6.4 N+1 e consultas [CONFIRMADO caminho / INFERÊNCIA FORTE efeito]
| Ponto | Padrão | Alcançável por endpoint? |
|-------|--------|--------------------------|
| `UserService.search` → `UserMapper.toResponse` percorre `roles` | 1 + N | **sim** (`GET /users`) |
| `ProductService.findById` → categorias tardias | 1 consulta extra por chamada (não é N+1) | sim |
| `ProductService.search` → categorias por produto | 1 + N | **não** (sem chamadores) |
| `AccountService.getAuthenticatedUser` → roles | 1 consulta extra | sim |
| `findAllPaged` | 2–3 consultas fixas (página, contagem, `JOIN FETCH`) | sim, sem N+1 |

### 6.5 Persistência estrutural [CONFIRMADO]
Produtos sem categoria não aparecem na listagem (`INNER JOIN`); `active` de catálogo nunca filtra nada (nem `GET /products/{id}`); usuários sem roles não autenticam (`INNER JOIN`) mas podem ser criados; `Role.authority` sem `UNIQUE`; `price` é `float(53)`; sem índices além de PK/`UNIQUE`; `LOWER(name) LIKE '%…%'` sem índice; sem `ddl-auto=validate`; dados semeados dependem de ids IDENTITY fixos (V102/V105).

---

## 7. Async + Mail + Transação

| Pergunta | Resposta |
|----------|----------|
| 1. Comportamento **garantido pela configuração** | Existem 2 métodos `@Async` e **nenhum `@EnableAsync`/executor** (CONFIRMADO). O Spring **não** processa `@Async` sem `@EnableAsync` — portanto a configuração **não garante** execução assíncrona. `spring.mail.test-connection=true` exige SMTP acessível na subida (bytecode do Boot). |
| 2. O que é **inferência** | Execução síncrona na thread da requisição (INFERÊNCIA FORTE); envio SMTP bloqueando a resposta; e-mail enviado **antes do commit**; falha do teste de conexão derrubaria o contexto sem credenciais válidas (INFERÊNCIA FORTE; o baseline passou provavelmente por credenciais locais — HIPÓTESE). |
| 3. Efeitos colaterais possíveis | (a) requisição fica presa ao SMTP: o projeto não define `mail.smtp.connectiontimeout`/`timeout` (CONFIRMADO) e os padrões do JavaMail são "infinito" (INFERÊNCIA FORTE), mantendo uma conexão do pool (Hikari padrão) aberta durante a transação; (b) usuário criado **sem e-mail** quando o SMTP falha: `sendActivationEmailAsync` captura a exceção, devolve `failedFuture` **descartado** — sem retry e sem `status=ERROR` (CONFIRMADO); (c) se o commit falhar depois do envio, o e-mail leva um token que não existe (INFERÊNCIA FORTE); (d) se `emailRepository.save` (participando da transação) falhar e a exceção for engolida, a transação pode ficar marcada como *rollback-only* e o `register` falhar no commit com o e-mail já enviado (HIPÓTESE); (e) `tb_email.status` é sempre `PENDING` e `content` guarda só o assunto (CONFIRMADO). |
| 4. Dentro ou fora da fronteira transacional? | **Dentro** (CONFIRMADO a chamada; INFERÊNCIA FORTE porque síncrona). `EmailService` não é transacional; `TokenService` e `EmailRepository.save` participam da transação de `AccountService`. |
| 5. Acoplamento persistência × SMTP? | **Sim**: a disponibilidade e a latência do SMTP entram no caminho de `register`, `resend-activation`, `password-recovery`, e no startup (`test-connection`) — CONFIRMADO o caminho; efeito INFERÊNCIA FORTE. |
| 6. Pontos para teste posterior | Thread do envio; comportamento com SMTP indisponível/lento; rollback-only; e-mail órfão; `test-connection` sem credenciais; render dos templates (`${texto}` ausente na redefinição). |

---

## 8. Profiles e ambiente

| Aspecto | `test` (padrão) | `dev` | `prod` | Classificação |
|---------|-----------------|-------|--------|---------------|
| Banco | H2 em memória (Hibernate `create-drop` + `import.sql`) | PostgreSQL + Flyway | **não definido**; H2 no classpath | CONFIRMADO; `create-drop` INFERÊNCIA FORTE |
| Se subir sem variáveis | ok (H2) | exige `POSTGRES_DATASOURCE_*` | tende a H2 embarcado + Flyway padrão com dados semeados | prod: **INFERÊNCIA FORTE**; efeito real **HIPÓTESE** |
| H2 console | habilitado (`/h2-console`), cadeia sem autenticação | não (devtools pode habilitar) | não | CONFIRMADO / INFERÊNCIA (devtools) |
| Mail | `test-connection=true`, defaults do Gmail | idem | idem | CONFIRMADO |
| Springdoc | caminhos liberados na segurança | caminhos **não** liberados (exceto `/swagger-ui/**`) | padrões do springdoc; JSON protegido | INFERÊNCIA FORTE |
| Logging | app `DEBUG`, MVC `TRACE`, bind `TRACE` | app `DEBUG`, SQL `DEBUG` (bind ineficaz) | app `INFO`, SQL `OFF` | CONFIRMADO config; efeito INFERÊNCIA FORTE |
| Segredos | defaults versionados (cliente OAuth2, SMTP) | idem; banco sem default | idem | CONFIRMADO |

**Coerência arquitetural** (fatos; nenhum é chamado de "inválido"): o perfil **padrão** é o mais permissivo e o menos parecido com `prod`; **nenhuma** configuração automatizada exercita PostgreSQL/Flyway; `prod` depende de configuração externa não versionada (QUESTÃO DE CONTRATO: política de perfis); a fonte de dados de referência está triplicada (`import.sql`, V100–V105, `create.sql`), com `create.sql` sem uso (CONFIRMADO); H2 é dependência de runtime (não de teste) em todos os perfis (CONFIRMADO).

---

## 9. Segurança operacional

| Tema | Implementado pela aplicação | Delegado ao Spring Authorization Server | Inferência | Hipótese |
|------|-----------------------------|----------------------------------------|------------|----------|
| Chave RSA | gerada em `jwkSource()` (2048 bits, `kid` = UUID), **uma** chave, sem persistência (CONFIRMADO) | — | reinício invalida todos os tokens; várias instâncias não interoperam | — |
| TTL access | `security.jwt.duration` (padrão 86400 s; `Integer`) | — | — | — |
| Refresh token | grant habilitado, 30 dias fixo, `reuseRefreshTokens(false)` (rotação) | emissão/rotação/validação | não relê usuário; não revoga em mudança de senha/desativação | comportamento real precisa de execução |
| Armazenamento das autorizações | `InMemoryOAuth2AuthorizationService` | — | **crescimento sem limite:** o mapa das autorizações com access token é um `ConcurrentHashMap` sem expurgo (bytecode 1.5.6; o limite de 100 vale só para autorizações "não completas"); uma entrada por login (INFERÊNCIA FORTE) | consumo de memória real |
| Revogação | nenhum código (sem logout/`revoke`) | endpoints `/oauth2/revoke` e `/oauth2/introspect` existem por padrão para o cliente autenticado | — | uso real |
| `kid`/rotação de chaves | um `kid` por processo, sem rotação | — | — | — |
| Validação do JWT | `JwtDecoder` único; sem emissor/audiência; sem consulta ao banco | — | token válido mesmo após desativação/exclusão do usuário | — |
| Cliente | id/segredo por propriedade (com defaults); segredo codificado com BCrypt na inicialização; autenticação do cliente padrão | — | segredo default previsível se não sobrescrito | — |

Nenhuma dessas linhas foi executada.

---

## 10. Logging e exposição de dados

| Dado exposto | `test` | `dev` | `prod` | Evidência / classificação |
|--------------|:------:|:-----:|:------:|---------------------------|
| **Senha em texto** (DEBUG do DTO em `UserController`) | **sim** | **sim** | não (nível `INFO`) | CONFIRMADO (código + níveis); baseline mostrou `UserCreateRequest[…password=…]` |
| Senha em texto via `TRACE` de `…mvc.method.annotation` (leitura/escrita do corpo) | provável | não | não | INFERÊNCIA FORTE |
| **Hash de senha, valores de token (ativação/recuperação), e-mails** via bind JDBC | provável (`org.hibernate.orm.jdbc.bind=TRACE`) | **não** (logger `…descriptor.sql.BasicBinder` não existe no Hibernate 6.6.45) | não | CONFIRMADO (bytecode) + INFERÊNCIA FORTE (efeito) |
| SQL (sem valores) | sim (`show-sql` + `SQL=DEBUG`) | sim | não (`OFF`) | CONFIRMADO |
| **E-mails de usuários** (INFO em `EmailService` e `AccountController`) | sim | sim | **sim** (INFO) | CONFIRMADO (código); PII em arquivo de log |
| Stack traces (`ERROR`) com texto de exceção/SQL | sim | sim | **sim** (arquivo) | CONFIRMADO |
| Token OAuth2 / JWT / cabeçalho `Authorization` | não | não | não | INFERÊNCIA FORTE (nenhum código registra; `spring.mvc.log-request-details` não configurado) |
| Corpo de erro de validação | só nome do campo e mensagem (não o valor) | idem | idem | CONFIRMADO |

Refinamento do B-7: o bind de parâmetros só é efetivo em `test`.

---

## 11. Contratos de documentação (somente divergências sustentadas pelo código)

| Documento / anotação | Divergência | Classificação |
|----------------------|-------------|---------------|
| OpenAPI × `ResourceServerConfig` | `GET` de catálogo marcados com Bearer e 401/403, mas públicos e sem `@PreAuthorize` | CONFIRMADO |
| OpenAPI de `PUT /users/{id}` | "ADMIN ou OPERATOR para o próprio usuário" × SpEL com `principal.id` (`jti`) | CONFIRMADO (texto) + INFERÊNCIA FORTE (efeito) |
| OpenAPI de `deactivate` | 204 × `UnsupportedOperationException` | CONFIRMADO |
| OpenAPI de criação/atualização | 400 declarado e não produzido | CONFIRMADO |
| OpenAPI de `/accounts/activate` | não declara 404 | CONFIRMADO |
| `PATCH /me/password` | sem `@Operation`/`@ApiResponses` | CONFIRMADO |
| Licença | Apache 2.0 (OpenAPI) × MIT (`pom.xml`) | CONFIRMADO |
| E-mail de contato | OpenAPI `…,17@…` × `pom.xml` `…@.17…`: **ambos malformados e diferentes** | CONFIRMADO |
| Springdoc por perfil | caminhos liberados só para `test`; nenhum perfil o desabilita | CONFIRMADO |
| `additional-spring-configuration-metadata.json` | `security.jwt.duration` como `Long` × código `Integer`; ignora `frontend.url`, `backend.url`, `account.*` | CONFIRMADO |
| `ProblemDetails` × RFC 7807 | classe própria; campos diferentes | CONFIRMADO (JavaDoc atual já descreve corretamente) |
| Nome do produto | `DSCatalog` (`spring.application.name`, `/docs-dscatalog`) × `ASJCatalog` (artefato, OpenAPI) | CONFIRMADO |
| `messages_en/es` × `pt_BR` | chave `error.auth.userId.claim.notFound` só em `pt_BR` | CONFIRMADO; fallback para português INFERÊNCIA FORTE |
| Template de redefinição × `EmailService` | `${texto}` referenciado e não fornecido; `titulo`/`token` fornecidos e não usados; `reactivate_*` sem consumidor | CONFIRMADO |

---

## 12. Persistência e consultas (comportamento estrutural)

| Caso | Comportamento | Classificação |
|------|---------------|---------------|
| Lista vazia em `categoryIds` | o service só cria lista vazia para `"0"`; passa ao `IN :categoryIds` (H2: retornou tudo no baseline); PostgreSQL não verificado | CONFIRMADO (H2, por execução) / **HIPÓTESE** (PostgreSQL) |
| Página vazia | `searchProductsWithCategories` é chamada com lista vazia (sem guarda) | CONFIRMADO (chamada) / **HIPÓTESE** (efeito) |
| `sort` por coluna não selecionada | o `SELECT *` externo só expõe `id`/`name` | CONFIRMADO (forma da SQL) / **HIPÓTESE** (erro em execução) |
| Produto sem categoria | invisível em `GET /products`, visível no service `search` (sem chamadores) | CONFIRMADO |
| Usuário sem roles | pode ser criado; não autentica (`INNER JOIN`) | CONFIRMADO (caminho) / INFERÊNCIA FORTE |
| Caixa do e-mail | validators e `existsBy…IgnoreCase` ignoram caixa; `findByEmail` e `searchUserAndRolesByEmail` são exatos; e-mail gravado cru (exceto `PUT /me`) | CONFIRMADO |
| Normalização de nomes | unicidade por `trim+lowercase`, gravação crua, `UNIQUE` do banco sensível à caixa | CONFIRMADO |
| Curingas em busca | nativa concatena o termo cru (`%`/`_` viram curingas); derivadas escapam (framework) | CONFIRMADO / INFERÊNCIA FORTE |
| `UserDetailsProjection` | uma linha por role; `User` parcial nunca persistido | CONFIRMADO |
| `IdentifiableUtils.reorderByReference` | descarta ids sem produto | CONFIRMADO |
| Entidades × migrations | conferem (tabelas/colunas/tamanhos); sem validação automática | CONFIRMADO (leitura) / HIPÓTESE (execução no PostgreSQL) |

---

## 13. Modelo de erro

```
Exceção ─▶ [cadeia de filtros] ─▶ 401 (bearer, sem corpo padrão) | JSON OAuth2 (/oauth2/**)
        └▶ [DispatcherServlet] ─▶ ControllerExceptionHandler ─▶ ProblemDetails | ValidationError ─▶ HTTP
```

| Exceção | Tratada? | HTTP | Observação |
|---------|:--------:|:----:|------------|
| `MethodArgumentNotValidException` | sim | 422 | `ValidationError` |
| `ResourceNotFoundException`, `NoResourceFoundException` | sim | 404 | |
| `DatabaseException` | sim | 400 | só `ProductService.delete` |
| `DataIntegrityViolationException` | sim | 409 | |
| `AccessDeniedException` | sim | 403 | só o que passa pelo advice |
| `InvalidTokenException` | sim | 400 | |
| `AuthenticatedUserNotFoundException` | sim | 401 | |
| `PasswordUpdateException` | sim | 422 | |
| `DisabledException` | sim | 403 | provavelmente inalcançável |
| `Exception` | sim (genérico) | **500** | absorve tudo abaixo |
| `HttpRequestMethodNotSupportedException` (405) | **não** | 500 | CONFIRMADO no baseline |
| `HttpMediaTypeNotSupportedException` (415), `HttpMessageNotReadableException`, `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException` | **não** | 500 | INFERÊNCIA FORTE |
| `ConstraintViolationException`, `NumberFormatException`, `IllegalStateException` (ex.: `ROLE_OPERATOR` ausente), `UnsupportedOperationException` | **não** | 500 | CONFIRMADO (ausência de handler) |
| `AuthenticationException` / erros de JWT | não (filtro) | 401 | INFERÊNCIA |
| `OAuth2AuthenticationException` | não (AS) | 400/401 (`invalid_grant`, etc.) | evidência de baseline (refresh) |
| Exceção após `/error` (dispatch de erro) | — | — | HIPÓTESE: o dispatch de erro passa pela cadeia de segurança; `/error` exigiria autenticação para anônimos |

Inconsistências: 4xx transformados em 5xx (linhas em negrito); quatro formatos de corpo de erro; 401 sem `ProblemDetails`; 403 dependente de onde a exceção ocorre. Nada foi corrigido.

---

## 14. Inventário consolidado de descobertas

Categorias descritivas de área (sem prioridade): contrato, segurança, persistência, configuração, observabilidade, documentação, teste. "Fase futura": **Decisão** (contrato), **Teste** (comprovar por execução) ou **Implementação** (após decisão).

| ID | Área | Descoberta | Classificação | Evidência | Impacto | Fase futura |
|----|------|------------|---------------|-----------|---------|-------------|
| K-01 | contrato | `PUT /products/{id}` no controller × `PATCH` nos testes | CONFIRMADO + QUESTÃO DE CONTRATO | `@PutMapping` (commit `6027fc2`); `patch()` em `ProductControllerTest`/`IT` | 4 testes falham (2 unitários, 2 de integração) | Decisão → Teste |
| K-02 | contrato | `POST /accounts/deactivate` não implementado | CONFIRMADO + QUESTÃO DE CONTRATO | `UnsupportedOperationException`; OpenAPI 204 | 500 para autenticado | Decisão → Implementação |
| K-03 | contrato | 405 → 500 | CONFIRMADO | log do baseline; ausência de handler | cliente recebe 500 por método errado | Decisão → Teste |
| K-04 | contrato | 415/JSON inválido/parâmetro ausente/tipo inválido → 500 | INFERÊNCIA FORTE | `handleGeneric` genérico | idem | Teste |
| K-05 | contrato | 401 sem `ProblemDetails`; anônimo em "URL pública + método" ⇒ 403 | INFERÊNCIA FORTE + QUESTÃO DE CONTRATO | `ResourceServerConfig`; advice | contrato de erro heterogêneo | Decisão → Teste |
| K-06 | contrato | `ProblemDetails` próprio ≠ RFC 7807 | CONFIRMADO + QUESTÃO DE CONTRATO | classe `ProblemDetails` | clientes precisam do formato próprio | Decisão |
| K-07 | contrato | `GET /accounts/activate` altera estado | CONFIRMADO + QUESTÃO DE CONTRATO | `AccountController`/`AccountService.confirmEmail` | prefetch/links podem ativar contas | Decisão |
| K-08 | contrato | Integridade: produto → 400, categoria → 409 | CONFIRMADO + INFERÊNCIA FORTE | `ProductService.delete`; handlers | uniformidade | Decisão → Teste |
| K-09 | contrato | Respostas de criação/atualização sem `description` (categoria) / `date` (produto) | CONFIRMADO + QUESTÃO DE CONTRATO | `CategoryResponse`, `ProductResponse`; commits `f8ceb98`, `6027fc2` | 3 testes de integração falham | Decisão → Teste |
| K-10 | contrato | `date` é validado e descartado | CONFIRMADO | `ProductMapper.toEntity` | campo inócuo no request | Decisão |
| K-11 | contrato | `GET /products` usa `findAllPaged`; teste mocka `search` | CONFIRMADO | `e82cee2` × `6854d30` | 2 testes falham | Teste |
| K-12 | contrato | Serialização direta de `Page` | CONFIRMADO (aviso) / HIPÓTESE (estrutura) | Spring Data 3.5.10 | estabilidade do JSON | Decisão → Teste |
| K-13 | contrato | `Location` só nos 3 creates; `register` 201 sem `Location`; `Location` não exposto ao CORS | CONFIRMADO | controllers; `ResourceServerConfig` | clientes de outra origem não leem `Location` | Decisão |
| K-14 | contrato | Verbos heterogêneos (categoria `PATCH`, produto/usuário `PUT`) e semântica parcial não determinada pelo verbo | CONFIRMADO | B-4 §13 | ambiguidade PUT/PATCH | Decisão |
| S-01 | segurança | `principal.id` = `jti` em `PUT /users/{id}` | CONFIRMADO + INFERÊNCIA FORTE | bytecode `JwtClaimAccessor.getId()` | OPERATOR provavelmente negado; contradiz OpenAPI | Decisão → Teste |
| S-02 | segurança | Cadastro público → `ROLE_OPERATOR` com escrita no catálogo | CONFIRMADO + QUESTÃO DE CONTRATO | `AccountService.register`; `@PreAuthorize` | privilégio após ativar e-mail | Decisão |
| S-03 | segurança | Access token não verifica estado do usuário (`active`, exclusão, roles) | CONFIRMADO (ausência) + INFERÊNCIA FORTE | `JwtDecoder`; `AuthenticatedUserService` | acesso até `exp` | Decisão → Teste |
| S-04 | segurança | Refresh não relê usuário; nada invalida em senha/desativação | INFERÊNCIA FORTE | provider padrão; ausência de código | janela de 30 dias | Decisão → Teste |
| S-05 | segurança | Chave RSA/`kid` e autorizações em memória | CONFIRMADO | `jwkSource()` | reinício invalida tokens; sem multi-instância | Decisão |
| S-06 | segurança | Autorizações completas em `ConcurrentHashMap` sem expurgo | INFERÊNCIA FORTE | bytecode 1.5.6 | memória cresce por login | Teste |
| S-07 | segurança | Sem logout/revogação na aplicação | CONFIRMADO | busca em `src/main` | — | Decisão |
| S-08 | segurança | `AuthenticatedUserService` não verifica `active` | CONFIRMADO | código | `/me` funciona para inativo | Decisão |
| S-09 | segurança | Claim `username` digitado, sem consumidor; `sub` = cliente | CONFIRMADO | `tokenCustomizer` | identidade ambígua para integradores | Decisão |
| S-10 | segurança | Scopes nominais; token sem `scope` | CONFIRMADO / INFERÊNCIA FORTE | provider; `JwtGenerator` | — | Decisão |
| S-11 | segurança | Enumeração de e-mails via `register` (422) | CONFIRMADO + QUESTÃO DE CONTRATO | `@UniqueEmail` | divulgação de cadastros | Decisão |
| S-12 | segurança | Sem limitação de taxa nos endpoints públicos (DNS/SMTP/DB) | CONFIRMADO (ausência) + INFERÊNCIA FORTE | código/config | custo/abuso | Decisão |
| S-13 | segurança | CORS: `split(",")` sem `trim`; um único conjunto de origens | CONFIRMADO | `ResourceServerConfig` | origem com espaço não casa | Teste |
| S-14 | segurança | Console H2 no perfil padrão; devtools traz `enabled=true` | CONFIRMADO / INFERÊNCIA | `application-test.properties`; jar do devtools | exposição em execução local | Decisão |
| S-15 | segurança | Segredo do cliente e senha SMTP com default versionado | CONFIRMADO | `application.properties` | defaults previsíveis | Decisão |
| S-16 | segurança | Tempo de resposta do login difere para usuário inexistente | HIPÓTESE | provider | enumeração por tempo | Teste |
| V-01 | contrato | Validação (com I/O) antes de `@PreAuthorize` | INFERÊNCIA FORTE | ordem do Spring MVC | 422 antes de 403; DNS/banco para quem seria negado | Teste |
| V-02 | contrato | E-mail validado normalizado, gravado cru; login/recuperação exatos | CONFIRMADO + INFERÊNCIA FORTE | mappers; repositórios | falha de login por grafia; duplicatas por caixa | Decisão → Teste |
| V-03 | contrato | `@ValidEmail` faz DNS sem timeout e considera só MX | CONFIRMADO | B-5 | latência/falso negativo | Teste |
| V-04 | contrato | Regras de senha: sem tamanho em `@StrongPassword`; sem `@Size` em update/reset; dados pessoais duplicados/ausentes | CONFIRMADO | B-5 | inconsistência entre fluxos | Decisão |
| V-05 | contrato | Unicidade não atômica (validator + `UNIQUE`) | INFERÊNCIA FORTE | B-5 | corrida ⇒ 409 | Teste |
| V-06 | contrato | Validators acoplados ao nome de variável `id` e ao MVC; `FieldMessage` (web) usado por `validation` | CONFIRMADO | B-5 | silêncio se a rota mudar | Decisão |
| P-01 | persistência | E-mail SMTP dentro da transação (síncrono) | CONFIRMADO (chamada) + INFERÊNCIA FORTE | `AccountService`; ausência de `@EnableAsync` | latência e falha acopladas | Decisão → Teste |
| P-02 | persistência | Sem timeouts SMTP | CONFIRMADO (ausência) + INFERÊNCIA FORTE | `application.properties` | thread e conexão presas | Teste |
| P-03 | persistência | Falha de envio engolida; sem retry/`ERROR` | CONFIRMADO | `EmailService` | usuário sem e-mail | Decisão |
| P-04 | persistência | *Rollback-only* por `save` falho engolido | HIPÓTESE | proxies transacionais | `register` falha no commit após e-mail | Teste |
| P-05 | persistência | E-mail enviado antes do commit (órfão se falhar) | INFERÊNCIA FORTE | ordem no `register` | link para token inexistente | Teste |
| P-06 | persistência | `Email.status` sempre `PENDING` | CONFIRMADO | B-1 | log de e-mails sem estado real | Decisão |
| P-07 | persistência | Exceções de integridade só no commit em update/delete | INFERÊNCIA FORTE | JPA flush | `try/catch` de `ProductService.delete` pode não pegar | Teste |
| P-08 | persistência | N+1 em `GET /users` (roles) | INFERÊNCIA FORTE | mapper | consultas extras por página | Teste |
| P-09 | persistência | `active` de catálogo nunca filtra | CONFIRMADO + QUESTÃO DE CONTRATO | B-1/B-3 | inativos aparecem | Decisão |
| P-10 | persistência | `INNER JOIN`: produto sem categoria não listado | CONFIRMADO | `searchProducts` | listagem incompleta | Decisão |
| P-11 | persistência | Usuário sem roles: criável, não autentica | CONFIRMADO (caminho) | `UserService.create`; `INNER JOIN` | conta "morta" | Decisão |
| P-12 | persistência | Exclusão de usuário sem proteção (auto-exclusão, último admin) | CONFIRMADO (ausência) | `UserService.delete` | — | Decisão |
| P-13 | persistência | `categoryIds`/`sort`/página vazia em PostgreSQL | HIPÓTESE | B-2 | 500 potencial | Teste |
| P-14 | persistência | Sem índices; sem `ddl-auto=validate`; ids fixos nos seeds | CONFIRMADO | migrations | — | Decisão |
| P-15 | persistência | `@Transactional(readOnly)` em método privado sem efeito | CONFIRMADO + INFERÊNCIA FORTE | `UserService.findEntityById` | — | Decisão |
| P-16 | persistência | `price` como `float(53)`/`Double` e opcional | CONFIRMADO | B-1/B-4 | precisão monetária | Decisão |
| C-01 | configuração | Perfil padrão `test` | CONFIRMADO + QUESTÃO DE CONTRATO | `${APP_PROFILE:test}` | ambiente mais permissivo é o padrão | Decisão |
| C-02 | configuração | `prod` sem datasource/JPA/Flyway | CONFIRMADO; efeito INFERÊNCIA FORTE / HIPÓTESE | `application-prod.properties` | H2 embarcado + dados semeados? | Decisão → Teste |
| C-03 | configuração | `spring.mail.test-connection=true` em todos os perfis | CONFIRMADO; efeito HIPÓTESE | bytecode do Boot | contexto depende de SMTP | Decisão → Teste |
| C-04 | configuração | `@Async` sem `@EnableAsync` | CONFIRMADO; execução síncrona INFERÊNCIA FORTE | busca em `src/main` | — | Decisão → Teste |
| C-05 | configuração | Bean `localeResolver` sobrepõe `spring.web.locale*` (dev) | INFERÊNCIA FORTE | `MessageSourceConfig` | propriedade ineficaz | Teste |
| C-06 | configuração | Springdoc ativo em todos os perfis; caminhos liberados só em `test` | CONFIRMADO | properties; `ResourceServerConfig` | UI/JSON inacessíveis em `dev`/`prod` | Decisão |
| C-07 | configuração | Dev/test com esquemas diferentes (Flyway/PostgreSQL × Hibernate/H2) e `import.sql` duplicado | CONFIRMADO | B-8 | divergência não detectável pela suíte | Decisão → Teste |
| C-08 | configuração | Sem `@ConfigurationProperties`; metadados incompletos; `security.jwt.duration` `Integer` × `Long` | CONFIRMADO | B-8 | — | Decisão |
| C-09 | configuração | H2 é dependência de runtime; devtools opcional | CONFIRMADO | `pom.xml` | H2 disponível em `prod` | Decisão |
| C-10 | configuração | `backend.url` injetado e não usado; templates sem consumidor/variável ausente | CONFIRMADO | B-8 | e-mail de recuperação sem texto | Decisão |
| O-01 | observabilidade | Senha em DEBUG (`UserController`) em `dev`/`test` | CONFIRMADO | código/níveis | credencial em log | Decisão |
| O-02 | observabilidade | Bind JDBC só em `test` (logger de `dev` inexistente) | CONFIRMADO + INFERÊNCIA FORTE | bytecode Hibernate 6.6.45 | hash/tokens em log de teste | Decisão → Teste |
| O-03 | observabilidade | `TRACE` do MVC no `test` pode imprimir corpos | INFERÊNCIA FORTE | `application-test.properties` | senha em log de teste | Teste |
| O-04 | observabilidade | E-mails de usuários em logs INFO (inclui `prod`); stack traces em arquivo | CONFIRMADO | `EmailService`, `AccountController` | PII em log | Decisão |
| O-05 | observabilidade | Sem Actuator/health/métricas | CONFIRMADO | `pom.xml` | — | Decisão |
| D-01 | documentação | OpenAPI × comportamento (ver §11) | CONFIRMADO | B-7 | contrato publicado incorreto | Decisão |
| D-02 | documentação | Metadados pom × OpenAPI (licença; e-mails malformados) | CONFIRMADO | `pom.xml`, `SpringDocOpenApiConfig` | — | Decisão |
| D-03 | documentação | `UserDetailsProjection` usa `@apiNote`/`@implNote` (rejeitadas pelo `javadoc` sem `-tag`) | CONFIRMADO | B-2 §13.1 #11 | `javadoc` do JDK falha; o plugin usa `failOnError=false` | Decisão |
| T-01 | teste | 10 testes falhando (ver §16) | CONFIRMADO | baseline B-0 | — | Teste |
| T-02 | teste | Integração (`*IT`) fora do `mvn test`; sem Testcontainers | CONFIRMADO | `pom.xml`; B-0 | 5 falhas só aparecem no diagnóstico | Decisão |
| T-03 | teste | Ausência de testes diretos: `searchProducts`, `searchProductsWithCategories`, `searchUserAndRolesByEmail`, `UniqueEmailForAuthenticatedUserValidator`, mappers, `AuthorizationServerConfig` | CONFIRMADO | B-0 §12.5 | — | Teste |
| T-04 | teste | Contexto depende de SMTP/credenciais reais (`test-connection`) | INFERÊNCIA FORTE + HIPÓTESE | bytecode; variáveis definidas no ambiente local | suíte não reproduzível sem credenciais | Teste |
| T-05 | teste | `ProductTest` afirma `createdAt` sem persistir; `Product.prePersist` é privado (o de `Category` é público e testado) | CONFIRMADO | `ProductTest`, `CategoryTest`, `6027fc2` | 1 falha | Decisão → Teste |

---

## 15. Decisões que precisam ser tomadas antes de corrigir

Nenhuma é decidida aqui. Para cada uma, o que o código/histórico já mostra:

| # | Decisão | O que já se sabe | Depende de |
|---|---------|------------------|------------|
| 1 | Verbo de atualização de produto (PUT × PATCH) | Controller é `PUT` desde `6027fc2`; testes são `PATCH`; categorias usam `PATCH`, usuários `PUT`; semântica é a mesma nos três (só campos não nulos, exceto `UserMapper`) | contrato desejado; decide também os 4 testes |
| 2 | Semântica de `PUT /users/{id}` | `ADMIN` edita qualquer usuário; `OPERATOR` tem `PUT /accounts/me` | manter ou remover a cláusula de `OPERATOR` |
| 3 | Identidade para *self-update* | `userId` (usuário) × `jti` (token) × `sub` (cliente) | escolher a fonte de identidade |
| 4 | `deactivate` de conta | não implementado; OpenAPI diz 204 | implementar, remover ou documentar |
| 5 | Formato de erro | `ProblemDetails` próprio, `ValidationError`, OAuth2, resposta do filtro | adotar RFC 7807 ou manter; unificar 401/403 |
| 6 | Política 401 × 403 | anônimo em método protegido ⇒ 403 | mudança de configuração de segurança |
| 7 | Política de OpenAPI | springdoc sempre ativo; caminhos por perfil; documento diverge do código | fonte da verdade (código × anotação) |
| 8 | Política de profiles | padrão `test`; `prod` incompleto | perfil padrão; configuração externa de `prod` |
| 9 | Política de SMTP | síncrono na transação; `test-connection`; sem timeout; sem retry | habilitar `@Async`? outbox? `test-connection` por perfil? |
| 10 | JWT após reinício | chave em memória | chave persistida/externa? |
| 11 | Revogação de tokens | inexistente | estratégia de invalidação e persistência das autorizações |
| 12 | Logging de credenciais | senha em DEBUG; bind em `test` | política de log por perfil |
| 13 | Serialização de `Page` | `PageImpl` direto | `PagedModel`/DTO próprio |
| 14 | Perfil do usuário autoregistrado | `ROLE_OPERATOR` com escrita no catálogo | papel inicial |
| 15 | Semântica de `active` no catálogo e de "conta desativada" | flag não filtra; token continua válido | regras de visibilidade |
| 16 | Campos de resposta de criação/atualização | testes esperam `description` (categoria) e `date` (produto) | contrato dos DTOs; destino de `date` |
| 17 | Uniformidade de status de integridade (400 × 409) | dois caminhos distintos | mapeamento único |
| 18 | `GET` de ativação | altera estado | trocar verbo/forma |
| 19 | Fonte de `createdAt` do produto | só em `@PrePersist` (privado) | se a entidade inicializa datas; como testar |
| 20 | Normalização de e-mail/nomes | validado normalizado; gravado cru | normalizar na entrada? |

---

## 16. Os 10 testes que falham × contratos descobertos

Fonte das falhas: B-0 §14 (execução real). Classificação sem decidir a correção; evidência do histórico do git quando existe (o histórico **não** decide a intenção).

| # | Teste | Causa observada | Categoria | Evidência | Estado |
|---|-------|-----------------|-----------|-----------|--------|
| 1 | `ProductTest.productShouldInstantiateCorrectly` | exige `createdAt`/`updatedAt` não nulos após só instanciar | teste possivelmente **desalinhado** com o mecanismo de datas + **contrato** (visibilidade do `prePersist`) | `6027fc2` trocou `date` por `createdAt/updatedAt` preenchidos em `@PrePersist` **privado** e ajustou o teste na mesma alteração; `CategoryTest` passa porque chama o `prePersist` público de `Category` | QUESTÃO DE CONTRATO (T-05) |
| 2 | `ProductControllerTest.findAllShouldReturnPage` | mock de `productService.search`, controller usa `findAllPaged` ⇒ `null` ⇒ NPE ⇒ 500 | **atualização de teste** | `findAllPaged` no controller desde `e82cee2` (06-02); teste mocka `search` desde `6854d30` (05-15) | evidência forte de teste desatualizado; nenhuma decisão de contrato aparente (K-11) |
| 3 | `ProductControllerTest.findAllShouldReturnFilteredPage` | idem | **atualização de teste** | idem | idem |
| 4 | `ProductControllerTest.updateShouldReturnUpdatedProductWhenIdExists` | `PATCH` ⇒ 405 ⇒ 500 | **contrato divergente** (PUT × PATCH) | K-01 | depende da decisão #1 |
| 5 | `ProductControllerTest.updateShouldReturnNotFoundWhenIdDoesNotExist` | idem | **contrato divergente** | K-01 | depende da decisão #1 |
| 6 | `CategoryControllerIT.insertShouldCreateCategoryAndReturnCreated` | `$.description` ausente em `CategoryResponse(id,name)` | **contrato divergente** (campos de resposta) / teste desatualizado | `CategoryResponse` perdeu `description`/`active` em `f8ceb98` (06-02); IT com `$.description` datava de `6854d30`/maio | depende da decisão #16 |
| 7 | `CategoryControllerIT.updateShouldUpdateCategoryWhenIdExists` | idem | idem | idem | idem |
| 8 | `ProductControllerIT.insertShouldCreateProductWithValidData` | `$.date` ausente em `ProductResponse` | **contrato divergente** (campo `date` removido do response e descartado no mapper) | `date` removido em `6027fc2` | depende das decisões #16 (e destino de `date`) |
| 9 | `ProductControllerIT.updateShouldReturnProductResponseWhenIdExists` | `PATCH` ⇒ 500 | **contrato divergente** (PUT × PATCH) | K-01 | decisão #1 |
| 10 | `ProductControllerIT.updateShouldReturnNotFoundWhenIdDoesNotExist` | idem | **contrato divergente** | K-01 | decisão #1 |

Resumo por categoria: **atualização de teste com evidência forte** — 2 (nº 2, 3); **contrato divergente** que depende de decisão — 7 (nº 4–10); **teste × mecanismo de datas** — 1 (nº 1). **Bug em código de produção:** nenhuma das 10 falhas tem, até aqui, evidência de defeito na implementação independente de contrato. **Falha de infraestrutura/configuração:** nenhuma (o contexto sobe; dependência de SMTP é risco separado, T-04). **Sem evidência suficiente:** nenhuma das 10, mas os itens 6–8 dependem de saber quais campos o contrato quer expor. Todas foram observadas **no baseline**, antes desta fase; nada foi reexecutado.

---

## 17. Backlog para a futura fase de testes (comprovar por execução)

Nenhum cenário foi executado.

**A. Já falham (10):** ver §16.
**B. Contrato HTTP:** `PATCH /products/{id}` (405→?); 415, JSON inválido, parâmetro ausente, `id` não numérico, `categoryIds=abc`; `POST /accounts/deactivate` (autenticado e anônimo); 401 × 403 em `/accounts/me` anônimo; formato de cada erro; `Location` nos creates; `GET` de catálogo sem token; serialização e aviso de `Page`; `sort` por coluna inválida; `page`/`size` inválidos.
**C. Autorização/identidade:** `PUT /users/{id}` como ADMIN e OPERATOR (próprio e outro); `GET /users/{id}` como OPERATOR; corpo inválido + role insuficiente (422 × 403); anônimo em rotas protegidas; `OPERATOR` criando/removendo catálogo; claims do JWT (`userId`, `username`, `authorities`, `scope`, `sub`, `jti`).
**D. Ciclo de vida:** uso de access token após desativar/excluir usuário, trocar senha, mudar roles; refresh após cada evento; reinício (JWT/refresh); `/oauth2/revoke` e `/oauth2/introspect`; crescimento do mapa de autorizações.
**E. E-mail/async/transação:** thread do envio; `@Async` efetivo; SMTP indisponível/lento; rollback-only; e-mail órfão; `test-connection` sem credenciais; templates (variável `texto`; `reactivate`); status `PENDING`.
**F. Persistência:** `categoryIds` vazio, página vazia e `sort` inválido em PostgreSQL; N+1 em `GET /users`; `findAllPaged` sem categorias; exclusão de categoria/produto/usuário relacionados (momento da exceção); unicidade concorrente; usuário sem roles; caixa do e-mail no login/recuperação; entidades × migrations (`validate`); `import.sql` × V100–V105.
**G. Configuração:** perfis (`test`, `dev`, `prod` sem variáveis); Flyway em PostgreSQL; H2 console por perfil e com devtools; Springdoc por perfil; locale (`Accept-Language` en/es, chave `userId`); CORS (preflight, cabeçalhos extras, lista com espaços); `spring.web.locale` em `dev`.
**H. Logging:** senha em DEBUG; bind e TRACE do MVC no `test`; bind ausente em `dev`; e-mails em INFO; stack traces.
**I. Estrutura da suíte:** classes `*IT` fora do `mvn test`; testes ausentes listados em T-03.

---

## 18. JavaDoc/documentação claramente incorretos (apenas registro; nada corrigido)

- `UserDetailsProjection` usa `@apiNote`/`@implNote` (não reconhecidas pelo `javadoc` sem `-tag`): geram erro no `javadoc` do JDK; o `maven-javadoc-plugin` tem `failOnError=false` (D-03).
- Não foram encontrados, nos arquivos analisados, outros JavaDocs que contradigam o código; as inconsistências restantes estão em **anotações OpenAPI** (`@Operation`/`@ApiResponses`), não em JavaDoc (§11), e no `pom.xml`/`SpringDocOpenApiConfig` (licença/contato).
- O B-8 §12/§13 e o B-7 §19 refletem o refino do bind de SQL; o B-7 já contém a nota correspondente.

---

## 19. O que este documento responde

1. **Como o backend funciona como um todo:** §1 (visão, contratos) e §3 (fluxos ponta a ponta).
2. **Contratos entre camadas:** §1.2; identidade em §2; erro em §13.
3. **Divergências comprovadas:** §5 (K-01…K-14), §11, §16 e o inventário (§14) com classificação CONFIRMADO.
4. **Apenas hipóteses:** itens HIPÓTESE do inventário (S-16, P-04, P-13 e efeitos de C-02/C-03) e §17.
5. **Dependem de decisão de projeto:** §15 (20 decisões).
6. **Dependem de testes:** §17 (backlog) e §16.
7. **Próximos passos após o conhecimento:** (i) revisar este B-9 e fixar o baseline arquitetural; (ii) tomar as decisões #1, #16 e #19 (destravam os 10 testes); (iii) então analisar a camada de testes e corrigir de forma controlada; (iv) decidir o restante (§15) em ordem definida pelo projeto.

## 20. O que **não** foi feito

Nenhuma alteração em `src/main`, `src/test`, `pom.xml`, `src/main/resources`, migrations, templates, propriedades, dependências, OpenAPI, segurança ou configuração; nenhuma correção; nenhum JavaDoc adicionado; nenhum teste criado ou executado (`mvn test`, ITs); a aplicação não foi executada; nenhum commit. Os números de falhas (5 + 5) vêm do baseline do B-0.
