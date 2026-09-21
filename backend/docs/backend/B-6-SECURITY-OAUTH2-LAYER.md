# ASJCatalog Backend — B-6: Security / OAuth2

> **Fase:** B-6 — análise e documentação (JavaDoc) de `security`.
> **Pré-requisitos:** [B-0](B-0-BACKEND-INVENTORY.md) a [B-5](B-5-VALIDATION-LAYER.md).
> **Escopo alterado:** somente comentários JavaDoc nos 8 arquivos de `src/main/java/**/security/**` e esta documentação, com notas pontuais nos documentos anteriores. Nenhuma linha de código executável, configuração, claim, authority, TTL, filter chain ou comportamento foi alterada.

Rótulos: **[FATO]** confirmado por leitura do código (ou de bytecode das bibliotecas); **[INFERÊNCIA]** conclusão razoável sobre o comportamento do framework, sem execução dedicada; **[HIPÓTESE]** precisa de execução; **[Evidência de baseline]** resultado já registrado no B-0 (nenhum teste foi executado agora). Fora do escopo desta fase (não alterados): `web`/controllers/`ControllerAdvice` (B-7), `config` geral, `src/test/**` (só leitura de `OAuth2TokenIT`, para evidência).

---

## 1. Objetivo
Descrever como a aplicação autentica (password grant), emite e valida tokens (JWT e refresh), constrói authorities e recupera a identidade do usuário, e confirmar ou separar os achados herdados de B-0 a B-5 — sem corrigir nada.

## 2. Escopo
`security/auth`, `security/config`, `security/userdetails`, `security/oauth2/authorization/config`, `security/oauth2/grant/password`, `security/oauth2/resource/config` — **8 arquivos** — mais leitura de `application*.properties` (valores sensíveis não reproduzidos), de `UserController` (somente o trecho de `@PreAuthorize`), de `UserService.loadUserByUsername` e de `OAuth2TokenIT`. Não há `JwtEncoder`/`JwtDecoder` personalizados além dos citados, nem `AuthenticationManager`/`AuthenticationProvider` beans próprios: o único provider é o do password grant, instanciado com `new`.

## 3. Inventário [FATO]

| Classe | Responsabilidade (lida na implementação) | Tipo | Dependências principais | Fluxo |
|--------|------------------------------------------|------|-------------------------|-------|
| `SecurityBeansConfig` | expõe `PasswordEncoder` = `BCryptPasswordEncoder` (força padrão) | configuração / criptografia | — | senhas, segredo do cliente |
| `AuthorizationServerConfig` | filter chain do AS (`@Order(2)`), cliente registrado, `TokenSettings`, gerador de tokens, customizador de claims, JWK/RSA, `JwtDecoder`; serviços em memória | configuração / OAuth2 / JWT / cripto | `UserDetailsService`, `PasswordEncoder`, Spring Authorization Server | emissão e validação |
| `CustomPasswordAuthenticationConverter` | converte `grant_type=password` em token de autenticação; valida forma dos parâmetros | converter (authentication) | `SecurityContextHolder` | login (1º passo) |
| `CustomPasswordAuthenticationToken` | representa a solicitação (usuário, senha, escopos, cliente) | authentication token (grant) | `OAuth2AuthorizationGrantAuthenticationToken` | login |
| `CustomPasswordAuthenticationProvider` | autentica usuário e cliente, emite access+refresh token, salva a autorização | provider (authentication) | `UserDetailsService`, `PasswordEncoder`, `OAuth2TokenGenerator`, `OAuth2AuthorizationService` | login (2º passo) |
| `AuthenticatedUser` | contêiner id/username/authorities usado só na emissão | principal auxiliar | `GrantedAuthority` | emissão de claims |
| `ResourceServerConfig` | filter chains do H2 (`@Order(1)`, condicional) e do RS (`@Order(3)`), `@EnableMethodSecurity`, conversor JWT→authorities, CORS | configuração / authorization | `JwtDecoder` (bean) | validação e autorização |
| `AuthenticatedUserService` | resolve o `User` a partir do claim `userId` do `Jwt` no `SecurityContext` | serviço de identidade | `UserRepository` | uso nos fluxos "me", validator e SpEL |

## 4. Arquitetura [FATO / INFERÊNCIA]

Um processo, dois papéis (Authorization Server e Resource Server) e **três** `SecurityFilterChain`s (ver §15). Fluxo real:

