# ASJCatalog Backend — B-5: Camada de Validação

> **Fase:** B-5 — análise e documentação (JavaDoc) de `validation`.
> **Pré-requisitos:** [B-0](B-0-BACKEND-INVENTORY.md), [B-1](B-1-DOMAIN-LAYER.md), [B-2](B-2-REPOSITORY-LAYER.md), [B-3](B-3-SERVICE-LAYER.md), [B-4](B-4-DTO-MAPPER-LAYER.md).
> **Escopo alterado:** somente comentários JavaDoc em `src/main/java/**/validation/**` (23 arquivos) e esta documentação, com notas pontuais nos documentos anteriores. Nenhuma lógica, regra, regex, mensagem, annotation, query ou comportamento de DNS foi alterado.

Rótulos: **[FATO]** confirmado no código; **[INFERÊNCIA]** conclusão razoável, sem execução dedicada; **[HIPÓTESE]** precisa de confirmação; **[A confirmar em testes]** só a execução resolve. Nenhum teste foi executado nesta fase.

---

## 1. Objetivo
Documentar o que a camada de validação **realmente impõe** (por leitura do código), fechando a lacuna entre o que os DTOs declaram (B-4) e o que os validators fazem, e registrar as divergências entre valor validado, valor persistido e restrições do banco — sem corrigir nada.

## 2. Escopo
`validation/{category,product,role,user}/{annotation,validator,contract}` — 23 arquivos. Leituras auxiliares (sem alteração): DTOs, repositories, `AuthenticatedUserService`, arquivos `messages_*.properties`, `ValidEmailValidatorTest` (somente leitura). `web`, `security`, `config` e `src/test/**` não foram alterados.

## 3. Inventário [FATO]

| # | Arquivo | Tipo | Nível | Consulta banco | Depende de HTTP | Depende de autenticação | Rede | Usado por |
|---|---------|------|-------|:--:|:--:|:--:|:--:|-----------|
| 1 | `CategoryCreateValid` | annotation | classe | — | — | — | — | `CategoryCreateRequest` |
| 2 | `CategoryCreateValidator` | `ConstraintValidator<…, CategoryCreateRequest>` | classe | sim | não | não | não | (1) |
| 3 | `CategoryUpdateValid` | annotation | classe | — | — | — | — | `CategoryUpdateRequest` |
| 4 | `CategoryUpdateValidator` | validator | classe | sim | **sim** | não | não | (3) |
| 5 | `ProductCreateValid` | annotation | classe | — | — | — | — | `ProductCreateRequest` |
| 6 | `ProductCreateValidator` | validator | classe | sim (2 repos) | não | não | não | (5) |
| 7 | `ProductUpdateValid` | annotation | classe | — | — | — | — | `ProductUpdateRequest` |
| 8 | `ProductUpdateValidator` | validator | classe | sim (2 repos) | **sim** | não | não | (7) |
| 9 | `ValidRoles` | annotation | campo | — | — | — | — | `UserCreateRequest.roleIds`, `UserUpdateRequest.roleIds` |
| 10 | `ValidRolesValidator` | `ConstraintValidator<…, Set<Long>>` | campo | sim | não | não | não | (9) |
| 11 | `PasswordPersonalData` | annotation | classe | — | — | — | — | `UserCreateRequest`, `UserRegisterRequest` |
| 12 | `PasswordPersonalDataValidator` | `ConstraintValidator<…, PasswordPersonalDataCandidate>` | classe | **não** | não | não | não | (11) |
| 13 | `StrongPassword` | annotation | campo | — | — | — | — | 5 DTOs (ver §5) |
| 14 | `StrongPasswordValidator` | `ConstraintValidator<…, String>` | campo | **não** | não | não | não | (13) |
| 15 | `UniqueEmail` | annotation | campo | — | — | — | — | `UserCreateRequest`, `UserRegisterRequest` |
| 16 | `UniqueEmailValidator` | validator | campo | sim | não | não | não | (15) |
| 17 | `UniqueEmailForAuthenticatedUser` | annotation | campo (+ `ANNOTATION_TYPE`) | — | — | — | — | `AuthenticatedUserUpdateRequest` |
| 18 | `UniqueEmailForAuthenticatedUserValidator` | validator | campo | sim | não | **sim** | não | (17) |
| 19 | `UserUpdateValid` | annotation | classe | — | — | — | — | `UserUpdateRequest` |
| 20 | `UserUpdateValidator` | validator | classe | sim | **sim** | não | não | (19) |
| 21 | `ValidEmail` | annotation | campo | — | — | — | — | 4 DTOs (ver §5) |
| 22 | `ValidEmailValidator` | validator | campo | não | não | não | **sim (DNS)** | (21) |
| 23 | `PasswordPersonalDataCandidate` | interface (contrato) | — | — | — | — | — | implementada por `UserCreateRequest`, `UserRegisterRequest`; usada por (12) |

