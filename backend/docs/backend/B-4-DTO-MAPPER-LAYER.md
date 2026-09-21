# ASJCatalog Backend — B-4: DTOs e Mappers

> **Fase:** B-4 — análise e documentação (JavaDoc) de `dto` e `mapper`.
> **Pré-requisitos:** [B-0](B-0-BACKEND-INVENTORY.md), [B-1](B-1-DOMAIN-LAYER.md), [B-2](B-2-REPOSITORY-LAYER.md), [B-3](B-3-SERVICE-LAYER.md).
> **Escopo alterado:** somente comentários JavaDoc em `src/main/java/**/dto/**` (19 records) e `src/main/java/**/mapper/**` (3 mappers), mais esta documentação e notas pontuais nos documentos anteriores. Nenhum campo, tipo, anotação de validação, assinatura, nome ou regra de atualização foi alterado.

Rótulos: **[FATO]** confirmado no código; **[INFERÊNCIA]** conclusão razoável, sem execução dedicada; **[HIPÓTESE]** precisa de confirmação. Nenhum teste foi executado nesta fase.

**Distinções usadas** (não misturar): **(S)** validação sintática/estrutural do DTO (Bean Validation); **(V)** regra delegada a validator de classe/campo (camada `validation`, próxima fase); **(A)** regra de aplicação (service); **(D)** regra de domínio (entidade); **(B)** restrição de banco.

---

## 1. Objetivo da camada

Definir o contrato de dados que atravessa a API: *records* imutáveis de entrada (com Bean Validation) e de saída, e os *mappers* manuais (`@Component`, sem MapStruct) que convertem DTO ↔ entidade. Os controllers recebem os DTOs, os services os usam e chamam os mappers; **controllers nunca chamam mappers** [FATO].

## 2. Composição [FATO]

| Pacote | Conteúdo | Qtde |
|--------|----------|:----:|
| `dto.category.{request,response}` | `CategoryCreateRequest`, `CategoryUpdateRequest`, `CategoryResponse`, `CategoryDetailsResponse` | 4 |
| `dto.product.{request,response}` | `ProductCreateRequest`, `ProductUpdateRequest`, `ProductResponse`, `ProductDetailsResponse` | 4 |
| `dto.user.request` | `UserCreateRequest`, `UserRegisterRequest`, `UserUpdateRequest`, `AuthenticatedUserUpdateRequest`, `PasswordUpdateRequest`, `PasswordResetRequest`, `UserEmailRequest` | 7 |
| `dto.user.response` | `UserResponse`, `UserDetailsResponse` | 2 |
| `dto.role.response` | `RoleResponse` | 1 |
| `dto.email.request` | `EmailRegisterRequest` (interno) | 1 |
| `mapper.{category,product,user}` | `CategoryMapper`, `ProductMapper`, `UserMapper` | 3 |

Total: **19 DTOs (todos `record`) e 3 mappers**. Não há DTO de token OAuth2, de `Token` nem de resposta de e-mail.

## 3. Mapa dos DTOs