```
POST /oauth2/token (client_secret_basic + grant_type=password, username, password)
  → cadeia @Order(2) → autenticação do cliente (framework: OAuth2ClientAuthenticationFilter)
  → CustomPasswordAuthenticationConverter.convert  → CustomPasswordAuthenticationToken (cliente como principal)
  → CustomPasswordAuthenticationProvider.authenticate
       → UserService.loadUserByUsername (UserRepository.searchUserAndRolesByEmail → User parcial)
       → PasswordEncoder.matches  → validateUserStatus
       → AuthenticatedUser(id, username digitado, authorities) nos details do token do cliente
       → OAuth2TokenGenerator: JwtGenerator (+ tokenCustomizer: authorities, userId, username) e refresh token
       → OAuth2AuthorizationService.save (memória; principalName = clientId)
  → resposta: access_token (JWT RS256 [INFERÊNCIA: algoritmo padrão]) + refresh_token

GET/POST … /api/v1/** com "Authorization: Bearer <JWT>"
  → cadeia @Order(3) → BearerTokenAuthenticationFilter → JwtDecoder (chave RSA em memória; expiração)
  → JwtAuthenticationConverter (claim "authorities", prefixo "")  → JwtAuthenticationToken (principal = Jwt)
  → @PreAuthorize (hasRole/SpEL)  → controller → AuthenticatedUserService (claim userId) quando necessário
```
O fluxo de renovação (`grant_type=refresh_token`) **não passa** por converter/provider customizados: é tratado pelo provider padrão do Spring Authorization Server [INFERÊNCIA: o código só *adiciona* os componentes do password grant]; a evidência de baseline (`OAuth2TokenIT`) mostra renovação com rotação e claims mantidos.

## 5. Authentication [FATO]
- **Cliente:** método de autenticação não declarado no `RegisteredClient` (vale o padrão do framework); `TokenUtil` e `OAuth2TokenIT` usam HTTP Basic com `security.client-id`/`client-secret`.
- **Usuário:** apenas no password grant, por e-mail (`username`) e senha; **não há** `AuthenticationManager` nem `DaoAuthenticationProvider` próprios nem login por formulário.
- **Requisições à API:** apenas JWT (`oauth2ResourceServer().jwt()`); sem sessão configurada explicitamente.

## 6. Custom Password Grant [FATO]

**Converter (`CustomPasswordAuthenticationConverter`)**: `grant_type != password` ⇒ `null`. Exige `username` e `password` não em branco e **únicos**; `scope` opcional mas único; qualquer violação ⇒ `OAuth2AuthenticationException(invalid_request)`. Escopos viram `Set` (split por espaço); os demais parâmetros (ex.: `client_id`) vão para `additionalParameters`. O cliente autenticado vem de `SecurityContextHolder`.

**Token (`CustomPasswordAuthenticationToken`)**: `AuthorizationGrantType("password")` fixo; guarda `username`, `password` (texto), `scopes` (imutável, `null`→vazio).

**Provider (`CustomPasswordAuthenticationProvider.authenticate`)**, em ordem:
1. principal deve ser `OAuth2ClientAuthenticationToken` autenticado, senão `invalid_client`;
2. `userDetailsService.loadUserByUsername(username)` — `UserService` (§7);
3. `validateCredentials`: **somente** `passwordEncoder.matches(password, hash)`; o username não é comparado a nada;
4. `validateUserStatus`: `isEnabled()` ⇒ senão `invalid_grant` "Your account has not been activated yet. Please check your email."; depois `isAccountNonLocked/NonExpired/CredentialsNonExpired` (sempre `true` em `User`);
5. `UsernameNotFoundException` **e** senha errada ⇒ `invalid_grant` "Invalid credentials" (mesma mensagem);
6. escopos autorizados = nomes das authorities ∩ escopos do cliente (`read`, `write`); **os escopos pedidos (`getScopes()`) não são usados**;
7. `userId = ((User) userDetails).getId()` (exige que o `UserDetails` seja `User`);
8. `AuthenticatedUser(userId, username digitado, authorities)` → `details` do token do cliente; **substitui o `SecurityContext`** da thread por um contexto com esse token;
9. gera access token (JWT) e refresh token (**sempre**); `null` em qualquer um ⇒ `server_error`;
10. salva `OAuth2Authorization` com `principalName = clientId` (não o usuário), atributo `Principal` = token do cliente, grant `password`, escopos autorizados;
11. retorna `OAuth2AccessTokenAuthenticationToken`.

Onde o usuário é buscado: **`UserRepository.searchUserAndRolesByEmail`** (via `UserService`), **projeção `UserDetailsProjection`**, montando um **`User` parcial**.

## 7. UserDetails / usuário parcial [FATO / INFERÊNCIA]

