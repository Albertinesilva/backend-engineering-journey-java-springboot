# Documentação do ASJCatalog — Capítulo 01

Esta pasta documenta o backend do ASJCatalog **como ele está no capítulo 01** (branch `chapter-01-crud`): uma API REST de CRUD de categorias e produtos, organizada em camadas, com Spring Boot, Spring Data JPA, PostgreSQL, H2 e Flyway. Os guias descrevem somente o código desta branch e são escritos para quem está começando. O `README.md` da raiz do repositório apresenta o capítulo.

## Guias

| Guia | Conteúdo |
| --- | --- |
| [GETTING-STARTED](guides/GETTING-STARTED.md) | Como rodar a aplicação localmente, com H2 (sem dependências) ou com PostgreSQL, e fazer as primeiras chamadas. |
| [CONFIGURATION](guides/CONFIGURATION.md) | Perfis `dev`, `test` e `prod`, arquivos de configuração e variáveis de ambiente. |
| [ARCHITECTURE](guides/ARCHITECTURE.md) | Camadas, organização dos pacotes e caminho de uma requisição pela aplicação. |
| [DOMAIN-MODEL](guides/DOMAIN-MODEL.md) | Entidades `Category` e `Product`, seus campos e o relacionamento entre elas. |
| [DATA-ACCESS](guides/DATA-ACCESS.md) | Repositórios, consultas derivadas, paginação e transações. |
| [DATABASE-MIGRATIONS](guides/DATABASE-MIGRATIONS.md) | Migrations do Flyway, `import.sql` do perfil `test` e `create.sql`. |
| [API-ENDPOINTS](guides/API-ENDPOINTS.md) | Os 14 endpoints da API, com exemplos reais de requisição e resposta. |
| [ERROR-HANDLING](guides/ERROR-HANDLING.md) | Formato das respostas de erro (`ProblemDetails`), `ErrorType` e status HTTP. |
| [CONVENTIONS](guides/CONVENTIONS.md) | Convenções de nomes, idioma, JavaDoc, commits, branches e migrations. |

## O que chega nos capítulos seguintes

Este capítulo ainda não tem os recursos abaixo. Cada guia registra, em "Limitações conhecidas", o comportamento atual e o capítulo em que ele muda.

| Recurso | Capítulo |
| --- | --- |
| Testes automatizados | 02 |
| Validação dos dados de entrada | 03 |
| Segurança (autenticação e autorização) | 03 |
| Envio de e-mail | 04 |
| Internacionalização das mensagens | 04 |