| DTO | Tipo | Endpoint / uso | Campos principais | Validação (S) / (V) | Mapper |
|-----|------|----------------|-------------------|---------------------|--------|
| `CategoryCreateRequest` | request | `POST /categories` | name, description | (S) nome 3–80 e padrão; descrição opcional 3–255 · (V) `@CategoryCreateValid` | `CategoryMapper.toEntity` |
| `CategoryUpdateRequest` | request | `PATCH /categories/{id}` | name, description | idem · (V) `@CategoryUpdateValid` | `CategoryMapper.updateEntity` |
| `CategoryResponse` | response | create/update/list; aninhado em `ProductResponse` | id, name | — | `toResponse` |
| `CategoryDetailsResponse` | response | `GET /categories/{id}`; aninhado em `ProductDetailsResponse` | id, name, description, active | — | `toDetailsResponse` (e `ProductMapper`) |
| `ProductCreateRequest` | request | `POST /products` | name, description, price, imgUrl, **date**, categoryIds | (S) ver §4 · (V) `@ProductCreateValid` | `ProductMapper.toEntity` (+ `syncCategories`) |
| `ProductUpdateRequest` | request | `PUT /products/{id}` | name, description, price, imgUrl, categoryIds | (S) ver §4 · (V) `@ProductUpdateValid` | `ProductMapper.updateEntity` (+ `syncCategories`) |
| `ProductResponse` | response | `GET /products`, create, update | id, name, description, price, imgUrl, categories[id,name] | — | `toResponse` |
| `ProductDetailsResponse` | response | `GET /products/{id}` | + createdAt, updatedAt, active; categories detalhadas | — | `toDetailsResponse` |
| `UserCreateRequest` | request | `POST /users` (admin) | firstName, lastName, email, password, roleIds | (S)+(V) ver §5 | `UserMapper.toEntity(…, roles)` |
| `UserRegisterRequest` | request | `POST /accounts/register` | firstName, lastName, email, password | (S)+(V) ver §5 | `UserMapper.toEntity(…, roles)` |
| `UserUpdateRequest` | request | `PUT /users/{id}` | firstName, lastName, email, password?, roleIds? | (S)+(V) ver §5 | `UserMapper.updateEntity` (+ service) |
| `AuthenticatedUserUpdateRequest` | request | `PUT /accounts/me` | firstName, lastName, email | (S) + `@UniqueEmailForAuthenticatedUser` | **nenhum** (service) |
| `PasswordUpdateRequest` | request | `PATCH /accounts/me/password` | currentPassword, newPassword, confirmPassword | (S) ver §6 | **nenhum** (service) |
| `PasswordResetRequest` | request | `POST /accounts/reset-password` | token, password | (S) ver §6 | **nenhum** (controller desempacota) |
| `UserEmailRequest` | request | `POST /accounts/resend-activation` e `/password-recovery` | email | (S) NotBlank + `@ValidEmail` | **nenhum** (controller desempacota) |
| `UserResponse` | response | create/list/update de usuário, register, `GET`/`PUT /accounts/me` | id, firstName, lastName, email, roles | — | `toResponse` |
| `UserDetailsResponse` | response | `GET /users/{id}` | + active | — | `toDetailsResponse` |
| `RoleResponse` | response (aninhado) | dentro dos responses de usuário | id, authority | — | `UserMapper.toRoleResponse` (privado) |
| `EmailRegisterRequest` | interno | `EmailService` → `Email(...)` | sender, recipient, content | anotações declaradas, **não acionadas** | nenhum |

Todos os 12 parâmetros `@RequestBody` dos controllers usam `@Valid` [FATO: 12 ocorrências em `web`].

## 4. DTOs de catálogo

### 4.1 Categoria [FATO]
- `name`: `@NotBlank`, `@Size(3–80)`, `@Pattern("^[A-Za-zÀ-ÿ0-9\\s]+$")` — letras (com acentos), dígitos e espaços (`\s` inclui tabulação/quebra de linha). (B) `name` `NOT NULL UNIQUE`, `varchar(80)`.
- `description`: apenas `@Pattern("^$|^.{3,255}$")` — aceita `null`, aceita `""`, ou 3–255 caracteres **sem quebras de linha** (o `.` não casa com terminadores de linha). (B) `varchar(255)`.
- Unicidade do nome (V): `@CategoryCreateValid` / `@CategoryUpdateValid` (esta com o `id` da URL).
- `active` não faz parte de nenhum request; o service cria a categoria ativa (A).