| Campo do `User` | Preenchido por `loadUserByUsername`? | Usado no login? |
|-----------------|--------------------------------------|-----------------|
| `id` | sim (projeção `id`) | sim: `((User) userDetails).getId()` → claim `userId` |
| `email` (= `getUsername()`) | sim (alias `username`) | `getUsername()` **não** é lido pelo provider (o claim usa o `username` digitado) |
| `password` (hash) | sim | sim: `PasswordEncoder.matches` |
| `active` | sim | sim: `isEnabled()` |
| `roles` | sim: uma `Role(roleId, authority)` por linha | sim: `getAuthorities()` → escopos e claim `authorities` |
| `firstName`, `lastName` | **nulos** | **não** (nenhuma leitura em `src/main` sobre esse objeto) |
| `tokens` | vazio | não |

- **[FATO]** O objeto é uma instância nova, **nunca persistida**, usada só durante a autenticação; depois, apenas `id`, o `username` digitado e as authorities sobrevivem (em `AuthenticatedUser`, nos `details` do token do cliente e nos claims).
- **[FATO]** Usuário **sem roles** ⇒ consulta com `INNER JOIN` ⇒ lista vazia ⇒ `UsernameNotFoundException` ⇒ `invalid_grant` "Invalid credentials": não autentica.
- **[FATO]** A consulta compara o e-mail exatamente (sem `trim`/caixa) e não filtra por `active` (o status é verificado depois).
- **[INFERÊNCIA]** Roles duplicadas por linha colapsam no `Set` de `User`; o `equals` de `Role` é por id.

## 8. AuthenticatedUser e AuthenticatedUserService

**`AuthenticatedUser`** [FATO]: contêiner (id, username, authorities) criado **só** pelo provider e lido **só** pelo `tokenCustomizer`; não é `UserDetails`, não é o principal da API e não é usado por `@PreAuthorize`. O JavaDoc antigo descrevia um construtor de 2 argumentos e uso em `@PreAuthorize` (incorreto; corrigido).

**`AuthenticatedUserService.getAuthenticatedUser()`** [FATO]:
1. `SecurityContextHolder.getContext().getAuthentication()`; `null` ou principal que **não seja `Jwt`** ⇒ `AuthenticatedUserNotFoundException("error.auth.invalid.principal")`;
2. `Long userId = jwt.getClaim("userId")`; `null` ou `<= 0` ⇒ `error.auth.userId.claim.notFound`;
3. `UserRepository.findById(userId)`; ausente ⇒ `error.auth.user.notFound`.
- **Não** verifica `active`; sem transação; uma consulta por chamada; `isCurrentUser(id)` = `getAuthenticatedUser().getId().equals(id)`.
- O claim é lido como `Long`; a evidência de baseline (os ITs de "me"/`isCurrentUser` verdes) indica que o `Jwt` decodificado entrega `Long`.
- **Confirmado:** o `userId` do JWT é a **única** fonte de identidade usada para carregar o `User` na aplicação. Nenhum outro código lê `username`, `sub` ou e-mail do JWT para isso. (A outra regra de identidade, o SpEL `authentication.principal.id`, usa uma propriedade diferente — §12.)
- Usos: `AccountService` (3 fluxos "me"), `UniqueEmailForAuthenticatedUserValidator`, SpEL `@authenticatedUserService.isCurrentUser(#id)` em `UserController.findById`.

## 9. JWT [FATO / INFERÊNCIA]
- **Formato:** `SELF_CONTAINED` (JWT). **Assinatura:** RSA (chave RSA de 2048 bits, `kid` = UUID); o algoritmo padrão do `JwtGenerator` para chave RSA é RS256 [INFERÊNCIA].
- **TTL do access token:** `security.jwt.duration` (segundos; padrão 86400).
- **`JwtDecoder`:** um único bean (`OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)`), usado por ambas as cadeias; validação por assinatura e, nos validadores padrão, expiração [INFERÊNCIA]; **sem** validador de emissor/audiência configurado e sem consulta ao estado do usuário ou da autorização. **`JwtEncoder`:** `NimbusJwtEncoder(jwkSource())` dentro de `tokenGenerator()`.
- **Emissor (`iss`):** `AuthorizationServerSettings` sem emissor fixo (resolvido da requisição) [INFERÊNCIA].

## 10. Claims [FATO, salvo indicação]