**Contagens:** 11 annotations, 11 validators, 1 contrato. Todas as annotations têm exatamente um validator e são usadas em ao menos um DTO; **não há annotation sem uso nem validator sem annotation** [FATO]. **Não existe** `UserCreateValid` (o JavaDoc antigo dos DTOs referenciava uma annotation inexistente — corrigido na B-4).

## 4. Arquitetura da validação [FATO]
- Todas as annotations têm `@Documented`, `@Constraint(validatedBy = X.class)` e `@Retention(RUNTIME)`, além de `message()`, `groups()`, `payload()`. Nenhuma declara atributos próprios; **nenhuma usa grupos ou payload**.
- **Nível de classe** (`@Target(TYPE)`): `CategoryCreateValid`, `CategoryUpdateValid`, `ProductCreateValid`, `ProductUpdateValid`, `PasswordPersonalData`, `UserUpdateValid`. **Nível de campo** (`@Target(FIELD)`): `ValidRoles`, `StrongPassword`, `UniqueEmail`, `ValidEmail`; e `UniqueEmailForAuthenticatedUser` (`FIELD` + `ANNOTATION_TYPE`).
- Os validators recebem dependências por **construtor** (`UserRepository`, `CategoryRepository`, `ProductRepository`, `RoleRepository`, `AuthenticatedUserService`, `HttpServletRequest`); só `ValidEmailValidator`, `StrongPasswordValidator` e `PasswordPersonalDataValidator` não têm dependências.
- **Violações personalizadas:** todos os validators que falham chamam `disableDefaultConstraintViolation()` e registram chaves de mensagem `{...}` específicas (resolvidas pelo `MessageSource`). Os validators de classe usam `addPropertyNode(campo)` para associar a violação a `name`, `categoryIds`, `email` ou `password`; os de campo registram na própria propriedade.
- **Mensagens padrão nunca emitidas:** `{error.validation.message}` (4 annotations de create/update de Category/Product), `{user.password.strong}` e `{user.update.validation}`. As duas últimas **não existem** nos arquivos de mensagens (pt_BR/en/es) [FATO]; como o validator sempre desabilita a violação padrão, isso não tem efeito hoje.
- **Estrutura de acúmulo:** os validators de classe (e `PasswordPersonalDataValidator`) usam `web.exception.response.FieldMessage` (record do pacote web) para juntar erros antes de registrá-los [FATO].

## 5. Annotations [FATO]

| Annotation | Alvo | Validator | Mensagem padrão (existe no bundle?) | Onde é usada | Composição |
|------------|------|-----------|-------------------------------------|--------------|------------|
| `@CategoryCreateValid` | classe | `CategoryCreateValidator` | `{error.validation.message}` (sim, não emitida) | `CategoryCreateRequest` | classe com 1 atributo validado |
| `@CategoryUpdateValid` | classe | `CategoryUpdateValidator` | idem | `CategoryUpdateRequest` | idem |
| `@ProductCreateValid` | classe | `ProductCreateValidator` | idem | `ProductCreateRequest` | composta: nome + categorias |
| `@ProductUpdateValid` | classe | `ProductUpdateValidator` | idem | `ProductUpdateRequest` | composta |
| `@ValidRoles` | campo | `ValidRolesValidator` | `{role.invalid}` (sim, emitida) | `UserCreateRequest`, `UserUpdateRequest` | simples |
| `@PasswordPersonalData` | classe | `PasswordPersonalDataValidator` | `{user.password.personalData}` (sim) | `UserCreateRequest`, `UserRegisterRequest` | simples (usa contrato) |
| `@StrongPassword` | campo | `StrongPasswordValidator` | `{user.password.strong}` (**não existe**; nunca emitida) | `UserCreateRequest`, `UserRegisterRequest`, `UserUpdateRequest`, `PasswordUpdateRequest.newPassword`, `PasswordResetRequest.password` | simples (7 regras) |
| `@UniqueEmail` | campo | `UniqueEmailValidator` | `{user.email.unique}` (sim) | `UserCreateRequest`, `UserRegisterRequest` | simples |
| `@UniqueEmailForAuthenticatedUser` | campo/meta | `UniqueEmailForAuthenticatedUserValidator` | `{user.email.unique}` (sim) | `AuthenticatedUserUpdateRequest` | simples |
| `@UserUpdateValid` | classe | `UserUpdateValidator` | `{user.update.validation}` (**não existe**; nunca emitida) | `UserUpdateRequest` | composta: e-mail + senha |
| `@ValidEmail` | campo | `ValidEmailValidator` | `{user.email.invalid}` (sim) | `UserCreateRequest`, `UserRegisterRequest`, `UserUpdateRequest`, `UserEmailRequest` | simples |

