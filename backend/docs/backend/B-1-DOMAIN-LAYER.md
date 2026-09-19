# ASJCatalog Backend — B-1: Camada de Domínio

> **Fase:** B-1 — documentação (JavaDoc) da camada `domain`.
> **Pré-requisito:** [B-0-BACKEND-INVENTORY.md](B-0-BACKEND-INVENTORY.md).
> **Escopo alterado:** somente comentários JavaDoc em `src/main/java/**/domain/**` (nenhuma linha de código executável foi modificada) e este documento.

Rótulos: **[FATO]** observado no código; **[INFERÊNCIA]** deduzido de fatos, sem execução; **[HIPÓTESE]** precisa de verificação.

---

## 1. O que foi analisado

Todos os 9 arquivos do pacote `com.albertsilva.dev.dscatalog.domain` (confirmado por listagem do diretório; o inventário B-0 estava correto quanto à composição da camada) e, para confirmar o uso real de cada membro, as chamadas a eles em `src/main/java` (busca por `grep`). Foram também conferidos os *mappings* contra as migrations Flyway `V001`–`V011`.

## 2. Classes da camada

| Classe | Tipo | Tabela | Papel |
|--------|------|--------|-------|
| `Identifiable<ID>` | interface | — | Contrato `ID getId()`; implementado por `Product` e estendido por `ProductProjection`. |
| `catalog.Category` | entidade | `tb_category` | Agrupamento de produtos; lado **inverso** do N—N com `Product`. |
| `catalog.Product` | entidade (`Identifiable<Long>`) | `tb_product` | Produto; lado **dono** do N—N com `Category` (`tb_product_category`). |
| `user.User` | entidade (`UserDetails`) | `tb_user` | Conta/credencial; lado dono do N—N com `Role` (`tb_user_role`); lado inverso do 1—N com `Token`. |
| `user.Role` | entidade (`GrantedAuthority`) | `tb_role` | Autoridade (`ROLE_ADMIN`, `ROLE_OPERATOR`); relação unidirecional a partir de `User`. |
| `recovery.Token` | entidade | `tb_token` | Token de ativação de conta / recuperação de senha; lado dono do N—1 com `User` (`LAZY`). |
| `recovery.Email` | entidade | `tb_email` | Registro (log) de e-mail enviado; sem relacionamentos. |
| `recovery.enums.TokenType` | enum | `tb_token.type` (texto + `check`) | `ACTIVATION`, `PASSWORD_RECOVERY`. |
| `recovery.enums.EmailStatus` | enum | `tb_email.status` (texto + `check`) | `PENDING`, `SENT`, `ERROR`. |

Não existem, nesta camada, classes utilitárias, exceções próprias, *value objects* ou serviços de domínio.

## 3. JavaDoc adicionado ou ajustado

Critério: documentar tipos, construtores, métodos de domínio/derivados, callbacks e contratos não óbvios; **não** documentar getters/setters triviais. Ao final, o *doclint* (`javadoc -Xdoclint:all,-missing`) não reportou avisos para o pacote; os únicos membros ainda sem comentário são getters/setters triviais, `hashCode` e `serialVersionUID`.

| Arquivo | O que foi feito |
|---------|-----------------|
| `Identifiable` | Novo JavaDoc do contrato e do método `getId()` (quem implementa/estende). |
| `Category` | Reescrita da seção "regras" (antes listava intenções não impostas pela entidade); doc dos 3 construtores; ajuste de `createdAt`, `updatedAt`, `active`, `products` (lado inverso), `getProducts()`, `prePersist()`, `equals`. |
| `Product` | Idem: preço e `active` deixam de ser descritos como regras da entidade; doc dos 2 construtores + o sem-argumentos; `categories`/`getCategories()` (lado dono, sem cascade, sem fetch explícito); callbacks privados `prePersist`/`preUpdate`; `equals`. |
| `User` | Nova descrição como `UserDetails` (mapa dos métodos do contrato), regras/invariantes, mapeamento; docs de `tokens`, `roles`, `password`, `active`, construtores, `addRole`, `setActive`, `activate`, `deactivate`, `hasRole`, `getAuthorities`, `isAccountNonExpired/NonLocked/CredentialsNonExpired`, `isEnabled`, `equals`. |
| `Role` | Relacionamento unidirecional, papel como `GrantedAuthority`, invariantes; construtores; `equals`; exemplo do `getAuthority` corrigido de `ROLE_USER` (não existe nos dados iniciais) para `ROLE_ADMIN`. |
| `Token` | Antes sem nenhum JavaDoc. Agora: classe, todos os campos, construtores, `isExpired`, `isValid`, `disable`, as duas fábricas estáticas, `validate` (ordem e chaves de mensagem), `getDisabled`, `equals`. |
| `Email` | Antes sem nenhum JavaDoc. Agora: classe, campos, os dois construtores, `equals`. |
| `TokenType` / `EmailStatus` | JavaDoc do enum (persistência por nome e `check` das migrations) e de cada constante de `TokenType` (as de `EmailStatus` já existiam). |