### 4.2 Produto [FATO]
- `name`: `@NotBlank`, `@Size(3–100)`, `@Pattern("^[A-Za-zÀ-ÿ0-9\\s\\-()]+$")` (letras, dígitos, espaço, hífen e parênteses). (B) `varchar(255) NOT NULL UNIQUE`.
- `description`: `@Size(3–200)` — aceita `null`; **`""` é recusado** (tamanho 0 < 3). (B) `TEXT`.
- `price`: `@Positive` — recusa 0 e negativos, mas **aceita `null`**; não há `@NotNull`. (D) `Product.price` é `Double` sem restrição; (B) coluna `float(53)` sem `NOT NULL`. **Logo, é possível criar produto sem preço** [FATO: DTO, entidade e banco permitem `null`].
- `imgUrl`: `@Pattern("^(https?://).+$")` — aceita `null`.
- `date` (**somente em `ProductCreateRequest`**): `@PastOrPresent`, aceita `null`. **Não é persistido nem usado** — ver §17.
- `categoryIds`: `@NotEmpty` (lista de `Long`; os elementos não são verificados por Bean Validation). Existência das categorias: (V) `@ProductCreateValid`/`@ProductUpdateValid` (`existsById` por id) e (A) `ProductService.syncCategories` (por quantidade).
- `ProductUpdateRequest` não tem `date`.

## 5. DTOs de usuário

> **Atualização (fase B-5):** detalhes confirmados nos validators: `@StrongPassword` aceita letras acentuadas como "especial", detecta sequências sobre os dígitos concatenados e rejeita `""`; `@UserUpdateValid` repete a regra de dados pessoais; `PasswordUpdateRequest` e `PasswordResetRequest` não têm verificação de dados pessoais. Ver [B-5-VALIDATION-LAYER.md](B-5-VALIDATION-LAYER.md).

| Campo | `UserCreateRequest` | `UserRegisterRequest` | `UserUpdateRequest` |
|-------|---------------------|-----------------------|---------------------|
| `firstName`/`lastName` | `@NotBlank`, `@Size(2–80)` | idem | idem |
| `email` | `@NotBlank`, `@ValidEmail`, `@UniqueEmail` | idem | `@NotBlank`, `@ValidEmail` (unicidade: `@UserUpdateValid`) |
| `password` | `@NotBlank`, `@Size(10–72)`, `@StrongPassword` | idem | **opcional**: só `@StrongPassword` (**sem `@Size`, sem `@NotBlank`**) |
| `roleIds` | `Set<Long>` `@ValidRoles` | — (role fixa no service) | `Set<Long>` `@ValidRoles` (opcional) |
| Classe | `@PasswordPersonalData` (`PasswordPersonalDataCandidate`) | idem | `@UserUpdateValid` |

- (V) `@ValidEmail`: formato + consulta DNS (MX) — B-0. `@StrongPassword`: sem espaços; maiúscula, minúscula, dígito e especial; não comum; sem sequência numérica ≥ 6 — B-0. **`@StrongPassword` não impõe tamanho**: em `UserUpdateRequest` o limite 10–72 não existe [FATO]; efeito no `BCryptPasswordEncoder` para senhas > 72 bytes não verificado [HIPÓTESE].
- `UserCreateRequest`/`UserRegisterRequest` implementam o contrato `PasswordPersonalDataCandidate` para a validação de classe.
- (B) `tb_user.email` `NOT NULL UNIQUE varchar(255)`; nomes `varchar(255)`.

## 6. DTOs de conta/autenticação [FATO]

| DTO | Finalidade | Campos / constraints | Consumo |
|-----|-----------|----------------------|---------|
| `AuthenticatedUserUpdateRequest` | usuário autenticado edita o próprio perfil | `firstName`/`lastName`: `@NotBlank` + `@Size(max 100)` (limite diferente dos 80 dos demais); `email`: `@NotBlank`, `@Email` (Jakarta, sem DNS), `@Size(max 255)`, `@UniqueEmailForAuthenticatedUser`. Sem senha/roles/active | `AccountService.updateAuthenticatedUser` (identidade pelo claim `userId`; sem mapper) |
| `PasswordUpdateRequest` | trocar a própria senha | `currentPassword`: `@NotBlank`; `newPassword`: `@NotBlank`, `@Size(10–72)`, `@StrongPassword`; `confirmPassword`: `@NotBlank`, `@Size(10–72)` (sem `@StrongPassword`). Sem `@PasswordPersonalData` | `AccountService.updatePassword` (igualdade nova×confirmação, senha atual e "diferente da atual" no service) |
| `PasswordResetRequest` | redefinir senha via token de recuperação | `token`: `@NotBlank`; `password`: `@NotBlank`, `@StrongPassword` (**sem `@Size`**, sem dados pessoais) | `AccountController` extrai `token()` e `password()` e chama `AccountService.resetPassword(String, String)` |
| `UserEmailRequest` | identificar conta pelo e-mail | `email`: `@NotBlank`, `@ValidEmail` | `AccountController` extrai `email()` para `resendActivationEmail` e `requestPasswordRecovery` |