As annotations de campo funcionam nos *records* porque o Java propaga a anotação ao campo do componente [INFERÊNCIA — confirmado indiretamente pelos ITs verdes do B-0 que exercem validação].

## 6. Validators — comportamento real [FATO]

| Validator | Tipo validado | Regras efetivas | Violação (chave → campo) |
|-----------|---------------|-----------------|--------------------------|
| `CategoryCreateValidator` | `CategoryCreateRequest` | nome (`trim`+minúsculas) não existe (`existsByNameIgnoreCase`); ignora nome nulo/em branco | `category.name.unique` → `name` |
| `CategoryUpdateValidator` | `CategoryUpdateRequest` | nome não pertence a **outra** categoria (`existsByNameIgnoreCaseAndIdNot`, id da URL); ignora se nome nulo/branco ou sem `id` na URI | `category.name.unique` → `name` |
| `ProductCreateValidator` | `ProductCreateRequest` | nome único (`existsByNameIgnoreCase`); cada `categoryId` existe (`existsById`) | `product.name.unique` → `name`; `product.categoryIds.invalid` → `categoryIds` |
| `ProductUpdateValidator` | `ProductUpdateRequest` | nome de outro produto (id da URL); categorias existem | idem |
| `ValidRolesValidator` | `Set<Long>` | todos os ids existem (`existsById` por id); nulo/vazio válidos | `role.invalid` → campo anotado |
| `PasswordPersonalDataValidator` | `PasswordPersonalDataCandidate` | senha (`trim`+minúsculas) não **contém** nome, sobrenome nem prefixo do e-mail (tokens ≥ 3); máx. 1 violação | `user.password.personalData` → `password` |
| `StrongPasswordValidator` | `String` | 7 regras (ver §9) | 7 chaves `user.password.*` → campo |
| `UniqueEmailValidator` | `String` | e-mail (`trim`+minúsculas) não existe (`existsByEmailIgnoreCase`) | `user.email.unique` → campo |
| `UniqueEmailForAuthenticatedUserValidator` | `String` | e-mail não pertence a **outro** usuário; id do usuário do JWT | `user.email.unique` → campo |
| `UserUpdateValidator` | `UserUpdateRequest` | e-mail de outro usuário (id da URL, NFE tratada); senha sem dados pessoais | `user.email.unique` → `email`; `user.password.personalData` → `password` |
| `ValidEmailValidator` | `String` | regex ASCII + MX por DNS | `user.email.invalid` → campo |

## 7. Validação de e-mail

### 7.1 `ValidEmailValidator` [FATO]
1. `null` ou `isBlank()` ⇒ **válido** (inclusive `"   "`).
2. Normaliza **só para validar**: `value.trim().toLowerCase()` (`toLowerCase()` sem `Locale`).
3. `email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")` — ASCII apenas; parte local: letras, dígitos e `+ _ . -` sem limite e **sem restrições de pontos** (`..`, `.` inicial/final passam); domínio: letras, dígitos, `.`, `-`; sufixo ≥ 2 letras. Sem IDN/acentos.
4. Domínio = texto após o `@` → `hasMxRecord`: `InitialDirContext` com `com.sun.jndi.dns.DnsContextFactory`, `getAttributes(domain, {"MX"})`, verdadeiro se houver ao menos um MX. **Qualquer `Exception` ⇒ `false`**; o contexto é fechado em `finally`.
5. Falha em (3) ou (4) ⇒ `{user.email.invalid}` (mesma mensagem para formato, ausência de MX e erro de DNS).

### 7.2 Rede e falhas [FATO / INFERÊNCIA / HIPÓTESE]
- **[FATO]** Consulta DNS **síncrona**, na thread da requisição, uma por e-mail que passe no formato; sem provedor/servidor, timeout nem tentativas configurados no código; só MX é considerado (sem fallback A/AAAA).
- **[FATO]** DNS indisponível, domínio inexistente ou timeout produzem o mesmo resultado que "sem MX": e-mail inválido. Logo **uma falha de rede rejeita e-mails válidos**.
- **[INFERÊNCIA]** Sem propriedades JNDI de timeout, valem os padrões do provedor DNS do JDK (documentados no JDK: tempo inicial de ~1 s com novas tentativas), de modo que uma indisponibilidade pode atrasar a resposta em vários segundos.
- **[FATO]** `ValidEmailValidatorTest` valida `user@gmail.com` **sem simular o DNS** (só mocka o `ConstraintValidatorContext`): esse teste depende de rede. **[A confirmar em testes]** o comportamento sem rede.
- **[HIPÓTESE]** Domínios que aceitam e-mail apenas por registro A/AAAA seriam rejeitados.