Trechos do JavaDoc anterior que estavam **imprecisos em relação ao código** e foram reescritos (sem alterar o código): "o nome da categoria deve representar claramente o agrupamento" e "o preço deve ser maior que zero" (não impostos pelas entidades — o preço é validado nos DTOs), "categorias/produtos inativos podem ser desconsiderados" e "usuários inativos podem ser ignorados" (as entidades apenas guardam o indicador), e "tokens de recuperação" em `User.tokens` (o conjunto contém também tokens de ativação).

## 4. Regras e invariantes de domínio (o que a camada realmente impõe)

Impostas por **mapeamento/banco** [FATO]:

- `Category.name`: obrigatório, único, ≤ 80; `Category.description` ≤ 255.
- `Product.name`: obrigatório, único. `Product.price` é `Double` sem restrição.
- `User.email`: obrigatório, único. Nome, sobrenome e senha: sem restrição.
- `Token.token` único; `user` obrigatório; `createdAt`, `expireDate`, `disabled`, `type` obrigatórios; `createdAt` não atualizável; `type` restrito por `check` às duas constantes.
- `Email`: todos os campos obrigatórios; `status` restrito por `check`.
- `Role.authority`: sem restrições (nem `unique`).

Impostas por **código** na própria entidade [FATO]:

- `Token.validate(TokenType)`: recusa por tipo diferente → desabilitado → expirado (nessa ordem), com `InvalidTokenException` e chaves `error.token.type.invalid`, `error.token.disabled`, `error.token.expired`.
- `Token.isExpired()`: `expireDate` estritamente anterior a `Instant.now()`. `Token.isValid()`: não desabilitado e não expirado (não verifica tipo).
- `Token.activationToken/passwordRecoveryToken`: UUID aleatório; expiração = agora + horas/minutos; sem validação dos argumentos.
- `Email(EmailRegisterRequest)`: `createdAt = agora`, `status = PENDING`.
- `User.activate/deactivate/hasRole`; `User.isEnabled() == active`; demais `is…NonExpired/Locked` sempre `true`.
- Callbacks de datas: `Category.prePersist` define só `createdAt`; `Product.prePersist` define `createdAt` e `updatedAt`; ambos definem `updatedAt` no `preUpdate`.

**Não** impostas pelo domínio (ficam em DTOs/validators/services): formato e unicidade "de negócio" de nome/e-mail, preço positivo, categorias obrigatórias em produto, força de senha, codificação de senha, transições de `EmailStatus`, desabilitação do token após o uso.

## 5. Relacionamentos importantes [FATO]

```
Product ⇄ Category      N—N   dono: Product (tb_product_category), sem cascade; Category.products é mappedBy
User    ⇄ Role          N—N   dono: User    (tb_user_role), sem cascade; Role não conhece User
User    ←  Token        1—N   dono: Token.user (LAZY, not null); User.tokens: cascade ALL + orphanRemoval, sem getter
Email                    (nenhum relacionamento; destinatário é texto)
```

## 6. Observações e possíveis problemas (nada foi corrigido)

