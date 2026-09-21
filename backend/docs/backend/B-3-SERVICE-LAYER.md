# ASJCatalog Backend — B-3: Camada de Serviços

> **Fase:** B-3 — análise e documentação (JavaDoc) de `service`.
> **Pré-requisitos:** [B-0](B-0-BACKEND-INVENTORY.md), [B-1](B-1-DOMAIN-LAYER.md) e [B-2](B-2-REPOSITORY-LAYER.md).
> **Escopo alterado:** somente comentários JavaDoc em `src/main/java/**/service/*.java` (os 6 services) e esta documentação, com notas pontuais nos documentos B-0/B-1/B-2. Nenhuma linha de código executável, anotação, assinatura ou transação foi alterada.

Rótulos: **[FATO]** confirmado no código; **[INFERÊNCIA]** conclusão razoável, sem execução dedicada; **[HIPÓTESE]** precisa de confirmação. Nenhum teste foi executado nesta fase; quando cito "evidência de baseline", refiro-me a resultados já registrados no B-0.

---

## 1. Objetivo da camada

Os services orquestram os casos de uso: recebem DTOs já validados pela camada web, consultam/gravam via repositories, aplicam as regras de aplicação, definem as fronteiras transacionais e devolvem DTOs de resposta. Não há autorização nos services (nenhuma referência a `SecurityContext`, `Authentication`, `@PreAuthorize` ou `hasRole` em `service/`) [FATO].

## 2. Composição dos services [FATO]

| Service | Papel | Métodos públicos |
|---------|-------|------------------|
| `ProductService` | catálogo de produtos e vínculo com categorias | `search`, `findAllPaged`, `findById`, `create`, `update`, `activate`, `deactivate`, `delete` |
| `UserService` (`UserDetailsService`) | administração de usuários + carga de usuário para login | `search`, `findById`, `create`, `update`, `activate`, `deactivate`, `delete`, `loadUserByUsername` |
| `AccountService` | ciclo de vida da conta do próprio usuário | `register`, `confirmEmail`, `requestPasswordRecovery`, `resendActivationEmail`, `resetPassword`, `updateAuthenticatedUser`, `updatePassword`, `getAuthenticatedUser`, `deactivateAccount` |
| `CategoryService` | categorias | `search`, `findById`, `create`, `update`, `activate`, `deactivate`, `delete` |
| `TokenService` | tokens de ativação/recuperação | `createActivationToken`, `createPasswordRecoveryToken`, `disableAllActivationTokens`, `disableAllPasswordRecoveryTokens`, `findAndValidateToken` |
| `EmailService` | envio de e-mail e registro em `tb_email` | `sendActivationEmailAsync`, `sendPasswordRecoveryEmailAsync`, `sendActivationEmail`, `sendPasswordRecoveryEmail` |

Total: **6 services**, todos `@Service`; 5 exceções em `service.exception` (consumidas pelos services e pelo handler).

## 3. Mapa de dependências [FATO]

| Service | Repositories | Mappers | Outros beans / serviços | Propriedades |
|---------|--------------|---------|-------------------------|--------------|
| `ProductService` | `ProductRepository`, `CategoryRepository` | `ProductMapper` | `IdentifiableUtils` (estático), projection `ProductProjection` | — |
| `UserService` | `UserRepository`, `RoleRepository` | `UserMapper` | `PasswordEncoder`; projection `UserDetailsProjection` | — |
| `AccountService` | `UserRepository`, `RoleRepository` | `UserMapper` | `PasswordEncoder`, `TokenService`, `EmailService`, `AuthenticatedUserService` (pacote `security.auth`) | — |
| `CategoryService` | `CategoryRepository` | `CategoryMapper` | — | — |
| `TokenService` | `TokenRepository` | — | domínio `Token` | `account.activation.token.hours`, `account.password-recovery.token.minutes` |
| `EmailService` | `EmailRepository` | — | `JavaMailSender`, `SpringTemplateEngine` | `frontend.url`, `backend.url` (esta não é usada) |

- Dependências entre services: apenas `AccountService → TokenService` e `AccountService → EmailService`. Não há ciclos.
- **Validators:** nenhum service chama validator; a validação ocorre antes, via `@Valid` nos controllers (o repositório é consultado pelos validators, não pelos services, para unicidade/existência).
- **Mappers** são chamados apenas pelos services (nunca por controllers).
- Chamadores: os 4 controllers chamam `Product/User/Category/AccountService`; `TokenService` e `EmailService` só são chamados por `AccountService`; `UserService.loadUserByUsername` só é chamado pelo `CustomPasswordAuthenticationProvider`.

## 4. ProductService

### 4.1 Visão geral
Chamador único: `ProductController`. Não usa validators nem autorização. O indicador `active` é apenas gravado (criar → `true`; `activate`/`deactivate`); **nenhuma consulta o utiliza como filtro** [FATO].

### 4.2 `findAllPaged` × `search` [FATO]