### 7.3 Valor validado × valor persistido [FATO / INFERÊNCIA]
- **[FATO]** O DTO **não é modificado** pelo validator. O mapper (`UserMapper.toEntity`/`updateEntity`) e `AccountService.register` gravam o e-mail como recebido (B-4 §16).
- **[FATO]** Consequência direta: um e-mail como `"  Maria@Gmail.com "` **passa** em `@ValidEmail` (validado como `maria@gmail.com`) e é persistido **com espaços e caixa original**.
- **[INFERÊNCIA]** Login (`searchUserAndRolesByEmail`), recuperação e reenvio (`findByEmail`) comparam o texto exato; o usuário precisa repetir a mesma grafia. O único fluxo que grava normalizado é o de perfil (`AccountService.updateAuthenticatedUser`).
- Confirma e amplia o achado de B-4: **`@ValidEmail` valida uma versão normalizada, mas o valor cru é persistido**.

## 8. Unicidade

### 8.1 Fluxo completo (DTO → annotation → validator → repository → resultado) [FATO]

| Caso | DTO/campo | Annotation | Validator | Repository (método) | Resultado |
|------|-----------|------------|-----------|---------------------|-----------|
| Criar categoria | `CategoryCreateRequest` | `@CategoryCreateValid` | `CategoryCreateValidator` | `CategoryRepository.existsByNameIgnoreCase` | violação em `name` |
| Atualizar categoria | `CategoryUpdateRequest` | `@CategoryUpdateValid` | `CategoryUpdateValidator` | `existsByNameIgnoreCaseAndIdNot(nome, idURL)` | violação em `name` |
| Criar produto | `ProductCreateRequest` | `@ProductCreateValid` | `ProductCreateValidator` | `ProductRepository.existsByNameIgnoreCase` | violação em `name` |
| Atualizar produto | `ProductUpdateRequest` | `@ProductUpdateValid` | `ProductUpdateValidator` | `existsByNameIgnoreCaseAndIdNot(nome, idURL)` | violação em `name` |
| Criar/registrar usuário | `email` de `UserCreateRequest`/`UserRegisterRequest` | `@UniqueEmail` | `UniqueEmailValidator` | `UserRepository.existsByEmailIgnoreCase` | violação no campo |
| Atualizar usuário (admin/self via id) | `UserUpdateRequest` | `@UserUpdateValid` | `UserUpdateValidator` | `existsByEmailIgnoreCaseAndIdNot(email, idURL)` | violação em `email` |
| Atualizar próprio perfil | `email` de `AuthenticatedUserUpdateRequest` | `@UniqueEmailForAuthenticatedUser` | `UniqueEmailForAuthenticatedUserValidator` | `existsByEmailIgnoreCaseAndIdNot(email, idDoJWT)` | violação no campo |

Diferença create × update: em criação não há id de exclusão; em atualização o id vem **da URL** (Category/Product/User) ou **do JWT** (perfil). O próprio registro é excluído da comparação pelo `AndIdNot`.

### 8.2 Divergências entre valor validado, persistido, query e banco [FATO / INFERÊNCIA]

| Regra | Valor validado | Valor persistido | Comparação da query | `UNIQUE` do banco |
|-------|----------------|------------------|---------------------|-------------------|
| Nome de categoria/produto | `trim`+minúsculas | **cru** (mapper) | ignora caixa; `trim` só no parâmetro | exata (sensível à caixa e a espaços) |
| E-mail (criar/registrar/atualizar) | `trim`+minúsculas | **cru** | ignora caixa; `trim` só no parâmetro | exata |
| E-mail (perfil) | `trim`+minúsculas | **normalizado** (`trim`+minúsculas) | idem | exata |

- **[FATO]** Um valor persistido com espaços nas pontas **não é encontrado** por uma checagem posterior do valor aparado (`upper(email)=upper('a@x.com')` ≠ `'a@x.com '`); o `UNIQUE` do banco, sensível a espaços e à caixa, aceitaria ambos.
- **[INFERÊNCIA]** As checagens são "consultar e depois inserir": não são atômicas; a garantia final é a restrição `UNIQUE`, e uma condição de corrida terminaria em `DataIntegrityViolationException` (409 pelo handler global).
- **[FATO]** Com **id inexistente** na URL (Category/Product/User), o `AndIdNot` compara com "nenhum registro excluído": se qualquer registro tiver o nome/e-mail, a violação é de unicidade (e não 404); se ninguém tiver, a validação passa e o service responde 404.
- **[FATO]** Com valor `null`/em branco: as checagens de unicidade são **ignoradas** (retornam válido); a obrigatoriedade é de `@NotBlank`.

## 9. Senhas

