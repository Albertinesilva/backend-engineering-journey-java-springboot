# Documentação do ASJCatalog

Esta pasta reúne a documentação técnica do backend do ASJCatalog. Ela está organizada em três partes:

- **Guias** (`guides/`): explicam como o projeto funciona e como trabalhar com ele. São o ponto de partida para quem está chegando ao projeto e são mantidos atualizados com o código.
- **Contrato de segurança** (`SECURITY-CONTRACT.md`): referência do comportamento atual de autenticação e autorização. Deve ser consultado antes de qualquer alteração em segurança.
- **Auditorias** (`audits/`): registros das análises feitas no código em datas específicas. Servem como histórico e podem não refletir o código atual.

O README da raiz do repositório apresenta o capítulo atual do projeto. Esta pasta documenta o projeto como um todo.

## Guias

| Guia | Conteúdo | Situação |
| --- | --- | --- |
| [GETTING-STARTED](guides/GETTING-STARTED.md) | Como preparar o ambiente, rodar a aplicação localmente e fazer a primeira requisição. | Disponível |
| [CONFIGURATION](guides/CONFIGURATION.md) | Perfis `dev`, `test` e `prod` e todas as variáveis de ambiente. | Disponível |
| [ARCHITECTURE](guides/ARCHITECTURE.md) | Camadas, organização dos pacotes e caminho de uma requisição pela aplicação. | Disponível |
| [DOMAIN-MODEL](guides/DOMAIN-MODEL.md) | Entidades do domínio, seus relacionamentos e as regras que ficam dentro delas. | Disponível |
| [DATA-ACCESS](guides/DATA-ACCESS.md) | Repositórios, consultas (derivadas, JPQL e nativas), paginação e a solução do problema N+1. | Disponível |
| [DATABASE-MIGRATIONS](guides/DATABASE-MIGRATIONS.md) | Migrations do Flyway, pastas `schema`, `reference` e `data` e o que roda em cada perfil. | Disponível |
| [API-ENDPOINTS](guides/API-ENDPOINTS.md) | Todos os endpoints da API, com permissões exigidas, corpo da requisição e respostas. | Disponível |
| [AUTHENTICATION](guides/AUTHENTICATION.md) | Como obter e renovar tokens, como funcionam as roles e como chamar rotas protegidas. | Disponível |
| [ACCOUNT-FLOWS](guides/ACCOUNT-FLOWS.md) | Cadastro, ativação de conta, recuperação e troca de senha e envio de e-mails. | Disponível |
| [VALIDATION](guides/VALIDATION.md) | Regras de validação dos dados de entrada e validadores customizados. | Disponível |
| [ERROR-HANDLING](guides/ERROR-HANDLING.md) | Formato das respostas de erro, códigos `ApiErrorCode` e status HTTP. | Disponível |
| [INTERNATIONALIZATION](guides/INTERNATIONALIZATION.md) | Mensagens da API em português, inglês e espanhol, escolhidas pelo cabeçalho `Accept-Language`. | Disponível |
| [TESTING](guides/TESTING.md) | Testes de unidade e de integração, como rodá-los e como estão organizados. | Disponível |
| [CONVENTIONS](guides/CONVENTIONS.md) | Convenções de código, de commits, de branches e de numeração de migrations. | Disponível |

## Contrato de segurança

- [SECURITY-CONTRACT.md](SECURITY-CONTRACT.md): comportamento atual do OAuth2, do JWT, do refresh token, das cadeias de filtros e do CORS, com os pontos que não devem ser alterados sem análise.

## Histórico de auditorias

Registros de auditorias anteriores do código. Cada arquivo traz a data em que foi feito e pode estar desatualizado; a documentação atual está nos guias.

| Arquivo | Conteúdo |
| --- | --- |
| [B-0-BACKEND-INVENTORY.md](audits/B-0-BACKEND-INVENTORY.md) | Inventário inicial do backend: versões, pacotes, classes, fluxos e lista de achados. |
| [B-1-DOMAIN-LAYER.md](audits/B-1-DOMAIN-LAYER.md) | Análise da camada de domínio: entidades, invariantes e relacionamentos. |
| [B-2-REPOSITORY-LAYER.md](audits/B-2-REPOSITORY-LAYER.md) | Análise dos repositórios e projections: consultas, paginação e desempenho. |
| [B-3-SERVICE-LAYER.md](audits/B-3-SERVICE-LAYER.md) | Análise dos services: fluxos de negócio, transações e exceções. |
| [B-4-DTO-MAPPER-LAYER.md](audits/B-4-DTO-MAPPER-LAYER.md) | Análise dos DTOs e mappers: conversões, atualizações parciais e dados sensíveis. |
| [B-5-VALIDATION-LAYER.md](audits/B-5-VALIDATION-LAYER.md) | Análise da camada de validação: o que cada validador realmente impõe. |
| [B-6-SECURITY-OAUTH2-LAYER.md](audits/B-6-SECURITY-OAUTH2-LAYER.md) | Análise da segurança: password grant, JWT, refresh token e cadeias de filtros. |
| [B-7-WEB-LAYER.md](audits/B-7-WEB-LAYER.md) | Análise da camada web: mapa de endpoints, status HTTP e tratamento de erros. |
| [B-8-CONFIG-INFRASTRUCTURE.md](audits/B-8-CONFIG-INFRASTRUCTURE.md) | Análise da configuração: perfis, banco, Flyway, e-mail, logging e variáveis de ambiente. |
| [B-9-ARCHITECTURAL-REVIEW.md](audits/B-9-ARCHITECTURAL-REVIEW.md) | Revisão arquitetural que cruza as análises B-0 a B-8. |
| [UTIL-LAYER.md](audits/UTIL-LAYER.md) | Análise do pacote `util` (`IdentifiableUtils`). |
| [T-0-TEST-BASELINE.md](audits/T-0-TEST-BASELINE.md) | Diagnóstico da suíte de testes e das falhas encontradas na época. |
| [T-1-TEST-SYNCHRONIZATION.md](audits/T-1-TEST-SYNCHRONIZATION.md) | Correção das falhas do T-0 e sincronização dos testes com o código. |