| Aspecto | `findAllPaged` (usada por `GET /products`) | `search` (**sem chamadores** em `src/main`) |
|---------|---------------------------------------------|---------------------------------------------|
| Consulta | `searchProducts` (SQL nativa, projeção) + `searchProductsWithCategories` (JPQL `JOIN FETCH`) | `findByNameContainingIgnoreCase` ou `findAll(Pageable)` |
| Filtro de nome | repassa `name` sem `trim`/tratamento; controller usa `""` | `trim`; vazio/nulo → sem filtro |
| Filtro de categoria | sim, via `categoryId` ("0" → lista vazia) | não |
| Produtos sem categoria | **não aparecem** (`INNER JOIN`) | aparecem |
| Ordenação | colunas `id`/`name` do `SELECT` externo | qualquer propriedade da entidade |
| Categorias na resposta | já carregadas (`JOIN FETCH`) | carregamento sob demanda (N+1 provável [INFERÊNCIA]) |
| Transação | `readOnly` | `readOnly` |

### 4.3 Sequência de `findAllPaged` [FATO]
1. `categoryId` → lista de ids: `"0"` ⇒ lista vazia; senão `split(",")` + `Long.parseLong` (nulo ⇒ `NullPointerException`; não numérico ⇒ `NumberFormatException`; nenhuma tratada).
2. `searchProducts(categoryIds, name, pageable)` → `Page<ProductProjection>`.
3. `page.map(ProductProjection::getId).toList()` → ids.
4. `searchProductsWithCategories(ids)` — **chamada mesmo se `ids` estiver vazio** (não há guarda).
5. `IdentifiableUtils.reorderByReference(products, page.getContent())`: indexa por id, percorre a ordem da página, descarta ids sem produto.
6. `productMapper.toResponse` em cada produto; `new PageImpl<>(responses, pageable, page.getTotalElements())`.

Confirmações de B-2: (a) **`categoryIds` vazio**: o service só cria lista vazia quando recebe `"0"`, nunca `null` — [FATO]; o efeito da lista vazia na SQL segue como hipótese fora do H2. (b) **Página sem produtos**: `searchProductsWithCategories` é chamado com lista vazia — [FATO da chamada]; o efeito no Hibernate/PostgreSQL permanece **[HIPÓTESE]**.

### 4.4 Demais operações [FATO]
- `findById`: `findById` (404 `error.product.notFound`) + `toDetailsResponse` (categorias sob demanda, dentro da transação de leitura [INFERÊNCIA]).
- `create`: mapper (nome, descrição, preço, imgUrl) → `setActive(true)` → `syncCategories` → `save` → `toResponse`. **Não usa** `request.date()`. O preço é opcional no DTO (`@Positive` aceita `null`; B-4 §4.2). Unicidade do nome e existência das categorias são de validator; o banco tem `UNIQUE`.
- `update`: `getReferenceById` dentro de `try`; `updateEntity` (campos nulos não sobrescrevem); `categoryIds != null` ⇒ `syncCategories`; `save`; `EntityNotFoundException` ⇒ `ResourceNotFoundException("error.product.notFound")`.
- `activate`/`deactivate`/`changeStatus`: `findById`; idempotente; sem `save` (dirty checking no commit).
- `delete`: `findById` (404) → `delete` → `DataIntegrityViolationException` ⇒ `DatabaseException("error.database.product.relatedEntities")`.
- `syncCategories`: `clear()` primeiro; nulo/vazio ⇒ retorna (produto sem categorias); `findAllById`; **compara quantidades**; diferente ⇒ `ResourceNotFoundException("error.product.categories.notFound")`.

### 4.5 `getReferenceById` — confirmação [FATO/INFERÊNCIA]
Em `update`, os primeiros acessos ao proxy (setters do mapper, `getCategories()` de `syncCategories` ou `save`) estão todos **dentro** do `try`, então a conversão de `EntityNotFoundException` cobre a inexistência do id [FATO do desenho]; o momento exato da exceção depende do Hibernate [INFERÊNCIA]. O `save` final é chamado sobre entidade já gerenciada.

### 4.6 Pontos específicos
- **Ids repetidos em `categoryIds`**: `syncCategories` compara `categories.size() != categoryIds.size()`; `findAllById` tende a devolver cada categoria uma vez ⇒ id repetido daria `error.product.categories.notFound` mesmo existindo [INFERÊNCIA].
- **`delete` e `DataIntegrityViolationException`**: a exclusão costuma ser sincronizada no commit (após o método); o `catch` pode não interceptar a violação [HIPÓTESE]. Na prática, só `tb_product_category` referencia produtos, e a JPA a limpa (lado dono) [FATO do mapping].
- **Categorias inexistentes** também são barradas antes pelo validator (`existsById` por id) [FATO em B-2].

## 5. UserService

### 5.1 Dois papéis [FATO]
1. **Administração** (chamada por `UserController`): `search`, `findById`, `create`, `update`, `activate`, `deactivate`, `delete`.
2. **`UserDetailsService`**: `loadUserByUsername`, injetado em `AuthorizationServerConfig` e repassado ao `CustomPasswordAuthenticationProvider` (único chamador).