### 9.1 `StrongPasswordValidator` [FATO]
- `null` ⇒ válido. `""` ⇒ **inválido** (violações: maiúscula, minúscula, número, especial). `"   "` ⇒ **inválido** (whitespace + as quatro anteriores).
- Todas as regras são avaliadas; **cada regra violada gera uma violação** (até 7):
  1. `whitespace`: qualquer caractere com `Character.isWhitespace`.
  2. `uppercase`: nenhuma letra `A-Z`.
  3. `lowercase`: nenhuma letra `a-z`.
  4. `number`: nenhum dígito `0-9` (`\d` ASCII).
  5. `specialCharacter`: nenhum caractere fora de `A-Za-z0-9` e espaço — **letras acentuadas contam como "especial"** (`é`, `ç`…).
  6. `common`: a senha, após `trim`+minúsculas, é **exatamente** uma de: `123456`, `1234567`, `12345678`, `password`, `admin`, `qwerty`, `abc123`, `111111`, `123123`.
  7. `sequence`: os **dígitos da senha concatenados** contêm ≥ 6 consecutivos crescentes ou decrescentes (passo ±1). Dígitos separados por outros caracteres também formam sequência (`a1b2c3d4e5f6`).
- **Não valida tamanho** (só `@Size` nos DTOs, quando existe: ausente em `UserUpdateRequest.password` e `PasswordResetRequest.password`).
- **[INFERÊNCIA]** A regra 6 é redundante com as demais: todas as entradas da lista são só dígitos ou só letras minúsculas, portanto já violam maiúscula e/ou especial.
- **[INFERÊNCIA]** O JavaDoc antigo de `@StrongPassword` afirmava "mínimo de 10 caracteres" — o validator não impõe isso (corrigido). O JavaDoc antigo de `validateNumericSequences` descrevia "dígitos consecutivos" — na verdade são os dígitos concatenados (corrigido).

### 9.2 `PasswordPersonalDataValidator` e `PasswordPersonalDataCandidate` [FATO]
- Contrato: `firstName()`, `lastName()`, `email()`, `password()` — implementado por `UserCreateRequest` e `UserRegisterRequest` (os quatro accessors agora têm JavaDoc).
- Candidato/senha `null` ⇒ válido. Senha `""` ou em branco ⇒ nada a comparar ⇒ válido.
- Senha normalizada `trim`+minúsculas; compara por **substring** com: primeiro nome, sobrenome e o trecho do e-mail **antes do primeiro `@`** (`split("@")[0]`, só se o e-mail contiver `@`); cada um normalizado (`trim`+minúsculas); **ignorado se `null` ou com < 3 caracteres**.
- No máximo **uma** violação (`user.password.personalData` → `password`). Nomes compostos com espaço são comparados inteiros e, como `@StrongPassword` proíbe espaços, tendem a nunca casar [INFERÊNCIA].
- Não consulta banco. Não se aplica a `UserUpdateRequest`/`PasswordUpdateRequest`/`PasswordResetRequest`; a atualização de usuário tem **cópia** da regra em `UserUpdateValidator`; `PasswordUpdateRequest` e `PasswordResetRequest` **não têm** verificação de dados pessoais [FATO].

## 10. Roles — `ValidRolesValidator` [FATO]
- `Set<Long>`: `null` ⇒ válido; vazio ⇒ válido.
- Caso contrário `allMatch(repository::existsById)`: **uma consulta por id**, interrompendo no primeiro inexistente; qualquer id inexistente invalida o conjunto (`{role.invalid}`, sem nó de propriedade).
- Duplicados: o tipo `Set` os colapsa antes (a desserialização). Ordem irrelevante. Não há comparação de quantidade (essa está no service).
- Elemento `null` dentro do conjunto (`[null]`): `existsById(null)` lançaria exceção do Spring Data [HIPÓTESE — depende da desserialização; **A confirmar em testes**].
- **Relação com `UserService`:** o validator aceita `null`/vazio; o service trata `roleIds == null` como "sem roles" (criação) ou "manter roles" (atualização) e `isEmpty()` como "sem roles" (criação) ou "remover todas" (atualização); o service repete a verificação por **contagem** (`error.role.ids.notFound`). Logo, o pipeline **aceita** criar/atualizar usuário sem roles (B-3 §5.2).

## 11. Create / Update (validators de classe) [FATO]

| Aspecto | Category Create | Category Update | Product Create | Product Update | User Update |
|---------|-----------------|-----------------|----------------|----------------|-------------|
| Campos validados | `name` | `name` | `name`, `categoryIds` | `name`, `categoryIds` | `email`, `password` |
| Depende do id da URL | não | **sim** | não | **sim** | **sim** |
| `NumberFormatException` do id | — | **não tratada** | — | **não tratada** | **tratada** (verificação ignorada) |
| Sem `id` na URI | — | verificação ignorada | — | verificação ignorada | verificação ignorada |
| Nulo/branco no campo | ignora | ignora | ignora (`name`); ignora `categoryIds` nulo/vazio | idem | ignora `email`; ignora `password` nulo/branco |
| Acúmulo de violações | 1 | 1 | até 2 | até 2 | até 2 |
| Duplica regra dos campos? | não (unicidade não existe em annotation de campo) | não | não | não | **sim**: cópia de `PasswordPersonalData` |
| Id da URL verificado como existente | — | não | — | não | não |