O DTO `UserRegisterRequest` (registro) foi descrito em §5. Nenhum DTO transporta o JWT nem tokens OAuth2.

## 7. DTOs de token/email [FATO]
- Não há DTO de `Token`. O valor do token de ativação chega por `@RequestParam token` em `GET /accounts/activate` (sem DTO), e o de recuperação por `PasswordResetRequest.token`.
- `EmailRegisterRequest` é **interno**: criado em `EmailService.registerEmailLog` (remetente fixo, destinatário do usuário, rótulo fixo) e consumido por `Email(EmailRegisterRequest)`. Suas anotações `@NotBlank`/`@Email` **não são acionadas** por nenhum caminho (sem `@Valid`). O JavaDoc antigo citava um campo `subject` e um construtor de 4 argumentos que **não existem** (corrigido).

## 8. DTOs de request (resumo) [FATO]
7 de conta/usuário + 4 de catálogo + 1 interno = **12 requests**. Nenhum request contém `id` (o id vem da URL, ou do JWT nos fluxos "me"), `active`, datas nem `tokens`. Campos **somente de entrada** e sensíveis: `password`, `currentPassword`, `newPassword`, `confirmPassword`, `token` (reset). Campo de entrada **não persistido**: `ProductCreateRequest.date`. Campos de entrada **que não chegam ao mapper**: `categoryIds` (service), `roleIds` (service), `UserUpdateRequest.password` (service).

## 9. DTOs de response (resumo)

| Response | Expõe | Não expõe (da entidade) |
|----------|-------|-------------------------|
| `CategoryResponse` | id, name | description, active, createdAt, updatedAt, products |
| `CategoryDetailsResponse` | id, name, description, active | createdAt, updatedAt, products |
| `ProductResponse` | id, name, description, price, imgUrl, categories (id, name) | createdAt, updatedAt, active |
| `ProductDetailsResponse` | tudo de `ProductResponse` + createdAt, updatedAt, active; categories (id, name, description, active) | — (todos os campos escalares da entidade estão presentes) |
| `UserResponse` | id, firstName, lastName, email, roles (id, authority) | **password**, active, tokens |
| `UserDetailsResponse` | + active | **password**, tokens |
| `RoleResponse` | id, authority | — |

Perfil da conta (`GET`/`PUT /accounts/me`) devolve `UserResponse`. Nenhuma entidade (`Token`, `Email`) tem response próprio [FATO].

## 10. Mappers [FATO]

| Mapper | Origem | Destino | Operação | Particularidades |
|--------|--------|---------|----------|------------------|
| `CategoryMapper` | `CategoryCreateRequest` | `Category` | `toEntity` | copia name/description; **não** define `active` (padrão `false`; service põe `true`) |
| | `CategoryUpdateRequest` + `Category` | `Category` (mutada) | `updateEntity` | **só campos não nulos**; `request`/`entity` nulos ⇒ no-op |
| | `Category` | `CategoryResponse` / `CategoryDetailsResponse` | `toResponse` / `toDetailsResponse` | nulo ⇒ `null`; não acessa `products` |
| | `Page<Category>` | `Page<CategoryResponse>` | `toResponsePage` | `Page.map` |
| `ProductMapper` | `ProductCreateRequest` | `Product` | `toEntity` | copia 4 campos; **ignora `date` e `categoryIds`**; não define `active` |
| | `ProductUpdateRequest` + `Product` | `Product` (mutada) | `updateEntity` | **só campos não nulos** (name, description, price, imgUrl); não toca categorias |
| | `Product` | `ProductResponse` / `ProductDetailsResponse` | `toResponse` / `toDetailsResponse` | **percorre `getCategories()`** e converte (resumida/detalhada) |
| | `Page<Product>` | `Page<ProductResponse>` | `toResponsePage` | `Page.map` |
| `UserMapper` | `UserCreateRequest` + `Set<Role>` | `User` | `toEntity` | copia 4 campos **crus** (senha em texto, e-mail sem normalizar); adiciona roles recebidas; **não lê `roleIds`** |
| | `UserRegisterRequest` + `Set<Role>` | `User` | `toEntity` | idem |
| | `UserUpdateRequest` + `User` | `User` (mutado) | `updateEntity` | copia **incondicionalmente** firstName/lastName/email; **sem null-check** (NPE se `request`/`entity` nulos); não toca senha/roles/active |
| | `User` | `UserResponse` / `UserDetailsResponse` | `toResponse` / `toDetailsResponse` | **percorre `getRoles()`**; coleta em `LinkedHashSet`; sem senha/tokens |
| | `Page<User>` | `Page<UserResponse>` | `toResponsePage` | `Page.map` |
| | `Role` | `RoleResponse` | `toRoleResponse` (privado) | id + authority |