### 5.2 Operações [FATO]
- `search`: `trim`; vazio ⇒ `findAll`; senão `findByFirstNameContainingIgnoreCase` (só `firstName`); `toResponsePage` percorre `roles` ⇒ N+1 provável [INFERÊNCIA].
- `create`: `findRolesByIdsOrThrow(roleIds)` → `toEntity` → `passwordEncoder.encode` → `activate()` → `save` → `toResponse`. E-mail **gravado como recebido**.
- **`roleIds` nulo/vazio** (confirma B-2): `findRolesByIdsOrThrow` devolve `Collections.emptySet()`; o validator `@ValidRoles` também aceita ⇒ **usuário sem roles pode ser criado** [FATO]. Como `loadUserByUsername` usa `INNER JOIN` ⇒ lista vazia ⇒ `UsernameNotFoundException` ⇒ o provider responde `invalid_grant`: **esse usuário não consegue autenticar** [FATO do caminho de código; execução não realizada].
- `update`: `getReferenceById` em `try`; `updateEntity` copia nome/sobrenome/e-mail **sem checar nulos** (o DTO exige); `roleIds` nulo mantém, conjunto (mesmo vazio) substitui via `clear()+addAll()`; `password` nulo mantém, senão codifica; `save`; `active` não muda. Um `PUT` com `roleIds=[]` remove todas as roles.
- `activate`/`deactivate`: `findEntityById`; idempotente; sem `save`.
- `delete`: `findById` + `delete`; `User.tokens` tem `cascade ALL`, e os vínculos `tb_user_role` são removidos (lado dono); sem tratamento de integridade; não impede a exclusão do próprio solicitante nem do último administrador [FATO: nenhum código nesse sentido].
- `findEntityById` está anotado com `@Transactional(readOnly = true)` **em método privado**: sem efeito prático (o proxy não intercepta) [INFERÊNCIA].

### 5.3 `loadUserByUsername` e o `User` parcial (investigação pedida) [FATO]
- Executa **uma** consulta nativa (`searchUserAndRolesByEmail`), sem `@Transactional`, sem carregamento tardio.
- Cria `new User()` com `id`, e-mail (`getUsername()` da projeção), senha (hash), `active` (da primeira linha) e uma `Role(roleId, authority)` por linha (`addRole`). `firstName`, `lastName` e `tokens` ficam nulos/vazios; o objeto **nunca é persistido**.
- **Uso posterior** (`CustomPasswordAuthenticationProvider`, único consumidor): `getPassword()` em `passwordEncoder.matches`; `isEnabled()`/`isAccountNonLocked()`/`isAccountNonExpired()`/`isCredentialsNonExpired()` em `validateUserStatus`; `getAuthorities()` para calcular escopos; `((User) userDetails).getId()` para o `AuthenticatedUser`. O `AuthenticatedUser` recebe como `username` o parâmetro **digitado** no request (`username`), não `userDetails.getUsername()`. Em `src/main`, `getFirstName()/getLastName()` só são lidos em `UserMapper` (sobre entidades carregadas), `AccountService` e `EmailService` (também sobre entidades carregadas) — **nenhuma leitura de nome/sobrenome sobre esse objeto parcial**.
- Comparação do e-mail: exata (sem `trim`, sem ignorar caixa). Usuário inativo **é** carregado; a recusa vem depois, no provider (`isEnabled()`).
- Relação entre `username`/e-mail e identidade: e-mail é o `username` no login; após o login, a identidade usada pelos endpoints "me" é o claim `userId`, não o e-mail (ver §14).

## 6. AccountService

### 6.1 Fluxos (sequência real) [FATO]

**Registro** — `POST /register`
`AccountController.register` → `AccountService.register`: `RoleRepository.findByAuthority("ROLE_OPERATOR")` (senão `IllegalStateException`) → `UserMapper.toEntity` (e-mail/nomes como recebidos) → `PasswordEncoder.encode` → `user.deactivate()` → `UserRepository.save` → `TokenService.createActivationToken` (→ `TokenRepository.save`) → `EmailService.sendActivationEmailAsync` (retorno descartado) → `UserMapper.toResponse`.

**Ativação** — `GET /activate?token=`
`confirmEmail` → `TokenService.findAndValidateToken(valor, ACTIVATION)` (`findByToken` → 404 se ausente; `Token.validate` → `InvalidTokenException`) → `token.getUser()` (tardio) → `user.activate()` → `token.disable()`. Sem `save` (dirty checking). Não verifica se o usuário já estava ativo.

**Reenvio de ativação** — `POST /resend-activation`
`resendActivationEmail` → `findByEmail` (exato); ausente **ou** já ativo ⇒ retorna silenciosamente → `TokenService.disableAllActivationTokens` → `createActivationToken` → `EmailService.sendActivationEmailAsync`.

**Recuperação de senha** — `POST /password-recovery`
`requestPasswordRecovery` → `findByEmail(email).ifPresent(...)`: cria token `PASSWORD_RECOVERY` → `EmailService.sendPasswordRecoveryEmailAsync`. **Ausente ⇒ nada** (o JavaDoc antigo dizia que lançava `ResourceNotFoundException`; corrigido). Não checa `active` e não desabilita tokens de recuperação anteriores.

**Redefinição** — `POST /reset-password`
`resetPassword` → `findAndValidateToken(valor, PASSWORD_RECOVERY)` → `user.setPassword(encode(...))` → `token.disable()` → `UserRepository.save`. Não ativa a conta, não desabilita outros tokens, não interage com tokens OAuth2 emitidos.

**Perfil do autenticado** — `PUT /me`
`updateAuthenticatedUser` → `AuthenticatedUserService.getAuthenticatedUser()` → `trim` em nome/sobrenome; **`trim + toLowerCase` no e-mail (única normalização de e-mail do sistema)** → `UserRepository.save` → `toResponse`.

