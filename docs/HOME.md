# Documentação do ASJCatalog — Capítulo 03

Esta pasta documenta o backend do ASJCatalog **como ele está no capítulo 03** (branch `chapter-03-validation-security`): a API REST de categorias e produtos dos capítulos anteriores, agora com validação dos dados de entrada (Bean Validation e validadores próprios), usuários e roles, login com OAuth2 e tokens JWT, e controle de acesso por rota com Spring Security. O perfil padrão deste capítulo é `dev`, que usa PostgreSQL; o perfil `test` roda a aplicação e os testes com o banco H2 em memória. Os guias descrevem somente o código desta branch e são escritos para quem está começando. O `README.md` da raiz do repositório apresenta o capítulo.

## Guias

| Guia | Conteúdo |
| --- | --- |
| [VALIDATION](guides/VALIDATION.md) | Regras de validação por DTO, validadores customizados, senha forte, consulta MX de e-mail, mensagens e respostas 422. |
| [AUTHENTICATION](guides/AUTHENTICATION.md) | Login com OAuth2, conteúdo do JWT, cadeias de filtros, roles, 401 e 403, quem pode fazer o quê, client e CORS. |
| [GETTING-STARTED](guides/GETTING-STARTED.md) | Como rodar a aplicação localmente, com H2 ou PostgreSQL, obter um token e fazer as primeiras chamadas. |
| [CONFIGURATION](guides/CONFIGURATION.md) | Perfis `dev`, `test` e `prod`, propriedades de segurança, variáveis de ambiente e arquivos de log. |
| [ARCHITECTURE](guides/ARCHITECTURE.md) | Camadas, organização dos pacotes e caminho de uma requisição pela segurança e pela validação. |
| [DOMAIN-MODEL](guides/DOMAIN-MODEL.md) | Entidades `Category`, `Product`, `User` e `Role`, seus campos e relacionamentos. |
| [DATA-ACCESS](guides/DATA-ACCESS.md) | Repositórios, consultas derivadas, a consulta nativa do login, paginação e transações. |
| [DATABASE-MIGRATIONS](guides/DATABASE-MIGRATIONS.md) | Migrations do Flyway (V001 a V105), `import.sql` do perfil `test` e `create.sql`. |
| [API-ENDPOINTS](guides/API-ENDPOINTS.md) | Os 21 endpoints da API, com permissões, formatos e exemplos reais. |
| [ERROR-HANDLING](guides/ERROR-HANDLING.md) | Formato das respostas de erro (`ProblemDetails` e `ValidationError`), status HTTP e o 401 sem corpo. |
| [TESTING](guides/TESTING.md) | Como rodar os testes, inventário das classes, testes com usuário simulado e com token real. |
| [CONVENTIONS](guides/CONVENTIONS.md) | Convenções de nomes, validação, segurança, testes, idioma, JavaDoc, commits, branches e migrations. |

## O que chega nos capítulos seguintes

Este capítulo ainda não tem os recursos abaixo. Cada guia registra, em "Limitações conhecidas", o comportamento atual e o capítulo em que ele muda.

| Recurso | Capítulo |
| --- | --- |
| Refresh token, com rotação | 04 |
| Fluxos de conta (cadastro, ativação, recuperação de senha) e envio de e-mail | 04 |
| Internacionalização das mensagens | 04 |
| Testes de integração executados no `verify` (Failsafe) | 04 |
| Escolha do perfil por variável de ambiente (`APP_PROFILE`) | 04 |
| Código estável de erro (`ApiErrorCode`) | 04 |