| Claim | Origem | Tipo | Onde é criado | Onde é consumido |
|-------|--------|------|---------------|------------------|
| `authorities` (custom) | `AuthenticatedUser.getAuthorities()` → nomes | lista de `String` | `tokenCustomizer` | `JwtAuthenticationConverter` (→ `hasRole`) |
| `userId` (custom) | `((User) userDetails).getId()` | número (`Long`) | `tokenCustomizer` | `AuthenticatedUserService` |
| `username` (custom) | parâmetro `username` **digitado** | `String` | `tokenCustomizer` | nenhum código de produção o lê; testes de integração o verificam |
| `sub` (padrão) | `context.getPrincipal().getName()` (bytecode do `JwtGenerator`) = **id do cliente** [INFERÊNCIA sobre o valor do nome do principal] | `String` | `JwtGenerator` | `JwtAuthenticationToken.getName()` (não é o usuário) |
| `scope` (padrão) | escopos autorizados, só se não vazios (bytecode: `CollectionUtils.isEmpty`) | lista | `JwtGenerator` | **não é convertido em authorities** (o claim de authorities foi trocado) |
| `jti` (padrão) | UUID gerado pelo `JwtGenerator` [INFERÊNCIA] | `String` | `JwtGenerator` | exposto por `Jwt.getId()` (ver §12) |
| `iss`, `aud`, `iat`, `nbf`, `exp` (padrão) | `JwtGenerator` (emissor da requisição; `aud` = client id; datas) [INFERÊNCIA] | — | `JwtGenerator` | validação de expiração |

- Claims **customizados** só são acrescentados quando o tipo do token é `access_token`.
- **Dependência de claim customizado para identidade:** sim — `userId`.
- **Escopos:** como as authorities são `ROLE_*` e os escopos do cliente são `read`/`write`, a interseção é vazia para as roles atuais ⇒ o access token **não** carrega `scope` [FATO do cálculo; efeito no JWT INFERÊNCIA]. `read`/`write` são nominais: a autorização não usa escopos.

## 11. Authorities [FATO]

```
tb_role.authority ("ROLE_ADMIN"/"ROLE_OPERATOR", seeds)
  → Role.getAuthority()                       (Role implements GrantedAuthority)
  → User.roles → User.getAuthorities()        (devolve o próprio conjunto de roles)
  → loadUserByUsername: new Role(roleId, authority) por linha
  → AuthenticatedUser.getAuthorities()
  → tokenCustomizer: claim "authorities" = lista de authority.getAuthority()
  → ResourceServerConfig.jwtAuthenticationConverter: claim "authorities", prefixo ""
  → GrantedAuthority "ROLE_ADMIN" etc. em JwtAuthenticationToken
  → @PreAuthorize("hasRole('ADMIN')")  ⇒  procura "ROLE_ADMIN"
```
- Formato: strings com prefixo `ROLE_` **já gravadas no banco**; nenhuma etapa acrescenta ou remove prefixo (o conversor usa prefixo vazio). Portanto **role e authority são o mesmo texto** nesta aplicação: `hasRole('ADMIN')` (que acrescenta `ROLE_`) casa com a authority `ROLE_ADMIN`; `hasAuthority` não é usado.
- **Snapshot:** as authorities do JWT são as do momento do login; mudanças posteriores de roles não afetam tokens já emitidos.
- **Usuário sem roles:** não chega a receber token (§7).

## 12. Authorization [FATO / INFERÊNCIA]
- `@EnableMethodSecurity` (`ResourceServerConfig`); regras em controllers: `hasRole('ADMIN')`, `hasRole('ADMIN') or hasRole('OPERATOR')`, `isAuthenticated()`, e dois SpEL em `UserController`:
  - `findById`: `hasRole('ADMIN') OR (hasRole('OPERATOR') AND @authenticatedUserService.isCurrentUser(#id))` — usa o `userId` do JWT (correto por construção);
  - `update`: `hasRole('ADMIN') OR (hasRole('OPERATOR') AND #id == authentication.principal.id)`.
- **`authentication.principal.id` (achado B-0 P1-7) — confirmado por leitura + bytecode:**
  - `authentication` é `JwtAuthenticationToken`; seu **principal é o próprio `Jwt`** (o `AuthenticatedUserService` também depende disso: `getPrincipal() instanceof Jwt`);
  - `Jwt` não possui campo `id`; a propriedade SpEL `id` resolve para **`Jwt.getId()`** (`JwtClaimAccessor`), que lê o claim **`jti`** — verificado no bytecode de `JwtClaimAccessor.getId()`;
  - `jti` é o UUID do token; **não** é o `userId`. `#id` é `Long` (variável de caminho).
  - Diferenciação: `userId` (claim customizado, id do usuário) ≠ `jti` (id do token) ≠ `sub` (id do **cliente**) ≠ `username` (e-mail digitado; claim customizado; não é a propriedade `name` do principal).
  - **[INFERÊNCIA forte]** A comparação `Long == String` é falsa; logo, `OPERATOR` **não** consegue passar por essa cláusula em `PUT /users/{id}` (ADMIN não é afetado, pois `hasRole('ADMIN')` vem antes). **[HIPÓTESE]** O status HTTP e a resposta exatos precisam de execução (B-7/testes). Nenhum teste existente cobre o `PUT` de um `OPERATOR` no próprio usuário.
