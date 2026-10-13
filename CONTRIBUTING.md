# 🤝 Contribuindo para o Projeto

Antes de tudo, obrigado pelo interesse em contribuir com o **ASJCatalog** 🚀

Este projeto é open source e sem fins comerciais. Ele nasceu da base do DSCatalog, do curso DevSuperior, e evoluiu de forma independente como um laboratório de aprendizado em desenvolvimento backend com Java e Spring Boot.

A proposta não é apenas compartilhar código, mas também incentivar aprendizado colaborativo, troca de conhecimento e construção de um ambiente saudável para desenvolvedores em evolução. **Se você está começando, este guia foi escrito pensando em você.**

---

# 🎯 Objetivo das Contribuições

As contribuições devem ajudar a:

- Melhorar qualidade de código
- Evoluir arquitetura e organização
- Corrigir bugs
- Aprimorar documentação
- Compartilhar boas práticas
- Tornar os projetos mais didáticos
- Simular padrões utilizados no mercado

---

# 🌿 Como as Branches Funcionam

| Branch | Para que serve | Recebe Pull Request? |
|---|---|---|
| `develop` | Código atual e completo do projeto, com a documentação em `docs/` | ✅ **Sim, sempre** |
| `main` | Página de entrada do repositório. Só recebe o código no deploy final | ❌ Não |
| `chapter-*` | Registro de cada capítulo, como foi publicado | ❌ Não |

Capítulos publicados:

| Branch | Tema |
|---|---|
| `chapter-01-crud` | Operações CRUD com Spring Boot |
| `chapter-02-tests` | Testes automatizados |
| `chapter-03-validation-security` | Validação e segurança |
| `chapter-04-domain-orm` | Domínio, ORM, casos de uso e acesso a dados |

Ainda faltam dois capítulos para encerrar a evolução do projeto.

---

# 🔎 Escolhendo uma Issue

As melhorias pendentes estão registradas como [issues](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/issues). Cada issue já traz três partes:

- **Contexto**: o que acontece hoje e onde;
- **O que fazer**: o caminho sugerido;
- **Critérios de aceite**: o que precisa estar pronto para a issue ser considerada resolvida.

## Labels

| Label | Significado |
|---|---|
| `good first issue` | Boa para quem está começando no projeto. **Comece por aqui.** |
| `bug` | Algo não funciona como deveria |
| `security` | Vulnerabilidade ou regra de acesso |
| `tech-debt` | Dívida técnica e qualidade de código |
| `tests` | Testes automatizados |
| `i18n` | Internacionalização e mensagens |
| `documentation` | Melhorias ou acréscimos na documentação |
| `enhancement` | Melhoria ou funcionalidade nova |
| `production-readiness` | Necessário para rodar em produção |
| `help wanted` | Precisa de atenção extra |

## Milestone

O milestone [Pronto para produção](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/milestone/1) reúne as issues que precisam ser resolvidas antes do deploy. São as prioridades do projeto.

## Antes de começar

**Comente na issue dizendo que vai trabalhar nela.** Isso evita que duas pessoas façam a mesma coisa. Se tiver dúvida sobre o caminho, pergunte ali mesmo.

Encontrou um problema que ainda não tem issue? Abra uma nova usando os modelos de **Bug** ou **Melhoria**.

---

# 🚀 Como Contribuir

## 1. Faça um Fork

Crie um fork deste repositório para sua conta GitHub.

> [!IMPORTANT]
> Na tela de criação do fork, **desmarque a opção "Copy the `main` branch only"**. A `main` não tem o código atual, e você vai precisar da `develop`.

Se o fork já foi criado só com a `main`, tudo bem: depois do clone (passo 2), traga a `develop` do repositório original:

```bash
git remote add upstream https://github.com/Albertinesilva/backend-engineering-journey-java-springboot.git
git fetch upstream develop
git checkout -b develop upstream/develop
```

---

## 2. Clone o Projeto

```bash
git clone https://github.com/SEU-USUARIO/backend-engineering-journey-java-springboot.git
cd backend-engineering-journey-java-springboot
```

---

## 3. Crie uma Branch a partir da `develop`

Sempre parta da `develop` atualizada:

```bash
git checkout develop
git pull
git checkout -b fix/3-sort-price-produtos
```

Utilize nomes claros e descritivos. Se a branch resolve uma issue, coloque o número dela no nome.

Exemplos:

```bash
feature/improve-validation
fix/category-endpoint
docs/update-readme
test/product-service-tests
```

---

## 4. Rode o Projeto

Você vai precisar do **JDK 17**, do **PostgreSQL** e de algumas variáveis de ambiente. Não é preciso instalar o Maven: o projeto traz o Maven Wrapper (`mvnw`).

O passo a passo completo, do banco de dados até a primeira requisição autenticada, está no guia [Primeiros Passos (GETTING-STARTED)](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/develop/docs/guides/GETTING-STARTED.md).

---

## 5. Realize as Alterações

Boas práticas esperadas:

- Código limpo e organizado
- Responsabilidade única
- Nomenclatura clara
- Baixo acoplamento
- Coesão entre camadas
- Seguir arquitetura existente
- Ao mudar um comportamento, atualizar o JavaDoc e o guia correspondente em `docs/guides/`