- **[FATO]** `@NotEmpty` de `categoryIds` (DTO) e `@ValidRoles` (roles) tratam nulo/vazio de formas diferentes (obrigatório × opcional).
- **[FATO]** `ProductCreateValidator`/`ProductUpdateValidator`: ids repetidos em `categoryIds` **passam** (cada um existe); o service depois compara por contagem (B-3 §4.6).
- **[FATO]** Elemento `null` em `categoryIds`: `existsById(null)` lançaria exceção — **[A confirmar em testes]**.
- **[FATO]** JavaDocs antigos corrigidos: "espaços extras" → `trim()` (apenas extremidades); "senha nula ou vazia" (em `UserUpdateValidator`) → nula **ou em branco**.
- Os DTOs (B-4) estão fiéis ao comportamento dos validators; nenhum JavaDoc de DTO precisou de correção nesta fase.

## 12. HTTP request / URL [FATO / INFERÊNCIA / HIPÓTESE]
- Três validators (`CategoryUpdateValidator`, `ProductUpdateValidator`, `UserUpdateValidator`) injetam `HttpServletRequest` e leem o atributo `HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE` (mapa de variáveis do template da rota), chave literal **`"id"`**.
- **[FATO]** Se o atributo for `null` ou não tiver `"id"`: verificação de unicidade **ignorada** (a validação retorna "sem erro").
- **[FATO]** Id não numérico: `Long.parseLong` sem tratamento em Category/Product (lança `NumberFormatException`); em User é capturada e a verificação é ignorada.
- **[FATO]** O acoplamento é ao nome da variável de caminho: as rotas atuais (`/{id}`) a fornecem; qualquer rota de atualização com outro nome quebraria a verificação silenciosamente (retorna válido).
- **[INFERÊNCIA]** Como os controllers declaram `@PathVariable Long id` **antes** de `@Valid @RequestBody`, uma URL com id não numérico deve falhar na conversão do argumento antes de chegar ao validator; portanto a `NumberFormatException` de Category/Product seria, na prática, inalcançável via HTTP. **[A confirmar em testes / na camada web (B-7)]**.
- **[HIPÓTESE]** O `HttpServletRequest` injetado no validator é o proxy da requisição corrente do Spring; validação fora de uma requisição HTTP (ex.: reuso programático do validator) faria `getAttribute` falhar ou retornar `null`.
- **Autenticação:** somente `UniqueEmailForAuthenticatedUserValidator` depende dela (usa `AuthenticatedUserService`, claim `userId` do JWT). Se não houver JWT/claim/usuário, é lançada `AuthenticatedUserNotFoundException`, que o validator não captura; como essa exceção atravessa o Bean Validation e chega ao cliente **depende do handler (B-7)** [A confirmar]. A rota `PUT /accounts/me` exige autenticação por regra de URL (B-0), então o caso é improvável.

## 13. Null / vazio / branco (matriz) [FATO, exceto onde indicado]

| Validator | `null` | `""` | `"   "` | valor normalizado | valor inválido |
|-----------|:------:|:----:|:-------:|-------------------|----------------|
| `ValidEmailValidator` | válido | válido | válido | `trim`+minúsculas antes de regex/MX | falha de regex/MX/DNS ⇒ inválido |
| `UniqueEmailValidator` | válido | válido | válido | `trim`+minúsculas | e-mail já existe |
| `UniqueEmailForAuthenticatedUserValidator` | válido | válido | válido | `trim`+minúsculas | e-mail de outro usuário |
| `StrongPasswordValidator` | válido | **inválido** (4 violações) | **inválido** (5 violações) | `trim`+minúsculas só p/ lista de comuns | qualquer regra violada |
| `PasswordPersonalDataValidator` | válido (candidato ou senha) | válido | válido | senha/tokens: `trim`+minúsculas | senha contém token ≥ 3 |
| `UserUpdateValidator` | (email/senha nulos ignorados) | ignorado | ignorado | e-mail: `trim`+minúsculas; senha idem | e-mail de outro usuário; senha com dado pessoal |
| `CategoryCreate/UpdateValidator` | nome ignorado | ignorado | ignorado | `trim`+minúsculas | nome já usado |
| `ProductCreate/UpdateValidator` | nome/`categoryIds` ignorados | nome ignorado; lista vazia ignorada | nome ignorado | `trim`+minúsculas (nome) | nome usado; id de categoria inexistente |
| `ValidRolesValidator` | válido | (conjunto vazio) válido | — | — | id inexistente; `[null]` **[A confirmar em testes]** |

