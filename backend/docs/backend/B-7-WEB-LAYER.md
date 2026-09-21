# ASJCatalog Backend — B-7: Camada Web

> **Fase:** B-7 — análise e documentação (JavaDoc) de `web` (controllers, tratamento de exceções, modelos de erro) e das configurações diretamente ligadas à exposição HTTP (`config/documentation`, `config/i18n`).
> **Pré-requisitos:** [B-0](B-0-BACKEND-INVENTORY.md) a [B-6](B-6-SECURITY-OAUTH2-LAYER.md).
> **Escopo alterado:** somente comentários JavaDoc nos 9 arquivos de `web` e nos 2 de `config` citados, mais esta documentação e notas pontuais nos anteriores. Nenhum endpoint, verbo, status, handler, formato de erro, CORS, OpenAPI, log ou comportamento foi alterado.

Rótulos: **[FATO]** confirmado no código/configuração; **[INFERÊNCIA]** comportamento do framework (Spring MVC/Security/Data) deduzido sem execução; **[HIPÓTESE]** precisa de execução; **[Baseline]** evidência já registrada no B-0. Nenhum teste foi executado.

---

## 1. Objetivo
Fechar o fluxo HTTP: `HTTP → Security → (binding/Bean Validation) → Controller → Service → Mapper → Response` e `Exception → ControllerExceptionHandler → ProblemDetails → HTTP`.

## 2. Escopo
`web/controller` (4), `web/exception/{handler,response,enums}` (5), `config/documentation/SpringDocOpenApiConfig`, `config/i18n/MessageSourceConfig`. Leitura auxiliar: `ResourceServerConfig` (B-6), `messages_*.properties`, `application*.properties` (springdoc), `OAuth2TokenIT`/`ResourceServerAuthorizationIT` não foram relidos. Não existe `WebMvcConfigurer`, `@EnableWebMvc`, `ObjectMapper` customizado nem `@EnableSpringDataWebSupport` no projeto [FATO].

## 3. Inventário [FATO]

| Componente | Tipo | Função |
|------------|------|--------|
| `AccountController` | `@RestController` `/api/v1/accounts` | 9 endpoints → `AccountService` |
| `CategoryController` | `/api/v1/categories` | 7 endpoints → `CategoryService` |
| `ProductController` | `/api/v1/products` | 7 endpoints → `ProductService` |
| `UserController` | `/api/v1/users` | 7 endpoints → `UserService` |
| `ControllerExceptionHandler` | `@RestControllerAdvice` | 11 handlers |
| `ProblemDetails` | classe própria (`Serializable`) | corpo padrão de erro |
| `ValidationError` | estende `ProblemDetails` | corpo do 422 (`fieldErrors`) |
| `FieldMessage` | `record` | item de erro de campo (e acumulador dos validators) |
| `ApiErrorCode` | enum (10 constantes) | `code` estável |
| `SpringDocOpenApiConfig` | `@Configuration` | metadados e esquema de segurança OpenAPI |
| `MessageSourceConfig` | `@Configuration` | `MessageSource` e `AcceptHeaderLocaleResolver` |

**Total: 30 endpoints em `/api/v1`** (9 + 7 + 7 + 7); a estimativa de 33 apresentada em relatório anterior estava incorreta. Fora de `/api/v1`: `POST /oauth2/token` e demais endpoints do Authorization Server (B-6), console H2 (perfil `test`) e Swagger.

Nenhum controller usa mapper, repository ou lógica de negócio: todos recebem DTOs, chamam **um** método de service e devolvem `ResponseEntity` [FATO].

## 4. Endpoints — mapa completo [FATO, salvo indicação]

Legenda de acesso: **URL-pública** = `permitAll` na cadeia `@Order(3)`; **URL-auth** = `anyRequest().authenticated()`; **@PreAuthorize** = regra do método (aplicada depois da validação do corpo; ver §9).