---

# 🧪 Testes

Sempre que possível:

- Adicione testes automatizados
- Garanta que os testes existentes continuem funcionando
- Evite quebrar contratos da API

Os comandos rodam dentro da pasta `backend`:

```bash
cd backend
./mvnw test     # só os testes de unidade
./mvnw verify   # testes de unidade + testes de integração (*IT)
```

No PowerShell, use `.\mvnw test` e `.\mvnw verify`.

**Antes de abrir o Pull Request, rode `./mvnw verify`.** Os testes usam o banco H2 em memória e não precisam do PostgreSQL nem de servidor de e-mail.

Como os testes estão organizados e como rodar uma classe só: guia [Testes (TESTING)](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/develop/docs/guides/TESTING.md).

---

# 📌 Padrões Utilizados

## Arquitetura

O projeto segue arquitetura em camadas:

```text
Controller → Service → Repository
```

---

## DTOs

- Evitar exposição direta de entidades
- Utilizar DTOs para requests/responses
- Manter separação clara entre domínio e API

---

## Tratamento de Exceções

- Utilizar exceções customizadas
- Padronizar respostas HTTP
- Centralizar tratamento no Exception Handler

Todas as convenções do projeto (pacotes, nomes de classes, idioma, JavaDoc, testes e migrations) estão no guia [Convenções (CONVENTIONS)](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/develop/docs/guides/CONVENTIONS.md).

---

## Commits

Prefira commits pequenos e objetivos, no formato:

```text
tipo(escopo): descrição
```

- **tipo**: a natureza da mudança (tabela abaixo);
- **escopo**: opcional, indica a área afetada (já usados: `i18n`, `flyway`, `config`);
- **descrição**: em português, em letra minúscula, no presente e na terceira pessoa (`adiciona`, `corrige`, `remove`), sem ponto final.

| Tipo | Uso | Exemplo real do histórico |
|---|---|---|
| `feat` | Funcionalidade nova | `feat(i18n): testa idioma por Accept-Language, remove config de locale sem efeito e atualiza banner` |
| `fix` | Correção | `fix(flyway): move roles para a pasta reference e aplica em todos os perfis` |
| `refactor` | Mudança de código sem mudar comportamento | `refactor: renomeia o pacote base de dscatalog para asjcatalog` |
| `test` | Testes | `test: amplia a cobertura com ITs de conta, autorização, CORS e consultas nativas` |
| `docs` | Documentação | `docs: adiciona o guia de decisões técnicas e a origem do pacote domain` |
| `build` | Build e dependências | `build: adiciona o maven-failsafe-plugin para executar os testes *IT` |

Outros exemplos:

```text
feat: adiciona validação de categoria
fix: corrige paginação de produtos
test: adiciona testes do ProductService
docs: atualiza README do capítulo 02
```

---

# 📥 Pull Requests

> [!IMPORTANT]
> **Abra o Pull Request sempre para a branch `develop`, nunca para a `main`.** Na tela do PR, confira se o campo **base** está como `develop`. A `main` só recebe o código no deploy final.

Antes de abrir um Pull Request:

- Verifique se o projeto compila
- Execute os testes com `./mvnw verify`
- Revise o código
- Confira os critérios de aceite da issue
- Atualize documentação se necessário

Depois de abrir o Pull Request:

- Confira na aba **Commits** do PR se os seus commits aparecem. Se a lista estiver vazia ou só mostrar commits de outras pessoas, faltou o `git push` da sua branch.
- O CI roda o `./mvnw verify` automaticamente no GitHub. Na sua primeira contribuição, o mantenedor precisa liberar a execução, então pode levar um tempinho até os testes começarem.
- O merge só acontece com o CI passando e depois da revisão do mantenedor.

Na descrição do PR, cite a issue com `Refs #número` (por exemplo, `Refs #3`). O modelo de PR já traz esse campo.

## Fechamento das issues

O GitHub só fecha uma issue automaticamente (com palavras como `Closes #3` ou `Fixes #3`) quando o Pull Request entra na **branch padrão** do repositório, que aqui é a `main`. Como os PRs entram na `develop`, **a issue não fecha sozinha**: ela será fechada manualmente pelo mantenedor depois que o PR for aceito na `develop`.

---

# 💬 Discussões e Sugestões

Sugestões de melhoria são muito bem-vindas. Abra uma issue para conversar sobre:

- Organização de projeto
- Arquitetura
- Testes
- Performance
- Segurança
- Documentação
- DevOps
- Observabilidade

---

# 🎓 Filosofia do Projeto

Este repositório prioriza:

- Aprendizado contínuo
- Evolução incremental
- Clareza arquitetural
- Fundamentos sólidos
- Qualidade de código
- Engenharia de software aplicada

Mais importante do que apenas “fazer funcionar” é entender profundamente o que está sendo construído.

Ao participar, siga o [Código de Conduta](CODE_OF_CONDUCT.md).

---

# 🚀 Obrigado por contribuir

Toda contribuição — seja código, documentação, correção ou sugestão — ajuda a fortalecer este projeto e a jornada de aprendizado compartilhada aqui.