Observação: `StrongPasswordValidator` rejeita `""`, mas `UserUpdateValidator` trata senha em branco como "não informada"; portanto, em `UserUpdateRequest`, `password=""` é **inválido** (não equivale a "manter a senha") [FATO].

## 14. Dependências e acoplamento [FATO]

| Dependência | Onde |
|-------------|------|
| `CategoryRepository` | `CategoryCreate/UpdateValidator`, `ProductCreate/UpdateValidator` |
| `ProductRepository` | `ProductCreate/UpdateValidator` |
| `UserRepository` | `UniqueEmail*`, `UserUpdateValidator` |
| `RoleRepository` | `ValidRolesValidator` |
| `AuthenticatedUserService` (**pacote `security.auth`**) | `UniqueEmailForAuthenticatedUserValidator` |
| `HttpServletRequest`, `HandlerMapping` (Spring MVC) | os três validators de atualização |
| `web.exception.response.FieldMessage` (**pacote web**) | 6 validators (Category×2, Product×2, `PasswordPersonalData`, `UserUpdate`) |
| JNDI/DNS (`javax.naming`) | `ValidEmailValidator` |
| Jakarta Validation | todos |

- **Acoplamento com persistence:** 8 validators consultam repositories diretamente (sem passar por service).
- **Acoplamento com web:** `FieldMessage` (estrutura) e `HandlerMapping`/`HttpServletRequest` (contexto MVC).
- **Acoplamento com security:** 1 validator.
- **Camadas:** `dto → validation → (repository, security.auth, web.exception.response)` e `web.controller → dto`; portanto há dependência de `validation` para o pacote `web` (`FieldMessage`), embora **sem ciclo de pacotes** entre as mesmas duas camadas identificado [FATO por leitura de imports]. Não há dependências circulares de classes dentro de `validation`.
- Nenhuma refatoração é proposta.

## 15. Fluxos e momento da validação

Pelo código (sem ainda ler `web`, tarefa da B-7):

1. **Bean Validation** (annotations nos DTOs; `@Valid @RequestBody` nos 12 endpoints com corpo): sintaxe (`@NotBlank`, `@Size`, `@Pattern`…) + validators customizados (consultas ao banco, DNS, HTTP, JWT).
2. **Service**: repete verificações essenciais por **contagem** (`error.product.categories.notFound`, `error.role.ids.notFound`) e, para tokens/senha atual, aplica regras próprias (`PasswordUpdateException`, `Token.validate`).
3. **Domínio**: `Token.validate` etc.
4. **Banco**: `UNIQUE`/`NOT NULL`/FK/`CHECK` como garantia final.

**Duplicações [FATO]:** unicidade de nome/e-mail (validator + `UNIQUE`); existência de categorias (validator `existsById` por id + service `findAllById` por contagem); existência de roles (validator + service); dados pessoais na senha (`PasswordPersonalDataValidator` + `UserUpdateValidator`).

**Antes/depois de acesso ao banco:** os validators de classe, `ValidRoles`, `UniqueEmail*` executam consultas **antes** de qualquer service; `ValidEmail` faz consulta de rede **antes**; `StrongPassword` e `PasswordPersonalData` são puros (sem I/O).

**Ordem relativa a `@PreAuthorize` [INFERÊNCIA — a confirmar em web (B-7)]:** o Spring MVC resolve e valida os argumentos do controller antes de invocar o método (proxy de segurança de método); portanto as validações — inclusive DNS e consultas ao banco — provavelmente ocorrem **antes** da decisão de autorização por `@PreAuthorize`. Nas rotas com corpo que exigem autenticação, o filtro de URL já barra anônimos antes.

## 16. Fatos
Ver seções acima. Destaques: 11 annotations/11 validators/1 contrato, todos casados e usados; nenhuma annotation com atributos próprios, grupos ou payload; validators só emitem mensagens específicas (defaults `{user.password.strong}` e `{user.update.validation}` não existem no bundle e nunca são emitidos); `@ValidEmail` faz DNS sem timeout configurado e qualquer exceção ⇒ inválido; validators normalizam só para comparar; e-mails/nomes persistem crus (exceto no perfil); `@StrongPassword` não valida tamanho, conta letras acentuadas como "especial", extrai dígitos concatenados para sequências e rejeita `""`; `PasswordPersonalData` só cobre create/register; `NumberFormatException` sem tratamento em Category/Product update; `ValidRoles` aceita `null`/vazio e faz N consultas; `UniqueEmailForAuthenticatedUserValidator` usa o id do JWT e não da URL; `ValidEmailValidatorTest` depende de DNS real.

## 17. Inferências
Redundância da regra "senha comum"; dígitos separados formam "sequência"; checagens de unicidade não atômicas; validação executada antes de `@PreAuthorize`; `NumberFormatException` de Category/Product inalcançável via HTTP; login/recuperação sensíveis à grafia do e-mail; DNS pode atrasar a requisição em indisponibilidade (padrões do JDK).