**Troca de senha** — `PATCH /me/password`
`updatePassword`: `newPassword == confirmPassword` → usuário autenticado → `matches(current, hash)` → `!matches(new, hash)` → `setPassword(encode(new))` (sem `save`; dirty checking). Falhas ⇒ `PasswordUpdateException` (chaves `error.account.password.*`).

**Consulta do autenticado** — `GET /me`: `getAuthenticatedUser` → `toResponse` (readOnly).

**Desativação** — `POST /deactivate`: **não implementado**: `deactivateAccount()` lança `UnsupportedOperationException("Unimplemented method 'deactivateAccount'")`, sem `@Transactional` [FATO]. O endpoint está publicado e documentado; o handler genérico responde 500 (B-0).

### 6.2 Observações do AccountService [FATO]
- `register` e `resendActivationEmail` declaram `throws MessagingException`, mas **nada em seus corpos lança essa exceção verificada** (os métodos `...Async` não a declaram).
- `import jakarta.validation.Valid` não é usado.
- `TokenService` é chamado dentro da transação de `AccountService` (participa dela).

## 7. CategoryService [FATO]
- `search`: `trim`; vazio ⇒ `findAll`; senão `findByNameContainingIgnoreCase`; resposta só `id`/`name` ⇒ sem N+1.
- `findById`, `create` (`setActive(true)`; unicidade por validator/`UNIQUE`), `update` (`getReferenceById` em `try`; `updateEntity` só sobrescreve campos não nulos; `active` intacto), `activate`/`deactivate` (idempotentes; sem `save`), `delete` (`findById` + `delete`).
- **Sem relação com produtos no código do service**: não há checagem de "categoria em uso". `Category.products` é lado inverso; a JPA não remove vínculos de `tb_product_category`. Excluir categoria com produtos tende a violar a FK **no commit** e propagar `DataIntegrityViolationException` (handler ⇒ 409) [INFERÊNCIA].
- Exceção: `ResourceNotFoundException("error.category.notFound")` em `findEntityById` e no `update`.

## 8. TokenService [FATO]
- Classe `@Transactional`; `createActivationToken`/`createPasswordRecoveryToken` → `Token.activationToken(user, horas)` / `Token.passwordRecoveryToken(user, minutos)` (UUID `randomUUID`, expiração = agora + prazo) → `save`. Não desabilitam tokens anteriores; não adicionam o token a `user.tokens`.
- `disableAllActivationTokens`: `findByUserAndTypeAndDisabledFalse(user, ACTIVATION)` → `Token::disable` (inclui vencidos; sem `save`). `disableAllPasswordRecoveryTokens`: idem para `PASSWORD_RECOVERY` — **sem chamadores**.
- `findAndValidateToken`: `findByToken` → `orElseThrow(ResourceNotFoundException("error.token.notFound"))` → `token.validate(type)` → devolve o token (não o altera).
- **`isValid()` × `validate()` × service:** `Token.isValid()` (booleano; `!disabled && !expired`; não confere tipo) **não é usado** por nenhum código de produção nem de teste. O service usa somente `Token.validate(TokenType)`, que confere na ordem tipo → desabilitado → expirado e lança `InvalidTokenException` com chave `error.token.type.invalid` / `error.token.disabled` / `error.token.expired`. O service não faz nenhuma verificação própria de validade.

## 9. EmailService

### 9.1 Estrutura [FATO]
- `sendActivationEmail(name, email, token)`: `MimeMessage` multipart; `Context` (`nome`, `titulo`, `texto`, `linkConfirmacao = frontend.url + "/activate-account?token=" + token`); `templateEngine.process("activate_user_by_email_template", ctx)`; assunto "Confirmação de Cadastro"; `From` `nao-responder@asjcatalog.com.br`; logo inline; `JavaMailSender.send`; log; `registerEmailLog(...)`.
- `sendPasswordRecoveryEmail(user, token)`: análogo, template `reset_password_email_template`, `linkRedefinicaoSenha = frontend.url + "/reset-password?token=" + token`; **não define a variável `texto`**, que o template referencia.
- `...Async`: chamam os síncronos dentro de `try/catch (Exception)`; em erro, `logger.error` + `CompletableFuture.failedFuture(ex)`; em sucesso, `completedFuture(null)`.
- `registerEmailLog`: `new Email(new EmailRegisterRequest("asjcatalog@gmail.com", destinatário, rótulo))` → `emailRepository.save`. O registro só é gravado **após** o envio ter sucesso; status sempre `PENDING`; `content` = rótulo fixo ("Confirmação de Cadastro"/"Redefinição de Senha"); remetente registrado ≠ `From` da mensagem.
- `backendUrl` (`@Value("${backend.url}")`) é injetado e **não utilizado**.