Nenhum mapper chama outro mapper; `ProductMapper` constrói `CategoryResponse`/`CategoryDetailsResponse` diretamente (`new`), sem usar `CategoryMapper`.

## 11. Transformações DTO → Entity [FATO]

| Fluxo real (sequência) | Onde termina cada campo |
|------------------------|-------------------------|
| `POST /categories`: `CategoryController.create` → `CategoryService.create` → `CategoryMapper.toEntity` → `setActive(true)` → `save` | name, description → entidade; `active` → service |
| `POST /products`: `ProductController.create` → `ProductService.create` → `ProductMapper.toEntity` → `setActive(true)` → `syncCategories` → `save` | 4 campos → mapper; `categoryIds` → service; **`date` → descartado** |
| `POST /users`: `UserController.create` → `UserService.create` → (`findRolesByIdsOrThrow`) → `UserMapper.toEntity(req, roles)` → `encode(password)` → `activate()` → `save` | firstName/lastName/email/password(cru) → mapper; roles → service; `password` recodificado |
| `POST /accounts/register`: `AccountController.register` → `AccountService.register` → `UserMapper.toEntity(req, Set.of(ROLE_OPERATOR))` → `encode` → `deactivate()` → `save` | idem; role fixa |
| `PUT /accounts/me`: `AccountController.updateAuthenticatedUser` → `AccountService.updateAuthenticatedUser` → **setters diretos** → `save` | **sem mapper**; normalização no service |
| `PATCH /accounts/me/password`, `POST /accounts/reset-password`, `POST /accounts/resend-activation`, `POST /accounts/password-recovery` | **sem mapper e sem entidade construída a partir do DTO**; o service altera a entidade carregada |
| `EmailService` → `new EmailRegisterRequest(...)` → `new Email(request)` (domínio) | sem mapper |

## 12. Transformações Entity → DTO [FATO]

`Category` → `CategoryMapper` → `CategoryResponse`/`CategoryDetailsResponse` → `CategoryController`; `Product` → `ProductMapper` → `ProductResponse`/`ProductDetailsResponse` → `ProductController`; `User` → `UserMapper` → `UserResponse`/`UserDetailsResponse` → `UserController`/`AccountController` (o `AccountController` recebe `UserResponse` do `AccountService`). `Role` → `RoleResponse` só aninhado. `Token`/`Email` nunca são convertidos em DTO de saída.

## 13. Atualizações parciais — critério real [FATO]

O critério "só se não for `null`" existe **apenas** em dois mappers; não se generaliza:

| Onde | Campos condicionais (não nulos) | Campos incondicionais |
|------|---------------------------------|-----------------------|
| `CategoryMapper.updateEntity` | `name`, `description` | — |
| `ProductMapper.updateEntity` | `name`, `description`, `price`, `imgUrl` | — |
| `UserMapper.updateEntity` | — | `firstName`, `lastName`, `email` (sem null-check) |
| `UserService.updatePasswordIfPresent` (service) | `password` | — |
| `UserService.updateRolesIfPresent` (service) | `roleIds` (nulo mantém; vazio limpa) | — |
| `ProductService.update` (service) | `categoryIds` (nulo mantém; vazio limpa) | — |
| `AccountService.updateAuthenticatedUser` (service) | — | nome, sobrenome, e-mail (com `trim`/minúsculas) |