- **Regras por URL:** ver §15.

## 13. Refresh token [FATO / INFERÊNCIA]
- **Habilitação:** grant `refresh_token` no cliente (`AuthorizationGrantType.REFRESH_TOKEN`); emitido **sempre** no password grant.
- **TTL:** 30 dias, **fixo no código**. **Rotação:** `reuseRefreshTokens(false)` ⇒ cada uso emite novo refresh token e o anterior é rejeitado — **evidência de baseline** (`OAuth2TokenIT`: novo par diferente, claims `username`/`userId`/`authorities` mantidos, reuso do antigo ⇒ `400 invalid_grant`, token inválido ⇒ `400 invalid_grant`).
- **Persistência:** `InMemoryOAuth2AuthorizationService` — perdido no reinício; não compartilhado entre instâncias.
- **Endpoint:** `POST /oauth2/token` com `grant_type=refresh_token` e autenticação do cliente (Basic).
- **Como os claims são reemitidos [INFERÊNCIA]:** o `tokenCustomizer` lê `AuthenticatedUser` dos `details` do principal guardado na autorização em memória (o objeto do login), sem recarregar o usuário do banco.
- **Relação com o estado do usuário:** **não há** nenhum código que consulte o usuário na renovação, nem que remova ou revogue autorizações (busca por `remove/revoke/logout` em `src/main`: 0 ocorrências). `principalName` da autorização é o **id do cliente**, não o usuário — não há chave para localizar as autorizações de um usuário.
- **Achados herdados:**
  - refresh após **alteração de senha**: nada invalida a autorização ⇒ o refresh token continua válido até 30 dias [INFERÊNCIA]; `AccountService.updatePassword/resetPassword` não interagem com tokens (B-3);
  - refresh após **desativação**: o provider padrão de refresh não chama `UserDetailsService`; portanto novos access tokens continuam sendo emitidos com as authorities do login [INFERÊNCIA]; a desativação só impede **novos logins** (`isEnabled`);
  - **logout/deactivate**: não existe logout; `AccountService.deactivateAccount` não está implementado;
  - **revogação:** sem mecanismo explícito da aplicação. O framework mantém, por padrão, os endpoints de revogação e introspecção (`/oauth2/revoke`, `/oauth2/introspect`), acessíveis a um cliente autenticado [INFERÊNCIA]; nenhum código da aplicação os chama.
  - **[HIPÓTESE]** Estes cenários precisam ser executados para confirmação.

## 14. Registered Client [FATO]

| Item | Valor no código |
|------|-----------------|
| Quantidade / repositório | 1 cliente; `InMemoryRegisteredClientRepository` |
| `id` interno | UUID novo a cada inicialização |
| `clientId` | `security.client-id` (existe padrão de desenvolvimento em `application.properties`; sobrescrevível por variável de ambiente) |
| Segredo | `security.client-secret` (**dado sensível**; existe padrão de desenvolvimento; é codificado com BCrypt na inicialização) |
| Método de autenticação do cliente | não declarado (padrão do framework; testes usam HTTP Basic) |
| Grant types | `password` (customizado) e `refresh_token` |
| Escopos | `read`, `write` |
| Formato do access token | `SELF_CONTAINED` |
| Refresh | 30 dias (fixo), sem reuso (rotação) |
| Consentimento / redirect URIs | não configurados (`ClientSettings` padrão) |
| Outros | sem `authorization_code`, `client_credentials`, PKCE, OIDC |

## 15. Filter chains [FATO / INFERÊNCIA]

| Ordem | Bean | Matcher | Mecanismo | Regras / observações |
|:----:|------|---------|-----------|----------------------|
| 1 | `h2SecurityFilterChain` (**condicional**: `spring.h2.console.enabled=true`, perfil `test`) | `PathRequest.toH2Console()` | — | CSRF e `frameOptions` desabilitados; sem regras de autorização ⇒ acesso sem autenticação |
| 2 | `asSecurityFilterChain` | `/oauth2/**`, `/.well-known/**` | Authorization Server (`OAuth2AuthorizationServerConfigurer`) + `oauth2ResourceServer().jwt()` | sem `authorizeHttpRequests`; autenticação do cliente pelo próprio AS; converter/provider do password grant **adicionados** aos padrões |
| 3 | `rsSecurityFilterChain` | **nenhum** (qualquer outra requisição) | `oauth2ResourceServer().jwt()` + CORS | CSRF desabilitado; regras de URL abaixo |