### 9.2 `@Async` sem `@EnableAsync` [FATO/INFERÊNCIA/HIPÓTESE]
- **[FATO]** Há dois métodos `@Async` e nenhum `@EnableAsync`/`AsyncConfigurer` em `src/main`.
- **[INFERÊNCIA]** Sem `@EnableAsync`, o Spring não cria o interceptor assíncrono, então os métodos executam de forma síncrona na thread de quem chama, dentro da transação de `AccountService`, antes do commit (envio ocorre antes de o usuário/token estarem confirmados no banco).
- **[HIPÓTESE]** O comportamento efetivo em execução (e o efeito de eventual auto-configuração externa) não foi verificado.
> **Atualização (fase B-8):** confirmado que não há `@EnableAsync`, `AsyncConfigurer` nem executor próprio (só o `applicationTaskExecutor` auto-configurado, sem uso); a execução síncrona continua sendo inferência. Ver [B-8-CONFIG-INFRASTRUCTURE.md](B-8-CONFIG-INFRASTRUCTURE.md) §8.
- Consequências independentes dessa questão **[FATO]**: falhas de envio nunca chegam ao chamador (o futuro é descartado) — o usuário é registrado mesmo sem e-mail; não há retry nem atualização de status para `ERROR`.

### 9.3 Transação e persistência
Sem `@Transactional`. O `save` do registro usa a transação de quem chama, se houver (comportamento padrão de `SimpleJpaRepository.save` [INFERÊNCIA]); com `@Async` efetivo, rodaria em outra thread sem transação de chamador (cada `save` abriria a sua).

## 10. Fronteiras transacionais [FATO]

Contagem em `service/`: 32 ocorrências de `@Transactional` (uma na classe `TokenService`) e 2 de `@Async`.

| Service | `readOnly = true` | Escrita (padrão) | Sem transação |
|---------|-------------------|------------------|---------------|
| `ProductService` | `search`, `findAllPaged`, `findById` | `create`, `update`, `activate`, `deactivate`, `delete` | — |
| `UserService` | `search`, `findById`, `findEntityById` (**privado, sem efeito** [INFERÊNCIA]) | `create`, `update`, `activate`, `deactivate`, `delete` | `loadUserByUsername` |
| `AccountService` | `getAuthenticatedUser` | `register`, `confirmEmail`, `requestPasswordRecovery`, `resendActivationEmail`, `resetPassword`, `updateAuthenticatedUser`, `updatePassword` | `deactivateAccount` |
| `CategoryService` | `search`, `findById` | `create`, `update`, `activate`, `deactivate`, `delete` | — |
| `TokenService` | — | todos (anotação na classe) | — |
| `EmailService` | — | — | todos (`@Async` em `sendActivationEmailAsync` e `sendPasswordRecoveryEmailAsync`) |

Operações dentro das fronteiras (resumo):
- `ProductService.findAllPaged` (RO): `ProductRepository` ×2 (nativa + JPQL) e `IdentifiableUtils`.
- `ProductService.create/update` (RW): `ProductRepository`, `CategoryRepository.findAllById`.
- `UserService.create/update` (RW): `RoleRepository.findAllById`, `PasswordEncoder`, `UserRepository`.
- `AccountService.register` (RW): `RoleRepository`, `UserRepository`, `TokenService`/`TokenRepository`, **`EmailService` (SMTP + Thymeleaf + `EmailRepository`) dentro da mesma fronteira** [INFERÊNCIA se `@Async` não estiver ativo].
- `AccountService.confirmEmail/resetPassword/updatePassword`: dependem de **dirty checking** (sem `save` em `confirmEmail` e `updatePassword`) e de **lazy loading** de `token.getUser()` [FATO do código].
- Operação assíncrona: apenas `EmailService.*Async` (ver §9.2). `open-in-view=false` (B-0): fora dessas fronteiras, coleções tardias não são acessíveis [INFERÊNCIA].

Diferenciação pedida: (a) **ausência explícita de `@Transactional`**: `loadUserByUsername`, `deactivateAccount`, `EmailService` inteiro; (b) **herdada**: repositórios Spring Data têm transações próprias nos métodos herdados (comportamento padrão do framework [INFERÊNCIA]); métodos de consulta declarados **não** ganham transação por padrão [INFERÊNCIA]; (c) **participação**: `TokenService` e o `save` de `EmailRepository` ingressam na transação do chamador quando ela existe.

## 11. Regras de aplicação

Legenda: **A** = implementada no service; **B** = delegada a validators; **C** = delegada ao domínio; **D** = imposta pelo banco; **E** = esperada, mas **não** implementada.

