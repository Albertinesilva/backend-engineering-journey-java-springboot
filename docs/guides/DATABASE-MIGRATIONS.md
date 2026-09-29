# Migrations do banco de dados

Este guia explica como a estrutura e os dados iniciais do banco são criados e versionados com o Flyway, e como criar uma migration nova.

## Sumário

1. [O que é uma migration](#1-o-que-é-uma-migration)
2. [Nunca altere uma migration já aplicada](#2-nunca-altere-uma-migration-já-aplicada)
3. [Pastas e versões](#3-pastas-e-versões)
4. [O que roda em cada perfil](#4-o-que-roda-em-cada-perfil)
5. [O import.sql do perfil test](#5-o-importsql-do-perfil-test)
6. [Criar uma migration nova](#6-criar-uma-migration-nova)
7. [Recriar o banco de dev do zero](#7-recriar-o-banco-de-dev-do-zero)
8. [Limitações conhecidas](#8-limitações-conhecidas)

## 1. O que é uma migration

Uma **migration** é um script SQL versionado que muda o banco: cria uma tabela, adiciona uma coluna, insere dados. O **Flyway** aplica as migrations em ordem de versão ao subir a aplicação e anota as que já aplicou numa tabela própria do banco, a `flyway_schema_history`. Na subida seguinte, ele aplica só as migrations novas.

Assim, qualquer pessoa que baixa o projeto chega ao mesmo banco, e cada mudança fica registrada no Git.

O nome do arquivo segue o padrão do Flyway. Em `V104__insert_role.sql`:

- `V` indica uma migration versionada;
- `104` é a versão;
- `__` (dois sublinhados) separa a versão da descrição;
- `insert_role` é a descrição, com palavras separadas por `_`.

## 2. Nunca altere uma migration já aplicada

Para cada migration aplicada, o Flyway grava na `flyway_schema_history` um **checksum**: um número calculado a partir do conteúdo do arquivo. A cada subida, ele recalcula o checksum dos arquivos e compara com o gravado.

Se você editar um arquivo que já foi aplicado, a subida falha com uma mensagem como esta:

```text
Validate failed: Migrations have failed validation
Migration checksum mismatch for migration version 001
```

Para corrigir ou mudar algo, **crie uma migration nova** (seção 6). Editar uma migration aplicada só é aceitável num banco local que você vai recriar do zero (seção 7).

## 3. Pastas e versões

As migrations ficam em `backend/src/main/resources/db/migration`, em três pastas:

| Pasta | Conteúdo | Arquivos |
| --- | --- | --- |
| `schema` | Estrutura do banco: tabelas, chaves e restrições | `V001` a `V011` |
| `reference` | Dados de referência, necessários em todos os ambientes | `V104__insert_role.sql` (roles `ROLE_OPERATOR` e `ROLE_ADMIN`) |
| `data` | Dados de exemplo para desenvolvimento | `V100` a `V103` e `V105` |

Arquivos de `schema`:

| Versão | Arquivo | O que faz |
| --- | --- | --- |
| V001 | `create_table_category` | Cria `tb_category` |
| V002 | `create_table_product` | Cria `tb_product` |
| V003 | `create_table_product_category` | Cria a tabela de junção `tb_product_category` |
| V004 | `alter_table_product_category` | Adiciona as chaves estrangeiras de `tb_product_category` |
| V005 | `create_table_role` | Cria `tb_role` |
| V006 | `create_table_user` | Cria `tb_user` |
| V007 | `create_table_user_role` | Cria a tabela de junção `tb_user_role` |
| V008 | `alter_table_user_role` | Adiciona as chaves estrangeiras de `tb_user_role` |
| V009 | `create_table_token` | Cria `tb_token` |
| V010 | `alter_table_token` | Adiciona a chave estrangeira de `tb_token` para `tb_user` |
| V011 | `create_table_email` | Cria `tb_email` |

Arquivos de `reference` e `data`:

| Versão | Pasta | Arquivo | Conteúdo |
| --- | --- | --- | --- |
| V100 | `data` | `insert_categories` | 44 categorias |
| V101 | `data` | `insert_products` | 163 produtos |
| V102 | `data` | `insert_product_category` | 226 vínculos entre produtos e categorias |
| V103 | `data` | `insert_user` | 2 usuários de exemplo |
| V104 | `reference` | `insert_role` | 2 roles |
| V105 | `data` | `insert_user_role` | 3 vínculos entre usuários e roles |

**A versão é global, não por pasta.** O Flyway junta os arquivos de todas as pastas configuradas e os ordena só pela versão. Por isso a V104, de `reference`, roda entre a V103 e a V105, de `data`, e duas pastas não podem ter a mesma versão.

Os vínculos da V102 e da V105 usam ids fixos (por exemplo `user_id = 1`). Eles dependem de os registros anteriores receberem os ids 1, 2, 3... na ordem de inserção.

## 4. O que roda em cada perfil

| Perfil | Flyway | Pastas aplicadas | Resultado |
| --- | --- | --- | --- |
| `dev` | Ligado | `schema`, `reference`, `data` | Estrutura, roles e dados de exemplo |
| `prod` | Ligado | `schema`, `reference` | Estrutura e roles. **Sem** usuários nem produtos de exemplo |
| `test` | Desligado | Nenhuma | O Hibernate cria as tabelas e o `import.sql` insere os dados (seção 5) |

As pastas de cada perfil estão na propriedade `spring.flyway.locations` de `application-dev.properties` e `application-prod.properties`. No perfil `prod`, o Hibernate usa `ddl-auto=validate`: se uma entidade não bater com as tabelas criadas pelas migrations, a aplicação não sobe.

## 5. O import.sql do perfil test

O perfil `test` usa o banco **H2 em memória**, criado vazio a cada subida, e desliga o Flyway. Nesse caso, o Hibernate cria as tabelas a partir das entidades e, logo depois, executa o arquivo `backend/src/main/resources/import.sql`, se ele existir. É assim que os testes de integração encontram dados no banco.

Hoje, os 440 comandos `INSERT` do `import.sql` são os mesmos das pastas `data` e `reference`, em outra ordem: usuários e roles vêm primeiro. Assim, o perfil `test` tem os mesmos usuários, categorias e produtos do perfil `dev`.

O `import.sql` só é executado quando o Hibernate cria as tabelas. Nos perfis `dev` (`ddl-auto=none`) e `prod` (`ddl-auto=validate`), ele é ignorado.

## 6. Criar uma migration nova

1. **Escolha a pasta.**
   - Mudou a estrutura (tabela, coluna, índice, restrição)? Use `schema`.
   - São dados que precisam existir em todos os ambientes, inclusive produção? Use `reference`.
   - São dados de exemplo para desenvolvimento? Use `data`.
2. **Escolha a versão.** Use uma versão **maior que a maior já existente em qualquer pasta**. Hoje a maior é V105, então a próxima é **V106**, qualquer que seja a pasta. Veja em [Limitações conhecidas](#8-limitações-conhecidas) por que não dá para usar V012 em `schema`.
3. **Crie o arquivo** com o nome no padrão, por exemplo `schema/V106__add_column_stock_to_product.sql`.
4. **Escreva o SQL para PostgreSQL**, que é o banco de `dev` e `prod`.
5. **Atualize as entidades** se a estrutura mudou. O perfil `prod` valida as entidades contra as tabelas, e o perfil `test` cria as tabelas a partir delas.
6. **Atualize o `import.sql`** se a migration insere dados que também devem existir nos testes.
7. **Suba a aplicação no perfil `dev`.** O log do Flyway, em nível `DEBUG` nesse perfil, mostra a migration aplicada. Rode os testes (`./mvnw verify`).
8. **Depois de aplicada, não edite mais o arquivo** (seção 2).

## 7. Recriar o banco de dev do zero

Útil depois de editar uma migration localmente ou quando o banco ficou num estado inconsistente. **Apaga todos os dados do banco `asjcatalog`.**

1. Pare a aplicação. O PostgreSQL não apaga um banco com conexões abertas.
2. Apague e crie o banco (o comando é igual no PowerShell e no bash):

   ```powershell
   psql -U postgres -c "DROP DATABASE asjcatalog;"
   psql -U postgres -c "CREATE DATABASE asjcatalog;"
   ```

3. Suba a aplicação no perfil `dev`. O Flyway encontra o banco vazio e aplica todas as migrations, da V001 à V105.

## 8. Limitações conhecidas

- **Migration nova de `schema` com versão V012 bloqueia a subida.** Num banco que já aplicou a V105 (ou a V104, em prod), uma migration com versão menor fica fora de ordem. Na configuração atual (sem `outOfOrder`), o Flyway recusa a subida com `Detected resolved migration not applied to database: 012`. Por isso a faixa V0xx de `schema` não pode mais ser usada para migrations novas, e toda migration nova precisa de versão maior que V105.
- **O `import.sql` é mantido à mão.** Nada garante que ele continue igual às pastas `data` e `reference`; ao mudar os dados de exemplo, atualize os dois.
- **Os vínculos usam ids fixos.** A V102, a V105 e o `import.sql` supõem que categorias, produtos, usuários e roles recebem os ids na ordem de inserção.
- **Flyway desligado nos testes.** Os testes usam tabelas criadas pelo Hibernate a partir das entidades, e não pelas migrations. Um erro de SQL numa migration não é detectado pelos testes: aparece só ao subir nos perfis `dev` ou `prod`.