**Regras de URL da cadeia 3 (em ordem):** `GET` `/api/v1/categories/**`, `/api/v1/products/**`, `/api/v1/accounts/**` ⇒ `permitAll`; `POST` `/api/v1/accounts/**` ⇒ `permitAll`; `/docs-asjcatalog`, `/docs-asjcatalog/**`, `/docs-asjcatalog.html`, `/swagger-ui/**` ⇒ `permitAll`; `anyRequest().authenticated()`.
- Sobre `GET`/`POST` `/api/v1/accounts/**` públicos: `GET /me` e `POST /deactivate` dependem apenas do `@PreAuthorize` do método (URL liberada) — B-0 P2-5.
- Sessão: política não configurada explicitamente; CSRF: desabilitado nas cadeias 1 e 3; nas rotas do AS aplica-se o padrão do configurer [INFERÊNCIA].
- CORS: `http.cors(...)` na cadeia 3 **e** um `CorsFilter` com `HIGHEST_PRECEDENCE`; origens de `cors.origins`; métodos `POST, GET, PUT, DELETE, PATCH`; cabeçalhos `Authorization` e `Content-Type`; credenciais permitidas. **Preflight:** `OPTIONS` não está na lista de métodos, mas o CORS confere o método *solicitado* — em princípio não há impacto (**a confirmar em testes**; refina B-0 §9.4).
- **Exceptions das cadeias:** sem `AuthenticationEntryPoint`/`AccessDeniedHandler` próprios. Rotas protegidas sem token válido tendem a receber `401` com `WWW-Authenticate: Bearer` do filtro de bearer token [INFERÊNCIA]; `AccessDeniedException` lançada por `@PreAuthorize` dentro do controller é tratada por `ControllerExceptionHandler` (B-7 confirma) [INFERÊNCIA].
- **Qual cadeia atende qual requisição:** `/oauth2/**` e `/.well-known/**` ⇒ 2; H2 console (quando existir) ⇒ 1; tudo o mais ⇒ 3.

## 16. Criptografia / JWK [FATO]
- Chave: `KeyPairGenerator("RSA")`, **2048 bits**, gerada em `jwkSource()` na inicialização; `RSAKey` com chave privada e `kid = UUID` novo; **uma única chave**; nada lido de arquivo ou propriedade; **não persistida**.
- Consequências observáveis (INFERÊNCIA): reinício ⇒ novas chaves ⇒ **todos os tokens anteriores** (access e, junto com o estado em memória, refresh) deixam de valer; **múltiplas instâncias** ⇒ chaves e autorizações diferentes ⇒ tokens não intercambiáveis.
- Publicação da chave pública: endpoint JWKS do AS (caminho padrão `/oauth2/jwks`); o JavaDoc antigo citava `/.well-known/jwks.json` e "discovery OpenID Connect" — corrigido (OIDC não habilitado).
- `PasswordEncoder`: `BCryptPasswordEncoder` sem força explícita (padrão do framework).

## 17. Exceções [FATO / INFERÊNCIA]

| Exceção | Origem | Capturada / convertida | Formato final conhecido |
|---------|--------|------------------------|-------------------------|
| `OAuth2AuthenticationException` (`invalid_request`) | converter | pelo AS (token endpoint) | resposta OAuth2 de erro (JSON) [INFERÊNCIA] |
| `OAuth2AuthenticationException` (`invalid_client`) | provider / autenticação do cliente | AS | resposta OAuth2 de erro |
| `OAuth2AuthenticationException` (`invalid_grant`, "Invalid credentials" / "…not been activated…") | provider | AS | evidência de baseline: `400` + `$.error = invalid_grant` (refresh); o mesmo mecanismo para o password grant [INFERÊNCIA] |
| `OAuth2AuthenticationException` (`server_error`) | provider | AS | resposta OAuth2 de erro |
| `UsernameNotFoundException` | `UserService.loadUserByUsername` | **capturada no provider** ⇒ `invalid_grant` | idem |
| `AuthenticatedUserNotFoundException` | `AuthenticatedUserService` | `ControllerExceptionHandler` ⇒ `401` (B-0); alcance real em B-7 | `ProblemDetails` |
| `AccessDeniedException` | `@PreAuthorize` | `ControllerExceptionHandler` ⇒ `403` (B-0); a confirmar em B-7 | `ProblemDetails` |
| Erros de JWT (assinatura/expiração/ausente) | `BearerTokenAuthenticationFilter` | filtro ⇒ `401` [INFERÊNCIA] | cabeçalho `WWW-Authenticate` |
| `IllegalArgumentException` | construtor do provider (`Assert.notNull`) | inicialização | falha de contexto |
| `ClassCastException` (potencial) | `(User) userDetails` no provider; `tokenCustomizer` (principal/`details`) | não tratada | não determinável (não ocorre com a configuração atual) |

## 18. Configurações [FATO]

