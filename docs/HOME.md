# Documentação do ASJCatalog — Capítulo 02

Esta pasta documenta o backend do ASJCatalog **como ele está no capítulo 02** (branch `chapter-02-tests`): a mesma API REST de categorias e produtos do capítulo 01, agora coberta por testes automatizados com JUnit 5, Mockito, MockMvc, `@DataJpaTest` e `@SpringBootTest`. Neste capítulo, o perfil padrão passou a ser `test`, e por isso a aplicação e a suíte de testes rodam sem PostgreSQL. Os guias descrevem somente o código desta branch e são escritos para quem está começando. O `README.md` da raiz do repositório apresenta o capítulo.

## Guias

| Guia | Conteúdo |
| --- | --- |
| [TESTING](guides/TESTING.md) | Como rodar os testes, a pirâmide, o inventário das classes, cada tipo de teste com trechos reais, factories, padrões de escrita e TDD. |
| [GETTING-STARTED](guides/GETTING-STARTED.md) | Como rodar a aplicação localmente, com H2 (sem dependências) ou com PostgreSQL, fazer as primeiras chamadas e rodar os testes. |
| [CONFIGURATION](guides/CONFIGURATION.md) | Perfis `test`, `dev` e `prod`, arquivos de configuração, arquivos de log e variáveis de ambiente. |
| [ARCHITECTURE](guides/ARCHITECTURE.md) | Camadas, organização dos pacotes e caminho de uma requisição pela aplicação. |
| [DOMAIN-MODEL](guides/DOMAIN-MODEL.md) | Entidades `Category` e `Product`, seus campos, construtores e o relacionamento entre elas. |
| [DATA-ACCESS](guides/DATA-ACCESS.md) | Repositórios, consultas derivadas, paginação e transações. |
| [DATABASE-MIGRATIONS](guides/DATABASE-MIGRATIONS.md) | Migrations do Flyway, `import.sql` do perfil `test` e dos testes, e `create.sql`. |
| [API-ENDPOINTS](guides/API-ENDPOINTS.md) | Os 14 endpoints da API, com exemplos reais de requisição e resposta. |
| [ERROR-HANDLING](guides/ERROR-HANDLING.md) | Formato das respostas de erro (`ProblemDetails`), `ErrorType` e status HTTP. |
| [CONVENTIONS](guides/CONVENTIONS.md) | Convenções de nomes, organização e nomes dos testes, idioma, JavaDoc, commits, branches e migrations. |

## O que chega nos capítulos seguintes

Este capítulo ainda não tem os recursos abaixo. Cada guia registra, em "Limitações conhecidas", o comportamento atual e o capítulo em que ele muda.

| Recurso | Capítulo |
| --- | --- |
| Validação dos dados de entrada | 03 |
| Segurança (autenticação e autorização) | 03 |
| Resposta 404 para rota inexistente | 03 |
| Testes de integração executados no `verify` (Failsafe) | 04 |
| Envio de e-mail | 04 |
| Internacionalização das mensagens | 04 |