Consequências: (i) em Categoria/Produto **não é possível limpar** um campo opcional enviando `null` (só `""` para a descrição da categoria; para o produto `""` é recusado pela validação); (ii) o rótulo "PATCH" de `CategoryController.update` e o "PUT" de `ProductController.update`/`UserController.update` não determinam a semântica: o critério é o descrito acima. (iii) Como `name` é obrigatório em Categoria/Produto, na prática só `description`, `price` e `imgUrl` são efetivamente opcionais.

## 14. Tratamento de null [FATO]

| Situação | Comportamento |
|----------|---------------|
| `toEntity(null)`, `toResponse(null)`, `toDetailsResponse(null)` | devolve `null` (Category, Product, User) |
| `updateEntity(null, …)` / `updateEntity(…, null)` | **Category/Product**: no-op silencioso; **User**: `NullPointerException` |
| `toEntity(request, null)` (roles) | não adiciona roles (sem erro) |
| `toResponsePage(null)` | `NullPointerException` (`entities.map`) |
| Campos opcionais do DTO nulos | preservados como `null` na entidade em `toEntity`; ignorados em `updateEntity` (Category/Product) |
| DTOs de resposta | podem conter `null` em `description`, `price`, `imgUrl` (produto) e `description` (categoria) |

## 15. Coleções e relacionamentos [FATO / INFERÊNCIA]

| Coleção | Direção | Mapeada por | Acesso | Observação |
|---------|---------|-------------|--------|------------|
| `Product.categories` | entrada: `categoryIds` (`List<Long>`) → **service** (`syncCategories`); saída: entidade → DTO no **mapper** | `ProductMapper.toResponse/toDetailsResponse` | **percorre** `getCategories()` (relacionamento tardio) | N+1 provável em `search`/`findById` [INFERÊNCIA]; `findAllPaged` carrega com `JOIN FETCH` [FATO B-2] |
| `User.roles` | entrada: `roleIds` (`Set<Long>`) → **service**, que passa `Set<Role>` a `UserMapper.toEntity`; saída: mapper | `UserMapper.toResponse/toDetailsResponse` | **percorre** `getRoles()` (tardio) | N+1 provável em listagens [INFERÊNCIA]; `LinkedHashSet` só preserva a ordem do `HashSet` de origem (indefinida) |
| `User.tokens` | **não mapeada** | — | nunca lida por mapper (não há getter) | nunca exposta |
| `Category.products` | **não mapeada** | — | nunca acessada por `CategoryMapper` | nunca exposta |

## 16. Normalização [FATO]

| Onde | trim | lowercase | Persistido? |
|------|:----:|:---------:|:-----------:|
| `UserMapper.toEntity` (create/register) | não | não | e-mail/nomes **crus** |
| `UserMapper.updateEntity` | não | não | cru |
| `UserService.create` / `update` | não | não | cru |
| `AccountService.register` | não | não | cru |
| `AccountService.updateAuthenticatedUser` | **sim** (nome, sobrenome, e-mail) | **sim** (só e-mail; `toLowerCase()` sem `Locale`) | **normalizado** — único ponto de gravação |
| Validators (`UniqueEmail*`, `UserUpdate*`, `ValidEmail`, `Category*/Product*` unicidade de nome) | sim | sim | **não** (apenas para comparar/validar) |
| Filtros `search` (Product/Category/User) | sim | não | não (só o termo de busca) |
| `findByEmail`/`searchUserAndRolesByEmail`/`findByAuthority` | não | não | comparação exata |