| # | Método | Endpoint | Acesso efetivo | Role | Request | Response (sucesso) | Status |
|---|--------|----------|----------------|------|---------|--------------------|:------:|
| 1 | POST | `/accounts/register` | URL-pública | — | `UserRegisterRequest` | `UserResponse` | 201 (sem `Location`) |
| 2 | GET | `/accounts/activate?token=` | URL-pública | — | query `token` | — | 204 |
| 3 | POST | `/accounts/resend-activation` | URL-pública | — | `UserEmailRequest` | — | 204 |
| 4 | POST | `/accounts/password-recovery` | URL-pública | — | `UserEmailRequest` | — | 204 |
| 5 | POST | `/accounts/reset-password` | URL-pública | — | `PasswordResetRequest` | — | 204 |
| 6 | POST | `/accounts/deactivate` | URL-pública **+** `@PreAuthorize` | ADMIN ou OPERATOR | — | — | **500** (não implementado; OpenAPI diz 204) |
| 7 | PUT | `/accounts/me` | URL-auth **+** `isAuthenticated()` (redundante) | qualquer autenticado | `AuthenticatedUserUpdateRequest` | `UserResponse` | 200 |
| 8 | PATCH | `/accounts/me/password` | URL-auth (sem `@PreAuthorize`, sem OpenAPI) | qualquer autenticado | `PasswordUpdateRequest` | — | 204 |
| 9 | GET | `/accounts/me` | URL-pública **+** `isAuthenticated()` | qualquer autenticado | — | `UserResponse` | 200 |
| 10 | POST | `/categories` | URL-auth + `@PreAuthorize` | ADMIN ou OPERATOR | `CategoryCreateRequest` | `CategoryResponse` | 201 + `Location` |
| 11 | GET | `/categories?name&page&size&sort` | **URL-pública** (sem `@PreAuthorize`) | — | query | `Page<CategoryResponse>` | 200 |
| 12 | GET | `/categories/{id}` | **URL-pública** | — | path | `CategoryDetailsResponse` | 200 |
| 13 | PATCH | `/categories/{id}` | URL-auth + `@PreAuthorize` | ADMIN ou OPERATOR | `CategoryUpdateRequest` | `CategoryResponse` | 200 |
| 14 | PATCH | `/categories/{id}/activate` | idem | ADMIN ou OPERATOR | — | — | 204 |
| 15 | PATCH | `/categories/{id}/deactivate` | idem | ADMIN ou OPERATOR | — | — | 204 |
| 16 | DELETE | `/categories/{id}` | idem | ADMIN ou OPERATOR | — | — | 204 |
| 17 | POST | `/products` | URL-auth + `@PreAuthorize` | ADMIN ou OPERATOR | `ProductCreateRequest` | `ProductResponse` | 201 + `Location` |
| 18 | GET | `/products?name&categoryIds&page&size&sort` | **URL-pública** | — | query | `Page<ProductResponse>` | 200 |
| 19 | GET | `/products/{id}` | **URL-pública** | — | path | `ProductDetailsResponse` | 200 |
| 20 | **PUT** | `/products/{id}` | URL-auth + `@PreAuthorize` | ADMIN ou OPERATOR | `ProductUpdateRequest` | `ProductResponse` | 200 |
| 21 | PATCH | `/products/{id}/activate` | idem | ADMIN ou OPERATOR | — | — | 204 |
| 22 | PATCH | `/products/{id}/deactivate` | idem | ADMIN ou OPERATOR | — | — | 204 |
| 23 | DELETE | `/products/{id}` | idem | ADMIN ou OPERATOR | — | — | 204 |
| 24 | POST | `/users` | URL-auth + `@PreAuthorize` | ADMIN | `UserCreateRequest` | `UserResponse` | 201 + `Location` |
| 25 | GET | `/users?firstName&page&size&sort` | idem | ADMIN | query | `Page<UserResponse>` | 200 |
| 26 | GET | `/users/{id}` | URL-auth + SpEL | ADMIN ou (OPERATOR e `isCurrentUser(#id)`) | path | `UserDetailsResponse` | 200 |
| 27 | PUT | `/users/{id}` | URL-auth + SpEL | ADMIN ou (OPERATOR e `#id == authentication.principal.id`) | `UserUpdateRequest` | `UserResponse` | 200 |
| 28 | PATCH | `/users/{id}/activate` | URL-auth + `@PreAuthorize` | ADMIN | — | — | 204 |
| 29 | PATCH | `/users/{id}/deactivate` | idem | ADMIN | — | — | 204 |
| 30 | DELETE | `/users/{id}` | idem | ADMIN | — | — | 204 |

`Location` (rotas 10, 17, 24): `ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")` — URL da requisição + `/{id}`. As rotas 1, 6 e 9 exploram a combinação "URL pública + regra de método".

## 5. Account [FATO]
Fluxos `HTTP → validação → autorização → controller → service → efeito → resposta`:

| Fluxo | Validação | Autorização | Service / efeito externo | Estado |
|-------|-----------|-------------|--------------------------|--------|
| Registro (1) | `@Valid UserRegisterRequest` (e-mail DNS/único, senha forte, dados pessoais) | pública | `register`: usuário inativo `ROLE_OPERATOR`, token de ativação, **e-mail** (`sendActivationEmailAsync`, síncrono na prática) | implementado; `throws MessagingException` nunca disparada |
| Ativação (2) | sem `@Valid`; `token` obrigatório | pública | `confirmEmail`: ativa usuário, desabilita token | implementado; **GET que altera estado** |
| Reenvio (3) | `@Valid UserEmailRequest` | pública | `resendActivationEmail`: 204 sempre | implementado |
| Recuperação (4) | `@Valid UserEmailRequest` | pública | `requestPasswordRecovery`: token + e-mail; 204 sempre | implementado |
| Redefinição (5) | `@Valid PasswordResetRequest` | pública | `resetPassword` | implementado |
| Perfil `PUT /me` (7) | `@Valid` + `@UniqueEmailForAuthenticatedUser` | autenticado | `updateAuthenticatedUser` (identidade: claim `userId`) | implementado |
| Senha `PATCH /me/password` (8) | `@Valid PasswordUpdateRequest` | autenticado (URL) | `updatePassword` | implementado; sem OpenAPI |
| Perfil `GET /me` (9) | — | `isAuthenticated()` | `getAuthenticatedUser` | implementado |
| **Desativar (6)** | — | `hasAnyRole('ADMIN','OPERATOR')` | `deactivateAccount` → `UnsupportedOperationException` | **não implementado**: um usuário autenticado recebe **500** (handler genérico); sem token, o método é negado (403 esperado); nenhum dado é alterado |

## 6. Category [FATO]
CRUD com `PATCH` para atualização; leituras **públicas**; escrita `ADMIN`/`OPERATOR`. `@Valid` em criar/atualizar: nome 3–80 com padrão e unicidade (`@CategoryCreateValid`/`@CategoryUpdateValid`, este com o `id` da URL); descrição opcional. Fluxo (B-3): `search` sem N+1; `activate`/`deactivate` só gravam o indicador; `delete` sem verificação de produtos (409 no commit se houver vínculo).

## 7. Product [FATO]
- Listagem `GET /products`: `name` (padrão `""`), `categoryIds` (`String`, padrão `"0"` = sem filtro, ids separados por vírgula) + `Pageable`; chama `ProductService.findAllPaged` (consulta nativa + `JOIN FETCH`); **não usa `search`**. Não numérico em `categoryIds` ⇒ `NumberFormatException` ⇒ 500.
- Atualização por **`PUT`** (exige nome e `categoryIds`; demais campos só sobrescrevem se não nulos — B-4). `PATCH /products/{id}` **não existe** (só `/activate` e `/deactivate`).
- Criação: `@ProductCreateValid` (nome único, categorias existentes); `date` validado e **descartado**; preço opcional.
- Divergências entre camadas já registradas: `date` (DTO × mapper), preço opcional (DTO/entidade/banco), `active` nunca filtra a listagem.

## 8. User [FATO / INFERÊNCIA]
Todas as rotas exigem autenticação por URL; `ADMIN` em 5 das 7; regras por usuário para `OPERATOR` em duas.

**`PUT /users/{id}`** — `hasRole('ADMIN') OR (hasRole('OPERATOR') AND #id == authentication.principal.id)`:

| Chamador | Resultado provável | Rótulo |
|----------|-------------------|--------|
| ADMIN (qualquer `id`) | passa por `hasRole('ADMIN')`; segue para o service | INFERÊNCIA forte (curto-circuito do `OR`) |
| OPERATOR (próprio ou outro `id`) | 2º termo falso: `principal` é o `Jwt`, `id` → `Jwt.getId()` = claim `jti` (texto) × `#id` `Long` ⇒ **negado (403)** mesmo no próprio usuário | **Risco / INFERÊNCIA forte**; execução: HIPÓTESE (nenhum teste cobre) |
| Token sem `ROLE_ADMIN`/`ROLE_OPERATOR` | negado (403) | INFERÊNCIA (não há tal usuário: quem não tem roles não recebe token — B-6) |
| Anônimo | 401 pelo filtro (rota exige autenticação) | INFERÊNCIA |

- **Ordem:** o corpo é validado **antes** de `@PreAuthorize`: `OPERATOR` com corpo inválido recebe **422**, não 403; `UserUpdateValidator` já executa consultas ao banco e DNS nesse caso [INFERÊNCIA; §9].
- **Contradição documental:** o OpenAPI (`@Operation`) promete "ADMIN ou OPERATOR para o próprio usuário" — comportamento **diferente** do que o SpEL tende a produzir (**ponto para correção futura**, não corrigido).
- **Contraste:** `GET /users/{id}` usa `@authenticatedUserService.isCurrentUser(#id)` (claim `userId`), coerente com a identidade do token [Baseline: `ResourceServerAuthorizationIT` verde].
- Roles: `roleIds` nulo ⇒ mantém; vazio ⇒ remove todas; usuário sem roles não faz login (B-3/B-6).