| Contexto | A — service | B — validators/DTO | C — domínio | D — banco | E — não implementadas |
|----------|-------------|--------------------|-------------|-----------|------------------------|
| Produto | criado ativo; categorias substituídas e verificadas por contagem; status idempotente; `DataIntegrityViolationException` → `DatabaseException` (com ressalva §4.6) | nome único e formato; categorias existem (`existsById`); preço `@Positive`; `imgUrl`;; o preço aceita `null` (B-4 §4.2) `categoryIds @NotEmpty`; `date @PastOrPresent` | `@PrePersist/@PreUpdate` de datas | `name` `UNIQUE NOT NULL`; FKs de `tb_product_category` | ocultar/bloquear produto inativo; exigir categoria na entidade; checar nome único no service; uso do `date` do request |
| Usuário (admin) | criado ativo; senha codificada; roles resolvidas e todas obrigatórias no service; status idempotente; `roleIds` nulo mantém, vazio limpa | e-mail válido (DNS) e único; senha forte e sem dados pessoais; roles existem; tamanhos de nome | `activate/deactivate`; `isEnabled = active` | `email` `UNIQUE NOT NULL`; FKs `tb_user_role`/`tb_token` | exigir ≥ 1 role; impedir excluir a si mesmo/último admin; normalizar e-mail; invalidar tokens OAuth2 ao desativar/excluir |
| Conta | registro com role fixa e conta inativa; ativação por token; recuperação silenciosa; reenvio só p/ inativo; reset com token; perfil normaliza e-mail; troca de senha com 3 checagens | e-mail/senha fortes; `@UniqueEmailForAuthenticatedUser`; formatos | `Token.validate`; `User.activate` | `token` `UNIQUE`; `type` com `check` | `deactivateAccount`; invalidar recovery tokens anteriores; confirmar novo e-mail; revogar sessões ao trocar/redefinir senha; verificar `active` na recuperação |
| Categoria | criada ativa; status idempotente; update parcial | nome único/formato | callbacks de datas | `name` `UNIQUE NOT NULL`; FK de `tb_product_category` | verificar produtos vinculados antes de excluir; efeito de `active` |
| Token | criação com prazo configurável; desabilitação em massa; busca+validação delegada | — | `Token.validate/isExpired/disable`, fábricas | `token` `UNIQUE`; `user_id NOT NULL` | uso de `isValid()`; invalidação automática por outro token |
| E-mail | montagem, envio, registro pós-envio | `@Email` no DTO interno | `Email` nasce `PENDING` | colunas `NOT NULL`, `check` de status | atualização de `status` (`SENT`/`ERROR`); retry; execução assíncrona real; envio fora da transação |

## 12. Exceções [FATO]

| Exceção | Condição | Onde é criada / camada | Tratamento posterior conhecido |
|---------|----------|------------------------|--------------------------------|
| `ResourceNotFoundException` | produto/categoria/usuário inexistente (`findEntityById`; `update` via `EntityNotFoundException`); categorias/roles não encontradas (`syncCategories`, `findRolesByIdsOrThrow`); token inexistente | Product/Category/User/TokenService | handler ⇒ 404 |
| `DatabaseException` | `DataIntegrityViolationException` capturada em `ProductService.delete` | ProductService | handler ⇒ 400 |
| `InvalidTokenException` | tipo diferente, desabilitado ou expirado | **domínio** (`Token.validate`), acionada por `TokenService.findAndValidateToken` | handler ⇒ 400 |
| `PasswordUpdateException` | confirmação diferente; senha atual inválida; nova igual à atual | AccountService (privados) | handler ⇒ 422 |
| `AuthenticatedUserNotFoundException` | sem `Jwt` como principal; sem claim `userId`; usuário inexistente | **`AuthenticatedUserService`** (security), propagada por `AccountService` | handler ⇒ 401 |
| `IllegalStateException` | `ROLE_OPERATOR` ausente em `register` | AccountService | handler genérico ⇒ 500 |
| `UnsupportedOperationException` | `deactivateAccount` sempre | AccountService | handler genérico ⇒ 500 |
| `UsernameNotFoundException` | `loadUserByUsername` sem linhas | UserService | capturada pelo provider ⇒ `invalid_grant` |
| `EntityNotFoundException` | proxy de `getReferenceById` de id inexistente | JPA (capturada em 3 `update`) | convertida em `ResourceNotFoundException` |
| `DataIntegrityViolationException` | violação de unicidade/FK | Spring/JPA | capturada só em `ProductService.delete`; nos demais casos propaga ⇒ handler ⇒ 409 |
| `NullPointerException` / `NumberFormatException` | `categoryId` nulo / não numérico em `findAllPaged` | ProductService (efeito) | handler genérico ⇒ 500 |
| `MessagingException` | declarada em `register`/`resendActivationEmail`/sync do `EmailService` | — (nunca lançada em `register`/`resend`) | — |
| Erros de envio de e-mail | qualquer `Exception` no `...Async` | EmailService | logados e devolvidos em `CompletableFuture` descartado |

## 13. Fluxos de negócio (visão sequencial)

```
Listagem de produtos
 ProductController.findAll → ProductService.findAllPaged
   → ProductRepository.searchProducts (nativa → ProductProjection)
   → ProductRepository.searchProductsWithCategories (JPQL JOIN FETCH)
   → IdentifiableUtils.reorderByReference → ProductMapper.toResponse → PageImpl

Login (grant password)
 POST /oauth2/token → CustomPasswordAuthenticationProvider
   → UserService.loadUserByUsername → UserRepository.searchUserAndRolesByEmail → UserDetailsProjection
   → User parcial → passwordEncoder.matches / isEnabled → AuthenticatedUser → JWT

Registro
 AccountController.register → AccountService.register
   → RoleRepository → UserMapper → PasswordEncoder → UserRepository.save
   → TokenService.createActivationToken → TokenRepository.save
   → EmailService.sendActivationEmailAsync → (Thymeleaf + JavaMailSender) → EmailRepository.save

Ativação:   AccountService.confirmEmail → TokenService.findAndValidateToken → Token.validate → user.activate(); token.disable()
Recuperação: AccountService.requestPasswordRecovery → UserRepository.findByEmail → TokenService.createPasswordRecoveryToken → EmailService.sendPasswordRecoveryEmailAsync
Reset:       AccountService.resetPassword → findAndValidateToken → encode → token.disable() → UserRepository.save
Perfil/Senha: AccountService.update* → AuthenticatedUserService (JWT userId → UserRepository.findById) → alteração
```