| # | Observação | Rótulo |
|---|------------|--------|
| 1 | O domínio depende de outras camadas: `Token` → `service.exception.InvalidTokenException`; `Email` → `dto.email.request.EmailRegisterRequest`; `User`/`Role` → Spring Security. | FATO |
| 2 | `Product.createdAt`/`updatedAt` só são preenchidos por `@PrePersist` (método privado). Um `Product` recém-instanciado tem ambos nulos — é a causa observada da falha `ProductTest.productShouldInstantiateCorrectly` registrada no B-0 (problema da camada de testes, não da entidade). | FATO |
| 3 | `equals`/`hashCode` de todas as entidades usam apenas `id`: duas instâncias transitórias (`id == null`) são iguais entre si, `hashCode` muda quando o `id` é atribuído na persistência e a comparação usa `getClass()` exato. Os relacionamentos usam `HashSet` (`User.roles`, `User.tokens`, `Product.categories`, `Category.products`). Risco de comportamento inesperado ao inserir entidades ainda sem `id` em conjuntos antes de persistir. | INFERÊNCIA |
| 4 | Por usar `getClass()`, comparar um *proxy* do Hibernate com a entidade real pode resultar em `false`; os services usam `getReferenceById` nas atualizações. Não verificado. | HIPÓTESE |
| 5 | `Token.isValid()` não tem chamadores em `src/main` nem em `src/test`; `User.hasRole(String)` não tem chamadores em `src/main` (é exercitado em `UserTest`). O fluxo de tokens usa `Token.validate`. | FATO |
| 6 | `Email.setStatus` não é chamado em `src/main`; o único status atribuído é `PENDING` (no construtor). `SENT` e `ERROR` nunca são atribuídos. | FATO |
| 7 | Assimetrias `Category` × `Product`: `Category.prePersist()` é público e não define `updatedAt`; `Product.prePersist()` é privado e define ambos; só `Product` implementa `Identifiable`. | FATO |
| 8 | `Product.price` é `Double` (coluna `float(53)`) — ponto flutuante para valor monetário, sem validação na entidade. | FATO |
| 9 | `Token.getDisabled()` foge do padrão `isXxx` de booleanos; e `Token` expõe `setDisabled`, `setExpireDate`, `setUser`, `setId` públicos, contornando `disable()`. | FATO |
| 10 | `User` usado como `UserDetails`: `isAccountNonExpired/Locked/CredentialsNonExpired` sempre `true`; não há modelo de bloqueio ou expiração. Além disso, `UserService.loadUserByUsername` monta um `User` **parcial** (id, e-mail, senha, `active`, roles; nome/sobrenome nulos) — ponto para a fase de services/segurança. | FATO |
| 11 | `Token.isExpired()` lança `NullPointerException` se `expireDate` for nulo (coluna obrigatória, portanto só possível em instâncias montadas manualmente). | FATO |
| 12 | `Token.expireDate` na *fábrica* usa o relógio da aplicação (`Instant.now()`); colunas de data usam `TIMESTAMP WITHOUT TIME ZONE` — comportamento de fuso na persistência não verificado. | HIPÓTESE |
| 13 | `Role.authority` não é `unique` nem `not null` no banco; nada na camada impede duplicidade de roles. | FATO |
| 14 | Não foi identificado, na camada de repositório, filtro pelo indicador `active` de `Category`/`Product`; o indicador parece apenas informativo. | INFERÊNCIA (baseada na leitura do B-0; não reexaminada linha a linha nesta fase) |

## 7. Pontos que precisam ser compreendidos nas próximas fases

1. **Repositories** (candidata a B-2): as queries e *derived queries* dependem diretamente destes mapeamentos (`Product.categories` com `JOIN FETCH`, `Token` por `token`/usuário/tipo, projeção `UserDetailsProjection` contornando a entidade `User` completa).
2. **Services**: quem chama `disable()`, `activate()`, `setActive()` e `validate()`; quem codifica senhas; quem define `active` na criação (as entidades nascem inativas).
3. **Transações e *lazy loading*** de `Token.user` (usado em `AccountService.confirmEmail`/`resetPassword` via `token.getUser()`).
4. **Segurança**: uso de `User`/`Role` como `UserDetails`/`GrantedAuthority` e o claim `authorities`.
5. Efeito real de `equals`/`hashCode` (itens 3 e 4 acima).

## 8. O que **não** foi feito nesta fase

Nenhuma alteração em código executável, mappings JPA, cardinalidades, `fetch`, `cascade`, `orphanRemoval`, `equals/hashCode`, construtores, modificadores, anotações, nomes ou lógica; nenhum arquivo em `src/test/**`, migrations, DTOs, services, repositories, controllers, security, validators ou configuração; nenhum teste corrigido; nenhum commit.