## 9. Validation e ordem das etapas
**Todos os 12 `@RequestBody` têm `@Valid`** [FATO]: register, resend-activation, password-recovery, reset-password, `PUT /me`, `PATCH /me/password`, `POST`/`PATCH` categorias, `POST`/`PUT` produtos, `POST`/`PUT` usuários. `@PathVariable`/`@RequestParam` **não** têm validação (`Long id`, `String token`, `name`, `firstName`, `categoryIds`).

Ordem relevante (código + comportamento do Spring):

| Etapa | Onde | Rótulo |
|-------|------|--------|
| 1. Filtros CORS (servlet, `HIGHEST_PRECEDENCE`) e cadeia de segurança: autenticação do JWT e regras de **URL** | `ResourceServerConfig` | FATO |
| 2. Resolução dos argumentos **na ordem de declaração**: `@PathVariable Long id` (conversão) → `@RequestBody` (desserialização JSON) → `@Valid` (Bean Validation: `@NotBlank`/`@Size`…, validators de classe/campo com DB, DNS, URL, JWT) | Spring MVC | INFERÊNCIA (comportamento padrão do framework) |
| 3. Invocação do método pelo **proxy de segurança de método**: `@PreAuthorize` avaliado **aqui**, depois dos argumentos | `@EnableMethodSecurity` | INFERÊNCIA |
| 4. Corpo do controller → service (regras de aplicação, `@Transactional`) → repository/banco (restrições) | — | FATO |

Consequências: erro de conversão do `id` ou de JSON ⇒ falha **antes** da autorização; corpo inválido de um usuário sem permissão ⇒ **422** (não 403); validators (banco/DNS) rodam para chamadores que serão negados depois. Nenhum código reordena isso.

## 10. Autorização [FATO]
Ocorrências: **somente `@PreAuthorize`** (nenhum `@Secured`/`@RolesAllowed`).

| Expressão | Onde | Base |
|-----------|------|------|
| `hasRole('ADMIN') or hasRole('OPERATOR')` | 10, 13–17, 20–23 | authority `ROLE_*` do claim `authorities` (B-6) |
| `hasRole('ADMIN')` | 24, 25, 28–30 | idem |
| `hasRole('ADMIN') OR (hasRole('OPERATOR') AND @authenticatedUserService.isCurrentUser(#id))` | 26 | claim `userId` → `UserRepository.findById` |
| `hasRole('ADMIN') OR (hasRole('OPERATOR') AND #id == authentication.principal.id)` | 27 | **principal = `Jwt`; `id` = `jti`** (B-6) |
| `hasAnyRole('ADMIN','OPERATOR')` | 6 (não implementado) | authorities |
| `isAuthenticated()` | 7, 9 | redundante em 7 (URL já exige); **única proteção** em 9 |

Sobre `authentication.principal.id`: **comportamento** confirmado por leitura/bytecode (B-6); **risco** = `OPERATOR` provavelmente não atualiza ninguém; **hipótese** = status/resposta exatos; **ponto para correção futura** = SpEL/documentação. `#id` depende de `-parameters` (compilação configurada).

## 11. Status HTTP [FATO]

**Sucesso:** 200 (GET, PUT/PATCH com corpo de resposta), 201 (create; e register), 204 (activate/deactivate/delete/`me/password`/fluxos sem corpo).

**Erros realmente produzidos pelo código:**

| Status | Origem |
|:------:|--------|
| 400 | `DatabaseException` (`ProductService.delete`); `InvalidTokenException` |
| 401 | `AuthenticatedUserNotFoundException` (corpo `ProblemDetails`); token ausente/inválido nas rotas protegidas (resposta do filtro de bearer, **sem** o formato `ProblemDetails`) [INFERÊNCIA] |
| 403 | `AccessDeniedException` (`@PreAuthorize`); `DisabledException` (provavelmente inalcançável) |
| 404 | `ResourceNotFoundException`; `NoResourceFoundException` |
| 409 | `DataIntegrityViolationException` |
| 422 | `MethodArgumentNotValidException`; `PasswordUpdateException` |
| 500 | qualquer outra `Exception` (ver §12) |

**Não são produzidos:** 405 (vira 500), 415 (vira 500), 400 por JSON ilegível, parâmetro ausente ou tipo de argumento inválido (viram 500). Os `400` declarados nos endpoints de criação/atualização de catálogo, usuários e registro **não** são produzidos por esses endpoints.

## 12. Exception handling [FATO]
`ControllerExceptionHandler` (`@RestControllerAdvice`), 11 handlers (o Spring escolhe o mais específico, não pela ordem):