## 18. Hipóteses
`[null]` em `roleIds`/`categoryIds` (exceção do Spring Data); domínios apenas com A/AAAA rejeitados; comportamento do `HttpServletRequest` injetado fora de requisição; propagação de `AuthenticatedUserNotFoundException` até o handler; propagação de exceções de validators pelo Bean Validation (`ValidationException` → 500?).

## 19. Riscos (sem correção)
| # | Risco | Rótulo |
|---|-------|--------|
| 1 | Dependência de rede/DNS na validação de e-mail: indisponibilidade rejeita e-mails válidos; latência na thread da requisição. | FATO (código) / INFERÊNCIA (latência) |
| 2 | E-mails e nomes gravados crus, mas validados normalizados: duplicatas por caixa/espaços possíveis no banco e falha de login por grafia. | FATO / INFERÊNCIA |
| 3 | `@StrongPassword` sem tamanho; `UserUpdateRequest`/`PasswordResetRequest` sem `@Size`. | FATO |
| 4 | Sequência numérica calculada sobre dígitos concatenados (falsos positivos). | FATO |
| 5 | Letras acentuadas satisfazem "caractere especial" (senha `Abcdefgh1é` passa). | FATO |
| 6 | `PasswordUpdateRequest` e `PasswordResetRequest` sem verificação de dados pessoais. | FATO |
| 7 | Regra de dados pessoais duplicada em `UserUpdateValidator`. | FATO |
| 8 | Acoplamento à variável de caminho `id` e ao contexto MVC; validação silenciosamente ignorada se ausente. | FATO |
| 9 | `NumberFormatException` sem tratamento em `Category/ProductUpdateValidator`. | FATO (código); alcance INFERÊNCIA |
| 10 | N consultas por validação (`ValidRoles`, categorias de produto). | FATO |
| 11 | Teste unitário do e-mail usa DNS real. | FATO |

## 20. Pontos a confirmar na fase de testes
- `ValidEmailValidator` com DNS indisponível/timeout e domínios sem MX.
- Elemento `null` em `roleIds`/`categoryIds`.
- `NumberFormatException` em `PUT /categories/abc` e `/products/abc` (qual resposta o cliente recebe).
- `UniqueEmailForAuthenticatedUserValidator` sem JWT (resposta HTTP).
- Sequência de dígitos separados; acentos; `""` em `UserUpdateRequest.password`.
- Ordem validação × `@PreAuthorize`.
- Comportamento com id inexistente na URL (unicidade × 404).

## 21. Decisões adiadas
Nenhuma correção ou refatoração: normalização de e-mail/nome na gravação, política de DNS (timeout/fallback), tamanho e composição de senha, remoção da duplicação de regras, tratamento de `NumberFormatException`, desacoplamento de `FieldMessage`/`HttpServletRequest`, mensagens padrão inexistentes — ficam para depois do mapeamento completo (B-6 a B-9) e da fase de testes.

## 22. JavaDoc adicionado ou ajustado

| Grupo | Alterações |
|-------|-----------|
| 11 annotations | JavaDoc de tipo reescrito (nível, validator, regra efetiva, DB/HTTP/auth/rede, onde é usada, metadados) e documentação dos 3 membros (`message`, `groups`, `payload`), com o status real da mensagem padrão. **Correções:** `@StrongPassword` ("mínimo 10 caracteres" — não impõe); `@CategoryCreateValid` ("relações entre múltiplos atributos" — valida um); exemplos com classe no lugar de `record` removidos. |
| 11 validators | JavaDoc de tipo e de `isValid` reescritos/criados com comportamento real (null/branco, normalização, consultas, mensagens); `ValidEmailValidator` (regex, DNS sem timeout, qualquer exceção ⇒ falso), `StrongPasswordValidator` (7 regras; helpers documentados; `validateNumericSequences` corrigido), `UserUpdateValidator` ("nula ou vazia" → "nula ou em branco"), Category/Product (`trim`, NFE não tratada, id inexistente), `ValidRolesValidator` (blocos `/* */` viraram JavaDoc). Campos comentados com `//` nos 3 validators de Category/Product viraram JavaDoc. |
| `PasswordPersonalDataCandidate` | JavaDoc do contrato e dos **4 accessors** que estavam sem documentação. |

Doclint (`-Xdoclint:all,-missing`) sem avisos para `validation`; o modo completo (com `missing`) só aponta ausência de `@param` em helpers privados e comentário de campos/construtores implícitos.

## 23. O que **não** foi feito
Nenhuma alteração de lógica, regex, regras, mensagens, annotations, queries, DNS/MX, DTOs, services, controllers, security, config, entidades, repositories, `pom.xml` ou testes; nenhum teste executado; nenhum commit.