Todos os fluxos acima foram confirmados por leitura das chamadas em `src/main`.

## 14. Segurança e identidade

- **Autorização:** nunca nos services; está nos controllers (`@PreAuthorize`) e no `ResourceServerConfig` (B-0). Os services de Product/Category/User recebem apenas ids de recurso, sem conhecer quem chama.
- **Usuário autenticado:** somente `AccountService` (nos 3 fluxos "me") o utiliza, via `AuthenticatedUserService.getAuthenticatedUser()`: lê `SecurityContextHolder`, exige `Jwt` como principal, lê o claim **`userId`** (`> 0`) e carrega o `User` com `UserRepository.findById`. Não usa o e-mail nem o claim `username`.
- **Autenticado × operador/admin:** os endpoints "me" atuam sempre sobre o dono do token; as operações administrativas (`UserService`) atuam sobre o id informado na URL. A regra "OPERATOR só edita a si mesmo" está em SpEL do `UserController` (hipótese P1-7 do B-0: `authentication.principal.id` num `Jwt`); os services não permitem confirmar isso — **permanece hipótese**.
- **Login:** `UserService.loadUserByUsername` é a ponte entre o banco e o grant `password`; o `AuthenticatedUser`/claims (`userId`, `username`, `authorities`) são montados no provider/config (camada `security`).

## 15. Operações externas/assíncronas [FATO]

| Operação | Onde | Observação |
|----------|------|------------|
| SMTP (`JavaMailSender.send`) | `EmailService.send*Email` | dentro do fluxo de `AccountService`; erros capturados nos wrappers |
| Renderização Thymeleaf | `EmailService` | templates `activate_user_by_email_template`, `reset_password_email_template` |
| Leitura de classpath (logo inline) | `EmailService` | `/static/image/logo-ASJ-Catalog-favicon.ico` |
| `@Async` | `EmailService` (2 métodos) | sem `@EnableAsync` (§9.2) |
| `PasswordEncoder` (BCrypt) | User/Account | custo computacional dentro da transação |
| DNS (MX) | **não** nos services (nos validators — B-0) | — |

## 16. Pontos de atenção (sem correção)

| # | Observação | Rótulo |
|---|------------|--------|
| 1 | `AccountService.requestPasswordRecovery`: JavaDoc antigo dizia lançar `ResourceNotFoundException`; o código não lança (silencioso). Corrigido no JavaDoc. | FATO |
| 2 | `ProductService.findAllPaged`: JavaDoc antigo dizia que `categoryId` nulo não filtra; o código lança `NullPointerException`. Corrigido no JavaDoc. | FATO |
| 3 | JavaDocs antigos afirmavam que `deactivate` "oculta de listagens" (Product/User/Category): não há tal lógica. Corrigido. | FATO |
| 4 | `register`/`resendActivationEmail`: `throws MessagingException` nunca disparado; falhas de e-mail são silenciosas. | FATO |
| 5 | E-mail sem normalização em `register`, `UserService.create/update`; normalizado só em `updateAuthenticatedUser`; consultas de login/recuperação/reenvio são exatas ⇒ dependência de caixa. **B-4 confirmou o mapa completo de normalização (B-4 §16):** o único ponto de gravação normalizada é `AccountService.updateAuthenticatedUser`; os validators normalizam apenas para comparar (`@ValidEmail` valida o valor com `trim`, mas o mapper grava o valor cru). | FATO (código) / INFERÊNCIA (efeito) |
| 6 | Usuário sem roles pode ser criado/atualizado e não autentica. | FATO (código) / INFERÊNCIA (efeito) |
| 7 | `PUT /users/{id}` com `roleIds=[]` remove todas as roles. | FATO |
| 8 | Vários tokens de recuperação podem coexistir; não há checagem de `active` na solicitação. | FATO |
| 9 | `resetPassword`/`updatePassword`/`deactivate` não revogam tokens OAuth2 (in-memory, refresh de 30 dias): tokens emitidos podem continuar úteis até expirar; comportamento do refresh está na camada `security`. | INFERÊNCIA / HIPÓTESE (refresh) |
| 10 | `AccountService.register` envia e-mail antes do commit se `@Async` não for efetivo. | INFERÊNCIA |
| 11 | Duplicidade de ids na lista `categoryIds` ⇒ `notFound` indevido (comparação por tamanho). | INFERÊNCIA |
| 12 | `syncCategories` faz `clear()` antes de validar (protegido pelo rollback). | FATO / INFERÊNCIA |
| 13 | `ProductService.delete`: `catch` de `DataIntegrityViolationException` pode não interceptar (flush no commit). | HIPÓTESE |
| 14 | `CategoryService.delete` sem checagem de produtos vinculados (409 via handler no commit). | INFERÊNCIA |
| 15 | `ProductService.create` ignora `date` do request. | FATO |
| 16 | `@Transactional(readOnly=true)` em método privado (`UserService.findEntityById`) sem efeito. | INFERÊNCIA |
| 17 | `backendUrl` não utilizado; import `Valid` não utilizado; `Token.isValid()` e `disableAllPasswordRecoveryTokens` sem chamadores. | FATO |
| 18 | Layering: `AccountService` depende de `security.auth`; `Token` (domínio) depende de `service.exception`. | FATO |
| 19 | N+1 provável em `ProductService.search`/`UserService.search`/`findById` (coleções tardias na conversão). | INFERÊNCIA |
| 20 | Registro de e-mail com `content` rótulo e `status` sempre `PENDING`. | FATO |
| 21 | JavaDoc preexistente usava tags `@apiNote`/`@implNote` (rejeitadas pelo `javadoc` do JDK sem `-tag`); removidas dos services nesta fase (restam em outras camadas). | FATO |