| Exceção | HTTP | `code` | Título / detalhe | Log |
|---------|:----:|--------|------------------|-----|
| `ResourceNotFoundException` | 404 | RESOURCE_NOT_FOUND | `error.resource.title` / **chave da exceção** (padrão `error.resource.message`) | WARN |
| `NoResourceFoundException` | 404 | RESOURCE_NOT_FOUND | títulos fixos | DEBUG |
| `DatabaseException` | 400 | DATABASE_ERROR | chave da exceção | ERROR + stack |
| `DataIntegrityViolationException` | 409 | CONFLICT | fixos | ERROR + stack |
| `MethodArgumentNotValidException` | 422 | VALIDATION_ERROR | fixos + `fieldErrors` | — |
| `AccessDeniedException` | 403 | ACCESS_DENIED | fixos | WARN |
| `InvalidTokenException` | 400 | INVALID_TOKEN | chave da exceção | WARN |
| `AuthenticatedUserNotFoundException` | 401 | AUTHENTICATION_REQUIRED | chave da exceção | WARN |
| `PasswordUpdateException` | 422 | PASSWORD_UPDATE_ERROR | chave da exceção | — |
| `DisabledException` | 403 | ACCESS_DISABLED | fixos | — |
| `Exception` | 500 | INTERNAL_SERVER_ERROR | fixos | ERROR + stack |