Consequências (sem correção):
- **[FATO]** O valor que os validators aceitam é o normalizado (`trim` + minúsculas), mas o mapper grava o valor cru. Ex.: `@ValidEmail` valida `value.trim().toLowerCase()`, então um e-mail com espaços nas pontas ou em caixa mista passa na validação e é persistido como veio.
- **[INFERÊNCIA]** Como login, recuperação de senha e reenvio comparam o e-mail exatamente, o usuário precisa usar a mesma grafia do cadastro; e `existsByEmailIgnoreCase` só protege duplicatas ignorando a caixa.
- O mesmo vale para `name` de categoria/produto: a unicidade é checada por `trim().toLowerCase()`, mas o nome é gravado cru (`UNIQUE` do banco diferencia caixa) [FATO do código; efeito INFERÊNCIA].

## 17. Campos ignorados/derivados [FATO]

- **`ProductCreateRequest.date`**: confirmado — `ProductMapper.toEntity` **não** o copia, `Product` **não** tem campo `date`, e nenhuma referência a `.date()` existe em `src/main` fora do próprio record (busca por `grep`). É validado (`@PastOrPresent`) e descartado. As datas do produto vêm exclusivamente dos callbacks JPA (`createdAt`/`updatedAt`).
- **`active`**: nunca vem de request; é definido pelos services (`true` na criação por admin, `false` no registro).
- **`categoryIds`/`roleIds`**: ignorados pelos mappers (tratados nos services).
- **`UserMapper.toEntity`**: o parâmetro `roles` substitui `roleIds`.
- **Derivados**: nenhum campo de response é calculado; todos são cópias de campos da entidade (ou coleções convertidas).
- **`ProductResponse`**: JavaDoc antigo dizia que `categories` "vem vazio propositalmente" e listava `createdAt` — ambos incorretos (o mapper preenche as categorias; `createdAt` não existe nesse record). Corrigido.

## 18. Dados sensíveis [FATO]

- **Senha**: aceita **somente em requests** — `UserCreateRequest.password`, `UserRegisterRequest.password`, `UserUpdateRequest.password` (opcional), `PasswordUpdateRequest` (3 campos) e `PasswordResetRequest.password`. **Nenhum response contém senha ou hash** (leitura dos 8 responses).
- **Token**: `PasswordResetRequest.token` (uso único) só em request; **nenhum response devolve token** (nem de ativação/recuperação, nem OAuth2 — este é gerado pelo Authorization Server, fora dos DTOs).
- **Mapper e senha**: `UserMapper.toEntity` copia a senha **em texto** para a entidade; a codificação ocorre no service imediatamente depois (na mesma transação). Entre os dois passos a entidade transitória guarda o texto puro [FATO].
- **`toString()` de `record`**: inclui todos os componentes, portanto imprime senhas/tokens se o objeto for logado. Confirmado que `UserController` loga `UserCreateRequest` e `UserUpdateRequest` inteiros em `DEBUG` (B-0 P1-3); `AccountController` loga só o e-mail; `Product/Category` DTOs também são logados inteiros, mas sem campo sensível.
- **Erros de validação**: o handler devolve apenas nome do campo e mensagem (`FieldMessage`), não o valor rejeitado [FATO, B-0].
- **Dados pessoais devolvidos**: e-mail, nomes, roles, `active` (só detalhe), e — para produtos — datas.
- Nenhuma exposição foi corrigida.

## 19. Fatos
Ver seções acima (rotulados em linha). Destaques: 19 DTOs / 3 mappers; controllers não chamam mappers; `date` descartado; e-mail normalizado só em `AccountService.updateAuthenticatedUser`; "não nulo" só em `CategoryMapper`/`ProductMapper`; `UserMapper.updateEntity` sem null-check e só com 3 campos; preço opcional no DTO/entidade/banco; `UserUpdateRequest.password` e `PasswordResetRequest.password` sem `@Size`; `AuthenticatedUserUpdateRequest` usa `@Email` (não `@ValidEmail`); nenhum response com senha/tokens; três mappers sem tratamento de `null` em `toResponsePage`.

## 20. Inferências
N+1 nas conversões de `Product.categories` e `User.roles` fora de `findAllPaged`; e-mail/nome gravados crus divergem do que foi validado; login/recuperação sensíveis à grafia do e-mail; `PUT/PATCH` não determinam semântica de atualização.