## 17. Fatos consolidados
Ver §2–§16 (rotulados em linha). Principais: 6 services; 32 `@Transactional` + 2 `@Async`; `search` de produtos sem chamadores; `findAllPaged` chama a segunda consulta sem guardar lista vazia; `deactivateAccount` não implementado; `loadUserByUsername` só é consumido pelo provider; identidade "me" vem do claim `userId`; nenhuma autorização nos services; `TokenService` usa só `Token.validate`.

## 18. Inferências consolidadas
`@Async` sem efeito ⇒ envio síncrono dentro da transação; N+1 nas conversões de listagens/detalhes; `@Transactional` privado sem efeito; efeito da falta de normalização de e-mail no login; usuário sem roles não autentica em execução; ids repetidos em `categoryIds`; exclusão de categoria em uso ⇒ 409 no commit; participação do `save` de `EmailRepository` na transação do chamador.

## 19. Hipóteses (confirmar depois)
1. `searchProductsWithCategories` com lista vazia (página sem ids) e `categoryIds` vazio em PostgreSQL (herdadas da B-2; o service confirma que as chamadas ocorrem, mas não o efeito).
2. `catch (DataIntegrityViolationException)` em `ProductService.delete` pode não ser alcançado.
3. Refresh tokens continuam válidos após desativar/trocar senha. *(B-6: não há nenhuma revogação no código; o refresh padrão não relê o usuário.)*
4. SpEL `authentication.principal.id` em `UserController.update` (B-0 P1-7). *(B-6: confirmado por leitura e bytecode que `id` resolve para `jti`; o efeito HTTP segue hipótese.)*
5. Comportamento em execução do `@Async` sem `@EnableAsync`.

## 20. Questões que dependem das próximas camadas
- **Mappers/DTOs (candidata a B-4):** ponto em que as coleções tardias são percorridas (N+1), a ausência de normalização de e-mail (`UserMapper.toEntity`), o `date` não copiado, atualização parcial por `null`.
- **Validators:** unicidade/existência (usam repositories), `@ValidRoles` aceitando vazio, DNS em `@ValidEmail`, dependência do `HttpServletRequest`.
- **Security:** `CustomPasswordAuthenticationProvider` (uso do `User` parcial, `username` digitado), refresh token, `AuthenticatedUserService`, SpEL do `UserController`.
- **Controllers:** `@PreAuthorize`, endpoints públicos, `deactivate` publicado.
- **Testes (fase futura):** os testes de service mockam repositórios; casos de lista vazia e `@Async` não cobertos.

## 21. JavaDoc adicionado ou ajustado

| Arquivo | Alterações |
|---------|-----------|
| `ProductService` | Classe reescrita; `search`, `findAllPaged` (sequência, comportamento, ressalvas), `findById`, `create`, `update`, `activate`, `deactivate`, `delete`, `syncCategories`, `changeStatus` reescritos (removidos `@apiNote/@implNote` e afirmações não implementadas). |
| `UserService` | Classe reescrita (dois papéis); todos os métodos públicos; `deactivate` (era `/* */`, agora JavaDoc); `findEntityById` (doc era cópia do público); `findRolesByIdsOrThrow`; **`loadUserByUsername`** (User parcial e uso); novos JavaDocs de `updatePasswordIfPresent`/`updateRolesIfPresent`. |
| `AccountService` | Classe reescrita; `register`, `confirmEmail`, `requestPasswordRecovery` (correção do `@throws`), `resendActivationEmail`; **novos** JavaDocs de `resetPassword`, `updateAuthenticatedUser`, `updatePassword`, `getAuthenticatedUser`, `deactivateAccount` (não implementado) e dos 3 validadores privados. |
| `CategoryService` | Classe reescrita; `search`, `findById`, `create`, `update`, `activate`, `deactivate` (era `/* */`), `delete`, `changeStatus`. |
| `TokenService` | **Novo** JavaDoc de classe e dos 5 métodos públicos (diferença `isValid`/`validate`). |
| `EmailService` | **Novo** JavaDoc de classe (dependências, `@Async`, transação, registro) e do campo `backendUrl`; reescritos os 2 `...Async`, os 2 síncronos e `registerEmailLog`. |

Verificação: o doclint (`-Xdoclint:all,-missing`) não reportou avisos para o pacote `service`; sem `@apiNote/@implNote` restantes; os únicos membros sem comentário são campos, loggers e o construtor de `TokenService`.

## 22. O que **não** foi feito
Nenhuma alteração de código executável, anotações (`@Transactional`, `@Async`, etc.), assinaturas, ordem de chamadas, tratamento de exceções, queries ou mappings; nenhum arquivo em `src/test/**`, repositories, projections, domain, DTOs, mappers, validators, controllers, security, config, migrations, properties ou `pom.xml`; nenhum teste executado; nenhum commit.