- **Sem handler** para `ConstraintViolationException`, `AuthenticationException`, OAuth2/JWT exceptions, `UnsupportedOperationException`, `HttpRequestMethodNotSupportedException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `MissingServletRequestParameterException`, `HttpMediaTypeNotSupportedException`: todas caem em `handleGeneric` (500).
- **405 → 500 (achado B-0): confirmado** pelo código (`@ExceptionHandler(Exception.class)` sem `ResponseEntityExceptionHandler`) e pelo log do baseline (`Resolved [HttpRequestMethodNotSupportedException…]` seguido de `Unexpected error`). A causa é mais ampla que o 405 (ver §11).
- **Detalhe traduzido:** `resolveMessage` traduz a chave da exceção; chave inexistente ⇒ texto padrão da categoria — a mensagem crua nunca é devolvida. Todas as 20 chaves `error.*` de título/detalhe usadas existem nos 3 idiomas.
- Erros de segurança **antes** do controller (cadeia de filtros) não passam por este advice; `AccessDeniedException` de `@PreAuthorize` **passa**, e por isso **usuário anônimo em `GET /accounts/me` ou `POST /accounts/deactivate` deve receber 403, não 401** [INFERÊNCIA: o `ExceptionTranslationFilter`, que produziria 401, não vê a exceção capturada pelo advice].
- OAuth2 (`/oauth2/token`): erros são do Authorization Server (`{"error": …}`), não deste advice (B-6).

## 13. ValidationError / FieldMessage [FATO]
JSON do 422: `{timestamp, status, code, error, message, path, fieldErrors:[{fieldName, message}]}`.
- Entra **uma entrada por violação de campo** (`getFieldErrors()`); o mesmo campo pode repetir (regras de senha, até 7). Mensagem = `getDefaultMessage()` (já interpolada com o `MessageSource`/idioma da requisição [INFERÊNCIA]); **não há valor rejeitado**.
- **Erros globais** (`getGlobalErrors()`) são ignorados; hoje nenhum validator produz erro global (os de classe usam `addPropertyNode`), então não há perda [FATO].
- Nomes de campo: os do DTO (`name`, `categoryIds`, `email`, `password`, …); para validators de campo, o nome do componente do `record`.
- Chaves de validação → mensagens: ver B-5 (as padrões `{user.password.strong}`/`{user.update.validation}` não existem, mas não são emitidas). `FieldMessage` também é o acumulador interno de 6 validators (acoplamento validation→web, B-5).

## 14. ProblemDetails [FATO]
- É uma classe **própria** (`ProblemDetails`, com "s"); **não** usa `org.springframework.http.ProblemDetail` (busca por `ProblemDetail` em `src/main`: 0 ocorrências) e **não** segue o RFC 7807: campos `timestamp`, `status`, `code`, `error` (título), `message` (detalhe), `path`. Não há `type`, `title`, `detail`, `instance` nem propriedades dinâmicas. `path` = `getRequestURI()` (sem query string).
- Padrão **consistente** entre todos os handlers (mesmo construtor/JSON); a única variação é o `fieldErrors` do 422. Inconsistências: o 401/403 do filtro de segurança e os erros OAuth2 usam outros formatos; o JavaDoc antigo exibia exemplo sem `code` e caminho sem `/api/v1` (corrigido).
- Serialização: `Instant` como texto ISO pelo Jackson do Spring Boot [INFERÊNCIA]; perfil `test` indenta a saída.

## 15. CORS [FATO / INFERÊNCIA]
Configuração única em `ResourceServerConfig` (B-6): origens de `cors.origins` (padrões de origem; padrão local `localhost:3000/5173`), métodos `POST, GET, PUT, DELETE, PATCH`, cabeçalhos `Authorization` e `Content-Type`, credenciais permitidas, **sem** `exposedHeaders` e sem `maxAge`. Aplicada em **dois** pontos: `http.cors(...)` (cadeia 3) e um `CorsFilter` de servlet com `HIGHEST_PRECEDENCE` (vale também para `/oauth2/**`).
- **Preflight (hipótese da B-6):** `OPTIONS` não precisa constar em `allowedMethods` — o CORS confere o método solicitado em `Access-Control-Request-Method`; o `CorsFilter` de servlet responde ao preflight **antes** da cadeia de segurança. **Parcialmente refutada** como problema para `POST/GET/PUT/DELETE/PATCH` com `Authorization`/`Content-Type` de origens permitidas [INFERÊNCIA]; preflight com **outros** cabeçalhos solicitados (por exemplo, `X-Requested-With`) seria recusado.
- **Consequência:** o cabeçalho `Location` (201) e outros não seguros **não são expostos** a scripts de outra origem [INFERÊNCIA].

## 16. OpenAPI / Swagger [FATO / INFERÊNCIA]
- Springdoc com `packagesToScan = web.controller`; esquema `security` (HTTP bearer, JWT) em `SpringDocOpenApiConfig`; `@Tag` por controller.
- **Caminhos por perfil:** `test` → `/docs-asjcatalog` (JSON) e `/docs-asjcatalog.html`; `dev` → `/docs-dscatalog` e `/docs-dscatalog.html`; `prod` → sem configuração (padrões do springdoc). A segurança libera **só** `/docs-asjcatalog`, `/docs-asjcatalog/**`, `/docs-asjcatalog.html` e `/swagger-ui/**`.
  - `test`: funcional. `dev`: os caminhos configurados não estão liberados (exigem autenticação), embora `/swagger-ui/**` esteja [INFERÊNCIA]. `prod`: `/swagger-ui/**` liberado, mas o JSON padrão (`/v3/api-docs`) exigiria autenticação [INFERÊNCIA].
  - **Não há** propriedade para desabilitar o springdoc em nenhum perfil [FATO]. Perfil ativo padrão: `test`.
- **Divergências documentação × código** [FATO]: (a) `GET` de catálogo estão marcados com exigência de Bearer e declaram 401/403, mas são públicos por URL e sem `@PreAuthorize`; (b) `PATCH /me/password` sem nenhuma anotação; (c) `deactivate` declara 204 e produz 500; (d) endpoints de criação/atualização declaram 400 que não é produzido; (e) `/accounts/activate` não declara o 404 de token inexistente; (f) `PUT /users/{id}` promete acesso de `OPERATOR` ao próprio usuário; (g) contato com e-mail digitado incorretamente (`albertinesilva,17@gmail.com`) e licença Apache 2.0 (o `pom.xml` diz MIT).

## 17. Endpoints públicos × protegidos — matriz [FATO / INFERÊNCIA]

| Endpoint | Security config | Controller | Resultado |
|----------|-----------------|------------|-----------|
| `GET /categories/**`, `GET /products/**` | `permitAll` | sem `@PreAuthorize` | **público** (OpenAPI diz autenticado) |
| `POST/GET /accounts/*` (register, activate, resend, recovery, reset) | `permitAll` | sem `@PreAuthorize` | **público** |
| `GET /accounts/me` | `permitAll` | `isAuthenticated()` | **protegido só pelo método** ⇒ anônimo: 403 [INFERÊNCIA] |
| `POST /accounts/deactivate` | `permitAll` | `hasAnyRole(...)` | **protegido só pelo método**; e não implementado |
| `PUT /accounts/me` | autenticado | `isAuthenticated()` | **redundante** |
| `PATCH /accounts/me/password` | autenticado | — | autenticado por URL |
| `POST/PATCH/PUT/DELETE` catálogo | autenticado | ADMIN ou OPERATOR | role-specific |
| `/users/**` | autenticado | ADMIN (ou regras por usuário) | role-specific; `PUT` possivelmente bloqueia OPERATOR (conflito com a documentação) |
| `/oauth2/**`, `/.well-known/**` | cadeia do AS | — | autenticação do cliente (Basic) |
| `GET /categories/{id}/activate` (não existe) | `permitAll` (GET) | sem mapeamento GET | 405 → 500 (público) [INFERÊNCIA] |

## 18. Fluxos de exceção (sem execução)
- **Validação:** `POST /users` (ADMIN) com e-mail já cadastrado → `@Valid` → `UniqueEmailValidator` viola → `MethodArgumentNotValidException` → `validation()` → **422** `ValidationError` com `fieldErrors:[{email, "Email já cadastrado"}]`.
- **Autenticação:** `GET /users` sem token → cadeia `@Order(3)` (`anyRequest().authenticated()`) → `BearerTokenAuthenticationEntryPoint` → **401**, cabeçalho `WWW-Authenticate`, corpo sem `ProblemDetails` [INFERÊNCIA]; token expirado/adulterado → 401.
- **Autorização:** OPERATOR `DELETE /users/1` → URL ok → argumentos → `@PreAuthorize("hasRole('ADMIN')")` nega → `AccessDeniedException` no controller → `handleAccessDenied` → **403** `ACCESS_DENIED` [Baseline: 403 do `ResourceServerAuthorizationIT`].
- **Negócio:** `GET /products/9999` → `ProductService.findEntityById` → `ResourceNotFoundException("error.product.notFound")` → `handleResourceNotFound` → **404** com o texto traduzido de `error.product.notFound`.
- **Não tratada especificamente:** `PATCH /products/1` → `HttpRequestMethodNotSupportedException` → `handleGeneric` → **500** `INTERNAL_SERVER_ERROR` (e log ERROR com stack).

## 19. Logging [FATO]
| Onde | Nível | O que aparece |
|------|:-----:|---------------|
| `UserController.create/update` | DEBUG | **DTO inteiro** (`UserCreateRequest`/`UserUpdateRequest`): **senha em texto** quando informada (`toString()` do `record`) |
| `Category/ProductController.create/update`, `CategoryService/ProductService.create` | DEBUG | DTO inteiro (sem dado sensível) |
| `AccountController.register` | DEBUG | e-mail |
| `resend-activation`, `password-recovery` | INFO | e-mail |
| `reset-password`, `updatePassword`, `deactivate` | INFO/nenhum | mensagem sem dados |
| `EmailService` | INFO | endereço do destinatário; **tokens não são registrados** |
| `ControllerExceptionHandler` | WARN/ERROR/DEBUG | caminho e mensagem da exceção; **stack trace completo** em ERROR (DatabaseException, DataIntegrityViolation, Exception) — pode conter texto de SQL/restrições **no log**, nunca na resposta |
- Nenhum controller registra credenciais de login (o `/oauth2/token` não passa por eles), tokens de ativação/recuperação ou JWT [FATO].
- Efeito do log do DTO: perfis `dev`/`test` têm `com.albertsilva.dev.dscatalog=DEBUG`; **[Baseline]** o log do teste exibiu `UserCreateRequest[…password=…]`. Em `prod` o nível é INFO (não emite).
- Fora de `web` (B-8): `application-dev/test` habilitam bind de parâmetros SQL em TRACE (`BasicBinder`/`orm.jdbc.bind`), o que pode registrar hash de senha, e-mails e valores de token em tabelas.
  - **Atualização (fase B-8):** o nome de logger configurado em `dev` (`org.hibernate.type.descriptor.sql.BasicBinder`) não existe no Hibernate 6.6.45 (o bind usa `org.hibernate.orm.jdbc.bind`); portanto o bind de parâmetros tende a ser registrado **apenas em `test`**, não em `dev`. Ver [B-8-CONFIG-INFRASTRUCTURE.md](B-8-CONFIG-INFRASTRUCTURE.md) §12.

## 20. Fatos
30 endpoints; controllers sem lógica; 12 `@Valid`; só `@PreAuthorize`; `PUT` em produtos e usuários, `PATCH` em categorias; `ProblemDetails` próprio; 11 handlers; sem handler para exceções do Spring MVC; `deactivateAccount` ⇒ 500; catálogo `GET` público; `PATCH /me/password` sem OpenAPI; springdoc habilitado em todos os perfis; `date` descartado; `Location` só nos 3 creates; logs de DTO com senha em DEBUG.

## 21. Inferências
Ordem argumentos/validação → `@PreAuthorize`; anônimo em rota "URL-pública + método protegido" ⇒ 403; 401 do filtro sem corpo `ProblemDetails`; `OPERATOR` bloqueado em `PUT /users/{id}`; preflight coberto para os cabeçalhos configurados; `Location` não exposto ao CORS; JSON do 422 com mensagens interpoladas por idioma; `/v3/api-docs` protegido em `prod`.

## 22. Hipóteses
Status/corpo exatos dos casos acima; comportamento do preflight com cabeçalhos extras; efeito de `PageImpl` serializado diretamente (estrutura JSON `content/totalElements/...`, com possível aviso do Spring Data); propagação de exceções lançadas dentro de validators; tempo de resposta dos endpoints públicos de e-mail.

## 23. Achados herdados — situação

| Achado | Situação |
|--------|----------|
| `ProductController.update` é `PUT` × testes `PATCH` | **Confirmado** (controller `@PutMapping`; `PATCH` ⇒ 405 ⇒ 500). Verbo real: PUT `/api/v1/products/{id}`; categorias usam PATCH. |
| `UserController.update` — `authentication.principal.id` | **Confirmado** por leitura (principal `Jwt`, `id`→`jti`); efeito 403 para OPERATOR = inferência forte; contradiz o OpenAPI. |
| `POST /accounts/deactivate` | **Confirmado**: 500 (`UnsupportedOperationException`); OpenAPI declara 204. |
| 405 → 500 no handler genérico | **Confirmado** (código + log de baseline); alcance **maior** (400/415 também). |
| CORS (preflight) | **Parcialmente refutado**: `OPTIONS` não precisa constar; permanece hipótese para cabeçalhos extras; `Location` não exposto. |
| Endpoints públicos | **Confirmado** e detalhado (matriz §17); GET de catálogo públicos contra o OpenAPI. |
| OpenAPI | **Confirmado**: caminhos liberados só para `test`; springdoc sempre habilitado. |
| Senha/token em logs | **Parcialmente confirmado**: senha em DEBUG (`UserController`); tokens não são logados por `web`; bind SQL em TRACE (dev/test) é ponto de B-8. |
| Ordem validação × autorização | **Inferência** (framework); não determinável sem execução. |
| Respostas 401/403 | **Parcialmente confirmado**: 403 JSON via advice (baseline); 401 do filtro sem corpo padrão (inferência); anônimo em rotas "URL-pública + método" ⇒ 403 (inferência). |

## 24. Pontos para B-8 (Config/Infrastructure)
`application*.properties` completos (perfis, logging de SQL/bind, `spring.mail.test-connection`, Flyway/prod sem datasource, springdoc por perfil), Jackson/serialização de `Page`, `server.*`, propriedades de CORS por ambiente, `logs/`, `pom.xml` (contato/licença), `import.sql` × Flyway.

## 25. Pontos para B-9 (Architecture Review)
Fronteira 401/403/500 e formatos de erro inconsistentes; política de verbos HTTP; acoplamento validation→web (`FieldMessage`) e domain→service/dto; duplicação de validações; consistência OpenAPI×código; modelo de identidade (`userId` × `jti` × `sub`); revogação de tokens; contratos de "público por URL + protegido por método".

## 26. Pontos para testes
405/415/JSON ilegível/parâmetro ausente/`id` não numérico (respostas reais); `PUT /users/{id}` para ADMIN e OPERATOR; 401/403 (anônimo em `/accounts/me` e `/accounts/deactivate`); ordem validação × `@PreAuthorize`; preflight CORS; `Location` nos 201; `GET` de catálogo sem token; `deactivate`; 422 (`fieldErrors`, idiomas); `PATCH /products/{id}` (divergência de testes); springdoc por perfil; logs (senha em DEBUG).

## 27. Decisões adiadas
Nenhuma correção: handlers para exceções do Spring MVC, verbo de produtos, SpEL de `PUT /users/{id}`, `deactivate`, exposição de `Location`, OpenAPI (segurança, códigos, caminhos), logs de DTO, formato `ProblemDetails`×RFC 7807.

## 28. JavaDoc adicionado ou ajustado

| Arquivo | Alterações |
|---------|-----------|
| 4 controllers | JavaDoc de classe (acesso real × OpenAPI, verbos) e de **todos os 30 endpoints** (verbo/rota, acesso, validação, fluxo, status, erros). Antes só havia um JavaDoc de uma linha por classe. Destaques: `deactivate` (não implementado, 500), `PUT /users/{id}` (`principal.id` = `jti`), `GET /me`, `PATCH /me/password` (sem OpenAPI), catálogo público. |
| `ControllerExceptionHandler` | Nunca teve JavaDoc de métodos: classe (tabela exceção→HTTP, consequências do handler genérico, escopo, logs) e os 11 handlers + helpers. |
| `ProblemDetails` | Classe reescrita: exemplo antigo omitia `code` e usava caminho sem `/api/v1`; explicitado que **não** é `ProblemDetail`/RFC 7807; construtores. |
| `ValidationError`, `FieldMessage`, `ApiErrorCode` | Documentados (estrutura JSON, duplo uso do `FieldMessage`, exceção de cada `code`). |
| `SpringDocOpenApiConfig`, `MessageSourceConfig` | Documentados (caminhos por perfil, esquema `security`, i18n). |

Doclint (`-Xdoclint:all,-missing`) sem avisos.

## 29. O que **não** foi feito
Nenhuma alteração de endpoint, verbo, status, handler, `ProblemDetails`, CORS, OpenAPI, logging, security, validation, service, DTO, repository, entidade, `pom.xml` ou teste; nenhum teste executado; nenhum commit.
