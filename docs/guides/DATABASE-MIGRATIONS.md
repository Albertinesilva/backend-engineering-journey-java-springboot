# Migrations do banco de dados

Este guia explica como a estrutura e os dados de exemplo do banco são criados no capítulo 02: as migrations do Flyway, o `import.sql` usado pelo perfil `test` e pelos testes, e o `create.sql`.

## Sumário

1. [O que é uma migration](#1-o-que-é-uma-migration)
2. [Nunca altere uma migration já aplicada](#2-nunca-altere-uma-migration-já-aplicada)
3. [Pastas e versões](#3-pastas-e-versões)
4. [O que roda em cada perfil](#4-o-que-roda-em-cada-perfil)
5. [O import.sql do perfil test](#5-o-importsql-do-perfil-test)
6. [O arquivo create.sql](#6-o-arquivo-createsql)
7. [Recriar o banco de dev do zero](#7-recriar-o-banco-de-dev-do-zero)
8. [Limitações conhecidas](#8-limitações-conhecidas)

## 1. O que é uma migration

Uma **migration** é um script SQL versionado que muda o banco: cria uma tabela, adiciona uma restrição, insere dados. O **Flyway** aplica as migrations em ordem de versão ao subir a aplicação e anota as que já aplicou numa tabela própria do banco, a `flyway_schema_history`. Na subida seguinte, ele aplica só as migrations novas.

O nome do arquivo segue o padrão do Flyway. Em `V001__create_table_category.sql`:

- `V` indica uma migration versionada;
- `001` é a versão;
- `__` (dois sublinhados) separa a versão da descrição;
- `create_table_category` é a descrição.

## 2. Nunca altere uma migration já aplicada

Para cada migration aplicada, o Flyway grava na `flyway_schema_history` um **checksum**: um número calculado a partir do conteúdo do arquivo. A cada subida, ele recalcula o checksum dos arquivos e compara com o gravado. Se um arquivo já aplicado foi editado, os números não batem e a subida falha.

Por isso, uma mudança no banco deve ser feita numa migration nova. Editar uma migration aplicada só é aceitável num banco local que você vai recriar do zero (seção 7).

## 3. Pastas e versões

As migrations ficam em `backend/src/main/resources/db/migration`, em duas pastas:

| Pasta | Conteúdo |
| --- | --- |
| `schema` | Estrutura do banco: tabelas e chaves |
| `data` | Dados de exemplo |

| Versão | Pasta | Arquivo | O que faz |
| --- | --- | --- | --- |
| V001 | `schema` | `create_table_category` | Cria `tb_category` |
| V002 | `schema` | `create_table_product` | Cria `tb_product` |
| V003 | `schema` | `create_table_product_category` | Cria a tabela de junção `tb_product_category`, com chave primária `(product_id, category_id)` |
| V004 | `schema` | `alter_table_product_category_add_fk_category` | Adiciona as duas chaves estrangeiras de `tb_product_category`: para `tb_category` e para `tb_product` |
| V100 | `data` | `insert_categories` | 15 categorias |
| V101 | `data` | `insert_products` | 25 produtos |
| V102 | `data` | `insert_product_category` | 37 vínculos entre produtos e categorias |

Uma **chave estrangeira** (*foreign key*) é a restrição que obriga um id a existir na tabela referenciada. É ela que impede apagar uma categoria que ainda tem produtos (resposta 409).

O Flyway junta os arquivos das pastas configuradas e os ordena só pela versão: primeiro V001 a V004, depois V100 a V102.

Os vínculos da V102 usam ids fixos (por exemplo `(1, 1)` liga o produto 1 à categoria 1). Eles dependem de categorias e produtos receberem os ids 1, 2, 3... na ordem de inserção.

## 4. O que roda em cada perfil

| Perfil | Flyway | Resultado |
| --- | --- | --- |
| `test` (padrão) | Desligado (`spring.flyway.enabled=false`) | O Hibernate cria as tabelas e o `import.sql` insere os dados (seção 5) |
| `dev` | Ligado: `spring.flyway.locations=classpath:db/migration/schema,classpath:db/migration/data` | Estrutura e dados de exemplo no PostgreSQL |
| `prod` | Não configurado | Veja Limitações conhecidas |

## 5. O import.sql do perfil test

O perfil `test` usa o banco **H2 em memória**, criado vazio a cada subida, e desliga o Flyway. Nesse caso, o Hibernate cria as tabelas a partir das entidades e, logo depois, executa o arquivo `backend/src/main/resources/import.sql`.

Os 77 comandos `INSERT` do `import.sql` são os mesmos das migrations da pasta `data`: 15 categorias, 25 produtos e 37 vínculos. Assim, o perfil `test` tem os mesmos dados do perfil `dev`.

O `import.sql` só é executado quando o Hibernate cria as tabelas. No perfil `dev` (`ddl-auto=none`), ele é ignorado.

**Os testes dependem desses dados.** Como os testes rodam no perfil `test`, o `import.sql` é executado tanto nos testes de integração (`@SpringBootTest`) quanto nos testes de repositório (`@DataJpaTest`). Os testes de integração usam ids, totais e a ordem alfabética desses registros, por meio das constantes das factories (`EXISTING_ID`, `DEPENDENT_ID`, `COUNT_TOTAL_CATEGORIES`...). Mudar o `import.sql` pode quebrar esses testes. Veja [TESTING.md](TESTING.md#10-factories-e-dados-de-exemplo).

## 6. O arquivo create.sql

O arquivo `backend/create.sql` fica fora da pasta `src` e **não é lido pela aplicação**. Ele contém:

- o SQL de criação das tabelas no formato gerado pelo Hibernate a partir das entidades;
- os mesmos 77 comandos `INSERT` de dados de exemplo.

As propriedades que geram esse tipo de arquivo (`jakarta.persistence.schema-generation.*`, com `create-target=create.sql`) estão comentadas no `application-test.properties`.

## 7. Recriar o banco de dev do zero

Útil depois de editar uma migration localmente, ou ao trocar de branch. **Apaga todos os dados do banco `asjcatalog`.**

> O banco `asjcatalog` tem o mesmo nome nos outros capítulos, mas a estrutura é diferente em cada um. Se ele foi criado por outra branch, o Flyway deste capítulo encontra um histórico de migrations que não corresponde aos arquivos daqui. Recrie o banco vazio antes de subir no perfil `dev`.

1. Pare a aplicação. O PostgreSQL não apaga um banco com conexões abertas.
2. Apague e crie o banco (o comando é igual no PowerShell e no bash):

   ```powershell
   psql -U postgres -c "DROP DATABASE asjcatalog;"
   psql -U postgres -c "CREATE DATABASE asjcatalog;"
   ```

3. Suba a aplicação no perfil `dev` (veja [GETTING-STARTED.md](GETTING-STARTED.md#4-caminho-completo-perfil-dev-com-postgresql)). O Flyway encontra o banco vazio e aplica todas as migrations, da V001 à V102.

## 8. Limitações conhecidas

- **Acentos corrompidos no perfil `test`.** O `import.sql` está em UTF-8, mas o `application-test.properties` não informa ao Hibernate a codificação do arquivo. Confirmado por execução neste ambiente (Windows, JDK 17, em que a codificação padrão da JVM é `Cp1252`): as descrições em português aparecem trocadas, como `ClÃ¡ssico da literatura de fantasia` em vez de `Clássico da literatura de fantasia`. Nas migrations do Flyway, os acentos aparecem corretos. A propriedade `spring.jpa.properties.hibernate.hbm2ddl.charset_name=UTF-8`, que resolve isso, chega no capítulo 04. Nenhum teste confere as descrições acentuadas, então a suíte passa mesmo assim.
- **Perfil `prod` aplica os dados de exemplo.** Sem configuração própria de Flyway, o perfil `prod` usa o local padrão `db/migration`, que inclui as subpastas `schema` e `data` (confirmado por execução, num banco H2 criado automaticamente: a listagem de produtos devolveu os 25 produtos). A separação que impede os dados de exemplo em produção chega no capítulo 04.
- **O `import.sql` é mantido à mão.** Nada garante que ele continue igual às migrations da pasta `data`; ao mudar os dados de exemplo, atualize os dois.
- **O `create.sql` pode ficar desatualizado.** Ele não é gerado automaticamente a cada build.
- **Flyway não roda com o perfil `test`.** Um erro de SQL numa migration só aparece ao subir no perfil `dev`; nenhum teste executa as migrations.