## 21. Hipóteses
1. Comportamento do `BCryptPasswordEncoder` com senhas acima de 72 bytes (o DTO de atualização/reset não impõe `@Size`).
2. `toLowerCase()` sem `Locale` em `AccountService` (efeito em locales como o turco) — não verificado.
3. Se a regex `^.{3,255}$` da descrição rejeita textos multilinha em tempo de execução (deduzido da semântica de `.` em Java; não executado).

## 22. Pontos para as próximas fases
- **Validation (candidata a B-5):** todas as anotações citadas aqui (`@ValidEmail`, `@UniqueEmail`, `@UniqueEmailForAuthenticatedUser`, `@StrongPassword`, `@PasswordPersonalData`, `@ValidRoles`, `@*CreateValid/@*UpdateValid`) — o que cada uma valida de fato, dependência de `HttpServletRequest`, DNS, e a divergência valor validado × valor gravado.
- **Controllers/Security:** endpoints que desempacotam DTOs (`PasswordResetRequest`, `UserEmailRequest`), logs de DTOs com senha, `@PreAuthorize`.
- **Testes (futura):** `ProductControllerIT`/`CategoryControllerIT` esperam `$.description` em `CategoryResponse` e `$.date` em `ProductResponse`, que **não existem** nesses records (B-0 §14.2) — a documentação desta fase confirma que os DTOs de produção não os têm.

## 23. JavaDoc adicionado ou ajustado

| Arquivo(s) | Alterações |
|------------|-----------|
| 19 DTOs | JavaDoc de tipo reescrito ou criado para todos (finalidade, endpoint, fluxo controller→service→mapper, campos com constraints, dados sensíveis, semântica de atualização, campos não persistidos) com `@param` para cada componente. **Correções de JavaDoc antigo incorreto**: `CategoryResponse` (`@param description/active` inexistentes), `CategoryDetailsResponse` ("mesmas informações que `CategoryResponse`"), `EmailRegisterRequest` (campo `subject` e construtor de 4 argumentos inexistentes), `ProductResponse` (`categories` "vazia" e `createdAt` inexistente), `UserCreateRequest`/`UserRegisterRequest` (`{@link UserCreateValid}` inexistente), `UserDetailsResponse` (exemplo sem `active`), exemplos com `ROLE_USER`. Os 4 DTOs de conta (`AuthenticatedUserUpdateRequest`, `PasswordUpdateRequest`, `PasswordResetRequest`, `UserEmailRequest`) não tinham nenhum JavaDoc. |
| `CategoryMapper` | Classe documentada; `toEntity` (corrige "define `active` padrão false caso não informado"), `updateEntity` (critério não nulo), `toResponse`, **novo** `toDetailsResponse`. |
| `ProductMapper` | Classe reescrita; `toEntity` (corrige: ignora `date`/`categoryIds`; não define `active`), `updateEntity`, `toResponse` (**corrige "Não inclui categorias (lista vazia)"**), `toDetailsResponse`, `toResponsePage`. |
| `UserMapper` | Classe documentada (não tinha); 5 blocos que eram comentários `/* * … */` (não JavaDoc) viraram JavaDoc; correções: `toEntity` "associa papéis com base nos IDs" (recebe entidades), `updateEntity` "substitui os papéis" (só 3 campos; sem null-check), `toResponse` "preserva a ordem"; novo JavaDoc de `toRoleResponse`. |

Doclint (`-Xdoclint:all,-missing`) sem avisos para `dto` e `mapper`; sem `@apiNote/@implNote`, sem blocos `/* … */` residuais.

## 24. O que **não** foi feito
Nenhuma alteração de código executável, campos, tipos, anotações de validação, assinaturas, nomes ou regras de atualização; nenhum arquivo em `src/test/**`, `domain`, `repository`, `projection`, `service`, `validation`, controllers, `security`, `config`, migrations, properties ou `pom.xml`; nenhum teste executado; nenhum commit.