| Configuração | Origem |
|--------------|--------|
| `security.client-id`, `security.client-secret` | `application.properties` (com variáveis de ambiente `CLIENT_ID`/`CLIENT_SECRET` e padrão de desenvolvimento) |
| `security.jwt.duration` | `application.properties` (`JWT_DURATION`, padrão 86400 s) |
| `cors.origins` | `application.properties` (`CORS_ORIGINS`, padrão com origens locais) |
| Refresh TTL 30 dias; `reuseRefreshTokens=false`; escopos `read`/`write`; grants; formato do access token; RSA 2048; CORS (métodos, cabeçalhos, credenciais) | **hardcoded** |
| Chave RSA, `kid`, id interno do cliente | **gerados na inicialização** |
| Cliente registrado, autorizações, consentimentos | **em memória** |
| BCrypt (força), método de autenticação do cliente, algoritmo de assinatura, emissor, caminhos dos endpoints, política de sessão | **padrão do framework** (não explícitos) |
| Provider do password grant | instanciado com `new` (não é bean) |

## 19. Fatos
1. 8 arquivos; 3 filter chains (1 condicional); 1 provider; 1 converter; 1 cliente; 1 chave RSA em memória.
2. O provider confere **só a senha** (username não é comparado) e ignora os escopos solicitados.
3. `authorizedScopes` = authorities ∩ {read, write} ⇒ vazio com as roles `ROLE_*`; sem claim `scope` (bytecode do `JwtGenerator`).
4. `principalName` da autorização = id do cliente.
5. Claims customizados: `authorities`, `userId`, `username` (digitado); `sub` = principal do token do cliente.
6. `AuthenticatedUserService` usa somente o claim `userId`; não verifica `active`.
7. `Jwt.getId()` lê `jti` (bytecode); `authentication.principal` é o `Jwt`.
8. Sem revogação/logout em `src/main`; refresh de 30 dias fixo; rotação ligada; armazenamento em memória.
9. `JwtAuthenticationConverter`: claim `authorities`, prefixo vazio; `scope` não vira authority.
10. Só `GET`/`POST /api/v1/accounts/**` etc. são públicos por URL; `PATCH /me/password` e `PUT /me` exigem autenticação por URL.
11. JavaDocs antigos incorretos corrigidos (ver §22).

### 19.1 Achados herdados — situação após B-6

| # | Achado herdado | Situação | Justificativa (código) |
|---|----------------|----------|------------------------|
| 1 | `AuthenticatedUserService` usa o `userId` do JWT | **Confirmado** | `jwt.getClaim("userId")` + `findById`; é a única fonte de identidade para carregar o `User` (§8). Não verifica `active`. |
| 2 | `UserController.update` usa `authentication.principal.id` | **Confirmado (leitura + bytecode); efeito em execução: hipótese** | O principal é o `Jwt`; `id` resolve para `Jwt.getId()` = claim `jti`, não `userId` (§12). Sem teste para `OPERATOR`. |
| 3 | `loadUserByUsername` cria `User` parcial | **Confirmado** | Campos preenchidos/nulos e uso exato no provider em §7; objeto nunca persistido. |
| 4 | Refresh token após alteração de senha | **Parcialmente confirmado** | Nenhuma invalidação no código (fato); efeito em execução é inferência (§13). |
| 5 | Refresh token após desativação | **Parcialmente confirmado** | Provider padrão de refresh não relê o usuário (inferência); nada revoga (fato). |
| 6 | RSA/JWK gerado e em memória | **Confirmado** | `generateRsa()`/`jwkSource()` (§16); autorizações também em memória. |
| 7 | Claims `authorities`/`username`/`userId` | **Confirmado** (com detalhe) | Três claims custom em `tokenCustomizer`; `username` é o valor digitado; `sub` é o id do cliente (§10). |
| 8 | Password grant | **Confirmado** (com detalhes novos) | Só a senha é conferida; escopos pedidos ignorados; `principalName` = clientId (§6). |
| 9 | `JwtDecoder` | **Confirmado** | Único bean, usado pelas duas cadeias; sem validação de emissor/audiência; sem consulta de estado (§9). |
| 10 | `TokenSettings` | **Confirmado** | SELF_CONTAINED; TTL por propriedade; refresh 30 d fixo; rotação (§13/§14). |

## 20. Inferências
Refresh tratado pelo provider padrão do framework e sem reler o usuário; sem escopo no JWT; `sub` = client id; `jti` UUID; RS256; emissor derivado da requisição; endpoints `/oauth2/jwks`, `/oauth2/revoke`, `/oauth2/introspect` ativos por padrão; tokens deixam de valer após reinício e entre instâncias; access token continua válido até expirar mesmo se o usuário for desativado, trocar a senha ou mudar de roles; `OPERATOR` não passa pela cláusula `#id == authentication.principal.id`.

