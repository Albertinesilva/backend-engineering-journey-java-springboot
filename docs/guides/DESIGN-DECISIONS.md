# Decisões técnicas

⬅️ Anterior: [Convenções](CONVENTIONS.md) · [🏠 Índice](../HOME.md)

Este guia reúne as principais decisões técnicas do ASJCatalog: o contexto de cada uma, o que foi decidido, por quê, que alternativas existem e quais são as consequências.

O projeto nasceu da base do DSCatalog, do curso DevSuperior, e evoluiu de forma independente. Por isso, várias decisões vieram dessa base e outras surgiram ao longo da evolução do projeto. Quando o motivo de uma decisão está registrado no código, no histórico de commits, nos guias ou nas [auditorias](../audits/), o campo **Por quê?** cita a fonte. Quando ainda não está, o campo traz uma pergunta para o autor responder.

O campo **Alternativas possíveis** lista opções reais do ecossistema. Ele não afirma que essas opções foram avaliadas na época. Quando a [revisão arquitetural B-9](../audits/B-9-ARCHITECTURAL-REVIEW.md#15-decisões-que-precisam-ser-tomadas-antes-de-corrigir) registra opções para a decisão, elas aparecem aqui.

## Resumo

| Decisão | Guia | Motivo |
| --- | --- | --- |
| [D-01 — Arquitetura em camadas com DTOs e mappers](#d-01--arquitetura-em-camadas-com-dtos-e-mappers) | [ARCHITECTURE](ARCHITECTURE.md) | Documentado |
| [D-02 — Pacote `domain` dividido em módulos](#d-02--pacote-domain-dividido-em-módulos) | [DOMAIN-MODEL](DOMAIN-MODEL.md) | Documentado |
| [D-03 — Regras de negócio dentro das entidades](#d-03--regras-de-negócio-dentro-das-entidades) | [DOMAIN-MODEL](DOMAIN-MODEL.md) | Documentado |
| [D-04 — Listagem de produtos em duas consultas](#d-04--listagem-de-produtos-em-duas-consultas) | [DATA-ACCESS](DATA-ACCESS.md) | Documentado |
| [D-05 — SQL nativo e projections na busca de produtos e no login](#d-05--sql-nativo-e-projections-na-busca-de-produtos-e-no-login) | [DATA-ACCESS](DATA-ACCESS.md) | A preencher |
| [D-06 — Open Session in View desligado](#d-06--open-session-in-view-desligado) | [DATA-ACCESS](DATA-ACCESS.md) | Documentado |
| [D-07 — Migrations em três pastas, com versão global](#d-07--migrations-em-três-pastas-com-versão-global) | [DATABASE-MIGRATIONS](DATABASE-MIGRATIONS.md) | Documentado |
| [D-08 — Perfis `dev`, `test` e `prod`, com `dev` como padrão](#d-08--perfis-dev-test-e-prod-com-dev-como-padrão) | [CONFIGURATION](CONFIGURATION.md) | A preencher |
| [D-09 — Perfil `test` com H2, sem Flyway](#d-09--perfil-test-com-h2-sem-flyway) | [TESTING](TESTING.md) | A preencher |
| [D-10 — Login com `grant_type=password`](#d-10--login-com-grant_typepassword) | [AUTHENTICATION](AUTHENTICATION.md) | A preencher |
| [D-11 — Chave RSA e autorizações em memória, com rotação do refresh token](#d-11--chave-rsa-e-autorizações-em-memória-com-rotação-do-refresh-token) | [AUTHENTICATION](AUTHENTICATION.md) | A preencher |
| [D-12 — Tokens de conta gravados no banco](#d-12--tokens-de-conta-gravados-no-banco) | [ACCOUNT-FLOWS](ACCOUNT-FLOWS.md) | A preencher |
| [D-13 — Respostas neutras na recuperação e no reenvio](#d-13--respostas-neutras-na-recuperação-e-no-reenvio) | [ACCOUNT-FLOWS](ACCOUNT-FLOWS.md) | Documentado |
| [D-14 — Cadastro público com `ROLE_OPERATOR`](#d-14--cadastro-público-com-role_operator) | [ACCOUNT-FLOWS](ACCOUNT-FLOWS.md) | A preencher |
| [D-15 — Envio de e-mail síncrono](#d-15--envio-de-e-mail-síncrono) | [ACCOUNT-FLOWS](ACCOUNT-FLOWS.md) | A preencher |
| [D-16 — Formato de erro próprio com código estável](#d-16--formato-de-erro-próprio-com-código-estável) | [ERROR-HANDLING](ERROR-HANDLING.md) | A preencher |
| [D-17 — Idioma escolhido pelo `Accept-Language`](#d-17--idioma-escolhido-pelo-accept-language) | [INTERNATIONALIZATION](INTERNATIONALIZATION.md) | Documentado |
| [D-18 — E-mail validado pelo registro MX e senha de até 72 caracteres](#d-18--e-mail-validado-pelo-registro-mx-e-senha-de-até-72-caracteres) | [VALIDATION](VALIDATION.md) | A preencher |
| [D-19 — Testes de integração no Maven Failsafe](#d-19--testes-de-integração-no-maven-failsafe) | [TESTING](TESTING.md) | Documentado |

## Decisões

### D-01 — Arquitetura em camadas com DTOs e mappers

- **Contexto:** a API recebe requisições HTTP, aplica regras de negócio e grava no banco. Essas responsabilidades precisavam de um lugar definido no código.
- **Decisão:** separar a aplicação em controllers (`web.controller`), services (`service`), repositórios (`repository`) e entidades (`domain`). Entre as camadas circulam DTOs (`record`s em `dto`), convertidos por mappers dedicados (`mapper`). Os controllers nunca recebem nem devolvem entidades.
- **Por quê?** Cada camada tem uma responsabilidade e só conversa com a vizinha. Separar a entrada da saída deixa claro o que o cliente envia e o que recebe, e impede que a API exponha as entidades. Os mappers tiram a conversão do controller e do service. Fonte: [ARCHITECTURE.md](ARCHITECTURE.md#1-camadas), seções 1 e 4.
- **Alternativas possíveis:** devolver as entidades diretamente nos controllers; usar projections do Spring Data como DTOs de saída; gerar os mappers com uma biblioteca como o MapStruct.
- **Consequências:** cada recurso tem DTOs de entrada e de saída e um mapper, e uma mudança de contrato da API não exige mudar a entidade. Em compensação, há mais classes por recurso, e a conversão é escrita à mão.
- **Guia relacionado:** [ARCHITECTURE.md](ARCHITECTURE.md)

### D-02 — Pacote `domain` dividido em módulos

- **Contexto:** as entidades ficavam num pacote `entity`. Com os fluxos de conta, surgiram `Token` e `Email`, e o número de entidades cresceu.
- **Decisão:** mover as entidades para `domain` (commit `1644d08`) e separá-las por contexto de negócio em `catalog`, `user` e `recovery` (commit `758a011`).
- **Por quê?** Para agrupar conceitos relacionados: o catálogo, os usuários e a recuperação de acesso. Fontes: a mensagem do commit `758a011` ("separa entidades por contexto de negócio") e [ARCHITECTURE.md](ARCHITECTURE.md#4-onde-fica-cada-tipo-de-classe), seção 4.
- **Alternativas possíveis:** manter um pacote único de entidades; organizar o projeto inteiro por funcionalidade, com controller, service, repositório e entidade de cada módulo no mesmo pacote.
- **Consequências:** as entidades de cada assunto ficam juntas, mas as outras camadas (`service`, `repository`, `dto`) continuam organizadas por tipo de classe. O módulo `recovery` também guarda o `Email`, que registra todos os e-mails enviados, e não só os de recuperação.
- **Guia relacionado:** [DOMAIN-MODEL.md](DOMAIN-MODEL.md)

### D-03 — Regras de negócio dentro das entidades

- **Contexto:** algumas regras dizem respeito ao estado de uma única entidade, como a validade de um token ou a ativação de um usuário.
- **Decisão:** colocar essas regras nas próprias entidades: `Token.validate(tipo)`, `Token.isExpired()`, `Token.disable()`, os factory methods `Token.activationToken()` e `Token.passwordRecoveryToken()`, e `User.activate()` e `User.deactivate()`.
- **Por quê?** A decisão de aceitar um token é do domínio: o `TokenService` apenas a aciona. Fonte: JavaDoc do `TokenService` ("a decisão de aceitar um token é do domínio").
- **Alternativas possíveis:** um modelo anêmico, em que as entidades só guardam dados e todas as regras ficam nos services.
- **Consequências:** as regras do token ficam num só lugar, e as regras das entidades podem ser testadas em Java puro, sem o Spring, como fazem `CategoryTest`, `ProductTest` e `UserTest`. As regras que dependem de outras entidades ou do banco continuam nos services.
- **Guia relacionado:** [DOMAIN-MODEL.md](DOMAIN-MODEL.md)

### D-04 — Listagem de produtos em duas consultas

- **Contexto:** a listagem de produtos é paginada e devolve as categorias de cada produto. Carregar as categorias produto a produto gera o problema N+1.
- **Decisão:** a primeira consulta aplica os filtros e a paginação e devolve só id e nome. A segunda carrega os produtos da página com as categorias, usando `JOIN FETCH`. Depois, `IdentifiableUtils.reorderByReference` devolve a ordem da primeira consulta.
- **Por quê?** Para evitar uma consulta por produto. Paginar sobre linhas simples garante que o `LIMIT` do banco conte produtos, e não pares produto-categoria, e a reordenação corrige a ordem, que a segunda consulta não garante. Fonte: [DATA-ACCESS.md](DATA-ACCESS.md#5-o-problema-n1-e-a-listagem-de-produtos), seção 5.
- **Alternativas possíveis:** `@EntityGraph` no repositório; carregamento em lote do Hibernate (`@BatchSize` ou `hibernate.default_batch_fetch_size`); uma consulta única com `JOIN FETCH` e paginação, que o Hibernate faz em memória e sinaliza com um aviso.
- **Consequências:** a listagem usa sempre duas consultas, independentemente do tamanho da página. A interface `Identifiable` existe para que a reordenação funcione com `Product` e `ProductProjection`.
- **Guia relacionado:** [DATA-ACCESS.md](DATA-ACCESS.md)

### D-05 — SQL nativo e projections na busca de produtos e no login

- **Contexto:** duas consultas do projeto são escritas em SQL nativo e devolvem projections: `ProductRepository.searchProducts`, que filtra e pagina os produtos, e `UserRepository.searchUserAndRolesByEmail`, que carrega o usuário e as roles no login.
- **Decisão:** usar `nativeQuery = true` nessas duas consultas, com `ProductProjection` (id e nome) e `UserDetailsProjection` (id, e-mail, senha, status e role, uma linha por role).
- **Por quê?**
  - **Pergunta:** Por que essas duas consultas foram escritas em SQL nativo, e não em JPQL? Havia alguma limitação do JPQL ou alguma vantagem do SQL nativo que motivou a escolha?
  - **Resposta:** _a preencher pelo autor_
- **Alternativas possíveis:** JPQL com `JOIN` e `DISTINCT`; a Criteria API ou Specifications do Spring Data, para os filtros opcionais; a Querydsl.
- **Consequências:** o login traz tudo o que precisa numa única consulta, sem carregar a entidade `User` e depois as roles. As consultas dependem dos nomes das tabelas e do SQL do banco. A busca de produtos só ordena por `id` e `name`, e `sort=price` responde 500. Produtos sem categoria e usuários sem role não aparecem, por causa do `INNER JOIN` ([DATA-ACCESS.md](DATA-ACCESS.md#8-limitações-conhecidas)).
- **Guia relacionado:** [DATA-ACCESS.md](DATA-ACCESS.md#3-consultas-jpql-e-nativas)

### D-06 — Open Session in View desligado

- **Contexto:** por padrão, o Spring Boot mantém a conexão com o banco aberta até o fim da requisição (*Open Session in View*), o que permite carregar relacionamentos *lazy* em qualquer ponto, inclusive no controller.
- **Decisão:** `spring.jpa.open-in-view=false` no `application.properties`.
- **Por quê?** O recurso esconde consultas extras. Desligado, todos os dados precisam ser carregados dentro do service, com a transação aberta. Fonte: [DATA-ACCESS.md](DATA-ACCESS.md#7-open-in-view-e-transações), seção 7.
- **Alternativas possíveis:** manter o padrão do Spring Boot (ligado).
- **Consequências:** acessar um relacionamento *lazy* fora da transação gera `LazyInitializationException`. É por isso que a listagem de produtos usa `JOIN FETCH` antes de converter para DTO (D-04).
- **Guia relacionado:** [DATA-ACCESS.md](DATA-ACCESS.md)

### D-07 — Migrations em três pastas, com versão global

- **Contexto:** o banco de produção precisa da estrutura e das roles, mas não dos usuários e produtos de exemplo usados em desenvolvimento.
- **Decisão:** separar as migrations do Flyway em `schema` (estrutura), `reference` (roles) e `data` (exemplos). O perfil `dev` aplica as três pastas e o `prod`, só `schema` e `reference`. A numeração é global: toda migration nova recebe a maior versão existente + 1, em qualquer pasta.
- **Por quê?** Para que os dados de exemplo nunca cheguem à produção. A numeração global existe porque o Flyway recusa uma migration com versão menor que a última aplicada. Fontes: [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md#3-pastas-e-versões), seções 3 e 4; commit `67a215a` ("move roles para a pasta reference e aplica em todos os perfis").
- **Alternativas possíveis:** uma pasta única com dados de exemplo removidos à mão; carregar os dados de exemplo fora do Flyway (`spring.sql.init`); a opção `outOfOrder` do Flyway; os *contexts* do Liquibase.
- **Consequências:** a pasta decide em que perfis a migration roda, e o número decide a ordem. As faixas antigas (`V001`–`V011` e `V100`–`V105`) não indicam onde numerar a próxima. Os vínculos de exemplo usam ids fixos.
- **Guia relacionado:** [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md)

### D-08 — Perfis `dev`, `test` e `prod`, com `dev` como padrão

- **Contexto:** a aplicação roda em desenvolvimento (PostgreSQL local), nos testes automatizados (H2) e em produção.
- **Decisão:** três perfis, escolhidos pela variável `APP_PROFILE`, com `dev` como padrão quando ela não existe (até o commit `fefd043`, o padrão era `test`). No `prod`, segredos, credenciais e endereços não têm valor padrão, e a aplicação não sobe se algum faltar (*fail fast*).
- **Por quê?**
  - **Pergunta:** Por que o perfil padrão, usado quando `APP_PROFILE` não está definida, passou de `test` para `dev` no commit `fefd043`? E por que a configuração foi dividida em três perfis, em vez de um arquivo único com variáveis de ambiente?
  - **Resposta:** _a preencher pelo autor_
- **Alternativas possíveis:** manter `test` como padrão ou não ter padrão, exigindo `APP_PROFILE` sempre; guardar a configuração de produção fora do repositório, como num servidor de configuração ou num gerenciador de segredos (B-9 §15, item 8).
- **Consequências:** quem clona o projeto sobe direto no `dev`, com PostgreSQL e Flyway, e precisa definir `POSTGRES_DATASOURCE_USER` e `POSTGRES_DATASOURCE_PASSWORD`. Em `prod`, uma variável esquecida impede a subida em vez de deixar a aplicação rodar com um valor errado. Os valores padrão de desenvolvimento ficam versionados no `application.properties`.
- **Guia relacionado:** [CONFIGURATION.md](CONFIGURATION.md)

### D-09 — Perfil `test` com H2, sem Flyway

- **Contexto:** os testes automatizados precisam de um banco com dados conhecidos e não devem depender de um PostgreSQL instalado.
- **Decisão:** no perfil `test`, usar o H2 em memória, desligar o Flyway, deixar o Hibernate criar as tabelas a partir das entidades e carregar os dados de exemplo pelo `import.sql`.
- **Por quê?**
  - **Pergunta:** Por que o perfil `test` cria as tabelas pelo Hibernate e carrega os dados pelo `import.sql`, em vez de aplicar as migrations do Flyway também no H2? E por que H2, e não um PostgreSQL de teste?
  - **Resposta:** _a preencher pelo autor_
- **Alternativas possíveis:** aplicar as migrations do Flyway no H2; usar Testcontainers com PostgreSQL, citado como opção na auditoria [B-8](../audits/B-8-CONFIG-INFRASTRUCTURE.md).
- **Consequências:** os testes rodam sem banco externo. As migrations não são testadas: um erro numa migration só aparece ao subir nos perfis `dev` ou `prod`. O `import.sql` precisa ser mantido em paralelo às migrations de `data`.
- **Guia relacionado:** [TESTING.md](TESTING.md#4-perfil-test-h2-e-importsql)

### D-10 — Login com `grant_type=password`

- **Contexto:** o cliente precisa trocar o e-mail e a senha do usuário por um token de acesso.
- **Decisão:** usar o grant `password`, em que o cliente envia o e-mail e a senha diretamente para `POST /oauth2/token`. Como o Spring Authorization Server não oferece mais esse tipo, ele foi implementado no projeto, no pacote `security.oauth2.grant.password`.
- **Por quê?**
  - **Pergunta:** Por que o login usa o grant `password`, que o Spring Authorization Server não oferece mais e que precisou ser implementado no projeto, em vez de outro fluxo, como o *authorization code* com PKCE?
  - **Resposta:** _a preencher pelo autor_
- **Alternativas possíveis:** *authorization code* com PKCE, com página de login no servidor de autorização; um endpoint de login próprio que emita o JWT sem o Spring Authorization Server.
- **Consequências:** o login é uma única requisição, simples de chamar pelo terminal ou por um front-end próprio. O projeto mantém o código do grant (conversor, token e provider), e as mensagens de erro do login não passam pela internacionalização.
- **Guia relacionado:** [AUTHENTICATION.md](AUTHENTICATION.md#2-login)

### D-11 — Chave RSA e autorizações em memória, com rotação do refresh token

- **Contexto:** os access tokens são JWT assinados com RSA (RS256), e os refresh tokens precisam ser guardados para serem reconhecidos na renovação.
- **Decisão:** gerar um par de chaves RSA de 2048 bits em memória a cada subida (`AuthorizationServerConfig.generateRsa()`), guardar as autorizações em memória (`InMemoryOAuth2AuthorizationService`) e emitir um novo refresh token a cada renovação (`reuseRefreshTokens(false)`), com validade fixa de 30 dias.
- **Por quê?**
  - **Pergunta:** Por que a chave de assinatura é gerada a cada subida e as autorizações ficam em memória, em vez de usar uma chave persistida e um armazenamento em banco? E por que o refresh token é rotacionado a cada uso e vale 30 dias?
  - **Resposta:** _a preencher pelo autor_
- **Alternativas possíveis:** chave persistida ou externa, como um *keystore* ou um serviço de chaves (B-9 §15, item 10); autorizações gravadas em banco com `JdbcOAuth2AuthorizationService`, o que também permite revogação (B-9 §15, item 11); reutilizar o mesmo refresh token até o vencimento; validade configurável por variável de ambiente.
- **Consequências:** não há configuração de chaves. Por outro lado, **todo reinício invalida todos os tokens** (também a cada reinício do DevTools), a aplicação não funciona com mais de uma instância e não existe logout nem revogação. A rotação faz um refresh token já usado responder `invalid_grant`.
- **Guia relacionado:** [AUTHENTICATION.md](AUTHENTICATION.md#5-a-chave-de-assinatura-muda-a-cada-subida)

### D-12 — Tokens de conta gravados no banco

- **Contexto:** a ativação de conta e a recuperação de senha enviam por e-mail um link com um token de uso único.
- **Decisão:** gerar o token como um UUID aleatório e gravá-lo na tabela `tb_token`, com tipo, validade (24 horas para ativação, 30 minutos para recuperação) e indicador de desativação. Antes de criar um token novo, os tokens ainda ativos do mesmo tipo são desativados, e um token usado com sucesso também é desativado.
- **Por quê?**
  - **Pergunta:** Por que os tokens de ativação e de recuperação são UUIDs gravados no banco, em vez de tokens assinados (como um JWT) que não precisam ser gravados? E por que só um token de cada tipo fica válido por vez?
  - **Resposta:** _a preencher pelo autor_
- **Alternativas possíveis:** tokens assinados com validade embutida, sem gravação no banco; gravar só o *hash* do token, e não o valor.
- **Consequências:** a aplicação consegue invalidar um token a qualquer momento e consultar os tokens no banco, o que permite testar os fluxos sem servidor de e-mail. O valor do token fica gravado em texto na tabela.
- **Guia relacionado:** [ACCOUNT-FLOWS.md](ACCOUNT-FLOWS.md#6-tokens-de-conta)

### D-13 — Respostas neutras na recuperação e no reenvio

- **Contexto:** `POST /accounts/password-recovery` e `POST /accounts/resend-activation` recebem um e-mail que pode ou não estar cadastrado.
- **Decisão:** responder sempre `204`, exista ou não o e-mail.
- **Por quê?** Para não revelar se o e-mail está cadastrado. Fontes: [ACCOUNT-FLOWS.md](ACCOUNT-FLOWS.md#3-reenvio-de-ativação), seção 3, e [GETTING-STARTED.md](GETTING-STARTED.md#10-limitações-conhecidas), seção 10.
- **Alternativas possíveis:** responder `404` quando o e-mail não existe.
- **Consequências:** esses dois endpoints não permitem descobrir quais e-mails estão cadastrados. O cadastro (`POST /accounts/register`) ainda permite: um e-mail repetido responde `422` (B-9, item S-11).
- **Guia relacionado:** [ACCOUNT-FLOWS.md](ACCOUNT-FLOWS.md)

### D-14 — Cadastro público com `ROLE_OPERATOR`

- **Contexto:** qualquer pessoa pode criar uma conta em `POST /accounts/register`.
- **Decisão:** a conta nasce **inativa**, recebe a role `ROLE_OPERATOR` e só pode fazer login depois da ativação pelo link enviado por e-mail.
- **Por quê?**
  - **Pergunta:** Por que a conta criada pelo cadastro público recebe `ROLE_OPERATOR`, que tem permissão para criar, alterar e remover categorias e produtos, em vez de uma role sem permissão de escrita no catálogo?
  - **Resposta:** _a preencher pelo autor_
- **Alternativas possíveis:** uma role inicial sem permissão de escrita no catálogo (B-9 §15, item 14); conta ativa já no cadastro, sem confirmação por e-mail.
- **Consequências:** depois de ativar a conta, qualquer pessoa cadastrada pode alterar o catálogo. A ativação por e-mail confirma que a pessoa tem acesso ao endereço informado.
- **Guia relacionado:** [ACCOUNT-FLOWS.md](ACCOUNT-FLOWS.md#2-cadastro-ativação-e-login)

### D-15 — Envio de e-mail síncrono

- **Contexto:** o cadastro, o reenvio de ativação e a recuperação de senha enviam e-mails pelo SMTP.
- **Decisão:** os métodos de envio do `EmailService` têm `@Async`, mas o projeto não tem `@EnableAsync`, e o envio acontece na mesma thread da requisição. Uma falha de envio é registrada só no log. A aplicação testa a conexão SMTP ao subir nos perfis `dev` e `prod` (`spring.mail.test-connection=true`); no `test`, o teste é desligado.
- **Por quê?**
  - **Pergunta:** O envio síncrono foi intencional, ou a ideia era assíncrona, já que os métodos têm `@Async` e se chamam `...Async`? Por que uma falha de envio fica só no log, sem avisar o cliente? E por que a conexão SMTP é testada na subida dos perfis `dev` e `prod`?
  - **Resposta:** _a preencher pelo autor_
- **Alternativas possíveis:** habilitar `@EnableAsync`; o padrão *outbox*, que grava o e-mail e o envia depois; timeout e nova tentativa no envio; ligar ou desligar o teste de conexão por perfil (B-9 §15, item 9).
- **Consequências:** a requisição espera o servidor SMTP responder. Se o envio falha, o cliente recebe sucesso, a conta fica inativa e o token fica gravado. Sem credenciais de e-mail válidas, a aplicação não sobe em `dev` sem `--spring.mail.test-connection=false`. Desligar o teste no perfil `test` foi feito para os testes não precisarem de credenciais de e-mail (commit `2c8c7d1`).
- **Guia relacionado:** [ACCOUNT-FLOWS.md](ACCOUNT-FLOWS.md#7-e-mails)

### D-16 — Formato de erro próprio com código estável

- **Contexto:** a API precisa devolver erros num formato previsível, em três idiomas.
- **Decisão:** usar a classe própria `ProblemDetails` (`timestamp`, `status`, `code`, `error`, `message` e `path`), montada pelo `ControllerExceptionHandler`, com o campo `code` vindo do enum `ApiErrorCode`.
- **Por quê?**
  - **Pergunta:** Por que a API usa uma classe de erro própria, em vez do formato da RFC 9457 (*Problem Details for HTTP APIs*) ou da classe `ProblemDetail` do próprio Spring, que segue essa RFC?
  - **Resposta:** _a preencher pelo autor_
- **Alternativas possíveis:** o formato da RFC 9457, com a classe `ProblemDetail` do Spring (B-9 §15, item 5).
- **Consequências:** o `code` não muda com o idioma e serve para o cliente tomar decisões, enquanto `error` e `message` são traduzidos (anotação `@Schema` do `ApiErrorCode`). O nome `ProblemDetails` lembra a RFC, mas o formato é outro. Os erros que não passam pelo handler, como os do login, têm outro formato.
- **Guia relacionado:** [ERROR-HANDLING.md](ERROR-HANDLING.md)

### D-17 — Idioma escolhido pelo `Accept-Language`

- **Contexto:** as mensagens da API precisam existir em português, inglês e espanhol.
- **Decisão:** a classe `MessageSourceConfig` declara o `messageSource` (arquivos `messages_*.properties` em UTF-8) e um `AcceptHeaderLocaleResolver`. O idioma vem do cabeçalho `Accept-Language` de cada requisição, com `pt_BR` como padrão.
- **Por quê?** Para preparar a aplicação para mais de um idioma, sem textos fixos no código. Fonte: [INTERNATIONALIZATION.md](INTERNATIONALIZATION.md#1-visão-geral), seção 1.
- **Alternativas possíveis:** configurar o idioma só pelas propriedades `spring.messages.*`; guardar o idioma em sessão ou em cookie (`SessionLocaleResolver` ou `CookieLocaleResolver`).
- **Consequências:** nada fica guardado em sessão: cada requisição traz o seu idioma, e um idioma não suportado cai no português. As propriedades `spring.messages.*` e `spring.web.locale*` não têm efeito no projeto; a configuração que dependia delas foi removida no commit `6b1ad04`.
- **Guia relacionado:** [INTERNATIONALIZATION.md](INTERNATIONALIZATION.md)

### D-18 — E-mail validado pelo registro MX e senha de até 72 caracteres

- **Contexto:** o cadastro e os fluxos de conta recebem e-mails e senhas que precisam ser válidos antes de chegar ao service.
- **Decisão:** o `@ValidEmail` confere o formato e consulta no DNS se o domínio tem registro MX. As senhas têm entre 10 e 72 caracteres nos DTOs que usam `@Size`; o limite de 72 existe porque o BCrypt só considera os primeiros 72 bytes ([VALIDATION.md](VALIDATION.md#2-regras-por-dto)).
- **Por quê?**
  - **Pergunta:** Por que o `@ValidEmail` consulta o registro MX do domínio no DNS, além de conferir o formato, se a ativação por e-mail já confirma que o endereço existe?
  - **Resposta:** _a preencher pelo autor_
- **Alternativas possíveis:** conferir só o formato com o `@Email` padrão e deixar a confirmação para o e-mail de ativação.
- **Consequências:** e-mails de domínios sem servidor de e-mail são recusados já no cadastro. A validação depende de internet: uma falha de DNS recusa e-mails válidos, e os testes que usam `@ValidEmail` falham sem acesso ao DNS. `PUT /accounts/me` usa só o `@Email`, então aceita e-mails que o cadastro recusaria.
- **Guia relacionado:** [VALIDATION.md](VALIDATION.md#6-validação-de-e-mail)

### D-19 — Testes de integração no Maven Failsafe

- **Contexto:** o projeto tem testes de unidade e testes de integração (`*IT`), que sobem a aplicação inteira.
- **Decisão:** acrescentar o `maven-failsafe-plugin` ao `pom.xml` (commit `e1f2330`). O Surefire roda as classes `*Test` e `*Tests` na fase `test`, e o Failsafe roda as `*IT` nas fases `integration-test` e `verify`.
- **Por quê?** Sem o Failsafe, `mvn verify` não executava os `*IT`, porque o Surefire padrão ignora essas classes. Fontes: auditoria [T-0](../audits/T-0-TEST-BASELINE.md), que registra que o build não executava os `*IT` e deixa a decisão em aberto, e [TESTING.md](TESTING.md#2-surefire-e-failsafe), seção 2.
- **Alternativas possíveis:** incluir os `*IT` no Surefire com `includes`; um perfil Maven separado para os testes de integração.
- **Consequências:** `./mvnw test` roda só o Surefire, e `./mvnw verify` roda os dois. O nome da classe, e não o tipo do teste, decide o plugin.
- **Guia relacionado:** [TESTING.md](TESTING.md)

## Trade-offs reconhecidos

Efeitos das decisões acima que o projeto conhece e mantém por enquanto. Os detalhes estão nas seções "Limitações conhecidas" dos guias.

| Trade-off | Consequência | Decisão | Onde está descrito |
| --- | --- | --- | --- |
| Chave RSA e autorizações em memória | Todo reinício invalida os tokens; a aplicação não funciona com mais de uma instância | D-11 | [AUTHENTICATION.md](AUTHENTICATION.md#5-a-chave-de-assinatura-muda-a-cada-subida) |
| Sem logout nem revogação | Um token emitido vale até vencer, mesmo depois de trocar a senha ou desativar o usuário | D-11 | [AUTHENTICATION.md](AUTHENTICATION.md#10-limitações-conhecidas) |
| Envio de e-mail síncrono | A requisição espera o servidor SMTP | D-15 | [ACCOUNT-FLOWS.md](ACCOUNT-FLOWS.md#9-limitações-conhecidas) |
| Falha de envio só no log | O cliente recebe sucesso mesmo quando o e-mail não foi enviado | D-15 | [ACCOUNT-FLOWS.md](ACCOUNT-FLOWS.md#9-limitações-conhecidas) |
| Cadastro público com `ROLE_OPERATOR` | Qualquer conta ativada pode alterar o catálogo | D-14 | [ACCOUNT-FLOWS.md](ACCOUNT-FLOWS.md#2-cadastro-ativação-e-login) |
| `INNER JOIN` nas consultas nativas | Produto sem categoria não é listado; usuário sem role não faz login | D-05 | [DATA-ACCESS.md](DATA-ACCESS.md#8-limitações-conhecidas) |
| Projection com só id e nome | A listagem de produtos só ordena por `id` e `name` | D-05 | [DATA-ACCESS.md](DATA-ACCESS.md#8-limitações-conhecidas) |
| E-mail comparado com `=` no login | `Maria@gmail.com` não encontra `maria@gmail.com` | D-05 | [DATA-ACCESS.md](DATA-ACCESS.md#8-limitações-conhecidas) |
| `Page` serializado diretamente | O formato do JSON paginado não é garantido entre versões do Spring Data | — | [DATA-ACCESS.md](DATA-ACCESS.md#8-limitações-conhecidas) |
| Migrations não testadas | Um erro numa migration só aparece em `dev` ou `prod` | D-09 | [TESTING.md](TESTING.md#9-limitações-conhecidas) |
| Validação de e-mail dependente de DNS | Sem internet, e-mails válidos são recusados e testes falham | D-18 | [VALIDATION.md](VALIDATION.md#7-limitações-conhecidas) |
| Preço em `Double` | Nem todo valor decimal é representado de forma exata | — | [DOMAIN-MODEL.md](DOMAIN-MODEL.md#9-limitações-conhecidas) |
| `equals` e `hashCode` por id | Duas entidades ainda não salvas são consideradas iguais | — | [DOMAIN-MODEL.md](DOMAIN-MODEL.md#9-limitações-conhecidas) |
| Segredos padrão versionados | Os valores de desenvolvimento ficam públicos no repositório; em `prod`, as variáveis são obrigatórias | D-08 | [CONFIGURATION.md](CONFIGURATION.md#7-limitações-conhecidas) |

---

⬅️ Anterior: [Convenções](CONVENTIONS.md) · [🏠 Índice](../HOME.md)