> **Atualização (fase B-7):** (1) a hipótese sobre o preflight de CORS foi parcialmente refutada — `OPTIONS` não precisa constar em `allowedMethods` e o `CorsFilter` de servlet responde ao preflight antes da cadeia de segurança; cabeçalhos solicitados fora de `Authorization`/`Content-Type` continuariam recusados e `Location` não é exposto; (2) o efeito de `principal.id` sobre `PUT /users/{id}` contradiz o `@Operation` do controller, e a validação do corpo ocorre antes do `@PreAuthorize`; (3) `GET /accounts/me` e `POST /accounts/deactivate` são públicos por URL e só protegidos por método, então um anônimo tende a receber 403 (não 401). Ver [B-7-WEB-LAYER.md](B-7-WEB-LAYER.md).

> **Atualização (fase B-8):** `cors.origins` é dividido por vírgula **sem `trim`** (espaços após a vírgula gerariam padrões que não casam a origem); o console H2 fica habilitado no perfil padrão `test` e o `spring-boot-devtools` traz `spring.h2.console.enabled=true` como padrão (a cadeia `@Order(1)` seria criada também em `dev` com devtools ativo — inferência); `security.jwt.duration` é lido como `Integer` (metadados: `Long`). Ver [B-8-CONFIG-INFRASTRUCTURE.md](B-8-CONFIG-INFRASTRUCTURE.md).

## 21. Hipóteses
Resposta HTTP exata de `PUT /users/{id}` por `OPERATOR` e de falhas de autenticação/autorização; refresh após senha/desativação (comportamento em execução); tempo de resposta diferente para usuário inexistente × senha errada (a senha só é conferida se o usuário existe); preflight CORS sem `OPTIONS` na lista; comportamento com dois `JwtDecoder`/instâncias.

## 22. Pontos a confirmar (B-7 ou testes)
Ordem entre Bean Validation e `@PreAuthorize`; principal em SpEL (`OPERATOR` em `PUT /users/{id}`); authorities efetivas por perfil (`OPERATOR` vs `ADMIN`); refresh depois de alteração de senha e de desativação; JWT após reinício; respostas HTTP de 401/403 (incluindo `GET /accounts/me` anônimo, que passa pela regra de URL pública); CORS (preflight); endpoints efetivamente públicos/protegidos; `/oauth2/revoke` e `/oauth2/introspect`; presença do claim `scope`.

## 23. Decisões adiadas
Nenhuma correção: revogação/invalidação de refresh tokens, persistência e rotação da chave, escopos, `sub`, verificação de `active` nos "me", SpEL de `update`, caminhos de documentação por perfil, tempo constante no login, JavaDoc de `web` — ficam para depois do mapeamento completo (B-7 a B-9) e da fase de testes.

## 24. JavaDoc adicionado ou ajustado

| Arquivo | Alterações |
|---------|-----------|
| `AuthorizationServerConfig` | Classe reescrita e todos os beans/métodos documentados. **Correções:** "Authorization Code Flow" (não configurado); claims (faltava `userId`); "grant password" (também `refresh_token`); JWKS `/.well-known/jwks.json` (padrão `/oauth2/jwks`); "discovery OpenID Connect" (não habilitado); "React SPA"; conselhos genéricos removidos. |
| `ResourceServerConfig` | Classe reescrita: lista real de rotas públicas (o antigo omitia `accounts` e citava `/docs-dscatalog`), ordem das cadeias, conversor JWT, CORS; métodos documentados. |
| `CustomPasswordAuthenticationProvider` | Sequência real (inclui refresh token, ignora escopos, `principalName`, contexto substituído); **`validateCredentials`** (dizia comparar username — só a senha); novo JavaDoc de `validateUserStatus`. |
| `CustomPasswordAuthenticationConverter` / `Token` | Comportamento real dos parâmetros; escopos não usados pelo provider. |
| `AuthenticatedUser` | Reescrito (construtor de 3 argumentos, papel real; removidas afirmações sobre `@PreAuthorize`). |
| `AuthenticatedUserService` | Fonte da identidade, exceções, ausência de verificação de `active`, usos. |
| `SecurityBeansConfig` | BCrypt com força padrão; "criado sempre que necessário" removido. |

Tags `@apiNote`/`@implNote` removidas do pacote; doclint (`-Xdoclint:all,-missing`) sem avisos.

## 25. O que **não** foi feito
Nenhuma alteração de segurança, OAuth2, JWT, claims, authorities, roles, TTL, refresh, filter chains, providers, converters, controllers, services, repositories, DTOs, entidades, validation, `pom.xml`, resources, properties ou testes; nenhum teste executado; nenhum commit.
