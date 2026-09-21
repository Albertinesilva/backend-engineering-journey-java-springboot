# ASJCatalog Backend — Package `util`

> **Etapa:** documentação complementar da produção (após B-9).
> **Alteração em `src/main`:** somente JavaDoc em `util/IdentifiableUtils.java`. Nenhuma linha executável, assinatura, import ou teste foi alterado.

Rótulos: **CONFIRMADO** (código ou referências encontradas), **INFERÊNCIA** (derivada, não executada), **HIPÓTESE** (exige teste/execução).

---

## 1. Objetivo
Conhecer e documentar o package `util`, o único package de produção ainda sem análise dedicada.

## 2. Escopo
`src/main/java/**/util/**`. Leitura auxiliar: `domain/Identifiable`, `domain/catalog/Product`, `projection/ProductProjection`, `service/ProductService`, testes (somente busca de referências) e os documentos B-0 a B-9.

## 3. Inventário do package `util` [CONFIRMADO]

| Item | Valor |
|------|-------|
| Arquivos | **1**: `util/IdentifiableUtils.java` |
| Classes | 1 (`public final class IdentifiableUtils`) |
| Interfaces / enums / records | 0 |
| Métodos | 1 público (`reorderByReference`); 0 privados |
| Campos / estado | nenhum |
| Construtores declarados | nenhum (vale o construtor padrão público) |
| Dependências | `java.util.{ArrayList, HashMap, List, Map}`; `domain.Identifiable` |

Não existem outras classes em `util`; nada mais foi documentado.

## 4. Classe e responsabilidade

**`IdentifiableUtils`** reordena uma lista de entidades segundo a ordem de uma segunda lista de referência, comparando **identificadores** (`Identifiable#getId`).

- **Por que está em `util`:** é uma função pura e genérica (`<ID, T extends Identifiable<ID>>`), sem relação com entidade, regra de negócio, persistência ou HTTP; depende só do contrato `Identifiable` (CONFIRMADO). O nome sugere utilitário geral para `Identifiable`, mas hoje há um único método e um único uso (§7).
- **Papel no backend:** restaurar a ordem de uma página depois de uma segunda consulta. Em `ProductService.findAllPaged`, a página vem de `searchProducts` (ordenada/paginada pelo banco); os produtos completos vêm de `searchProductsWithCategories` (`JOIN FETCH ... WHERE id IN :ids`, **sem `ORDER BY`**); o util recoloca os produtos na ordem da página (CONFIRMADO no código; a ordem de retorno da consulta `IN` não é garantida — INFERÊNCIA baseada em ausência de `ORDER BY`).
- **Responsabilidade que parece de outra camada:** a ordem poderia ser garantida na própria consulta JPQL (`ORDER BY`) ou pelo service; o util existe porque a paginação é feita em uma consulta e o carregamento das coleções em outra (B-2). Isto é observação de desenho, **não** correção proposta.

## 5. Métodos e comportamento

### `public static <ID, T extends Identifiable<ID>> List<T> reorderByReference(List<T> unordered, List<? extends Identifiable<ID>> ordered)`

| Aspecto | Comportamento |
|---------|---------------|
| Parâmetros | `unordered`: elementos a reordenar; `ordered`: lista de referência (pode ser de outro tipo, desde que `Identifiable<ID>`; no uso real é `List<ProductProjection>`) |
| Retorno | **nova** `ArrayList<T>` com os elementos de `unordered` na ordem de `ordered` |
| Algoritmo | 1) `HashMap<ID,T>` indexado por `getId()` de `unordered`; 2) percorre `ordered` e acrescenta `map.get(id)` quando existe |
| Complexidade | O(n + m) tempo e espaço |
| Exceções | `NullPointerException` se qualquer lista ou elemento for `null` (`for` sobre lista nula; `entity.getId()` / `identifiable.getId()` em elemento nulo). Nenhuma exceção é declarada nem tratada |
| Efeitos colaterais | nenhum: não altera as listas de entrada; não faz I/O nem log |
| Estado / thread-safety | sem estado; seguro para uso concorrente desde que as listas de entrada não sejam modificadas por outra thread durante a chamada |

Regras implícitas (todas CONFIRMADAS por leitura do código):
1. **Descarte silencioso:** id de `ordered` sem correspondente em `unordered` não entra no resultado; elemento de `unordered` cujo id não aparece em `ordered` também não entra. O resultado nunca tem mais elementos que `ordered` (cada ocorrência de `ordered` contribui com no máximo um elemento).
2. **Duplicatas em `unordered`:** o índice mantém o **último** elemento por id.
3. **Duplicatas em `ordered`:** o mesmo elemento aparece repetido no resultado, uma vez por ocorrência.
4. **Id `null`:** `HashMap` aceita chave `null`; elementos com id `null` são indexados e podem ser correspondidos entre si (relevante para entidades não persistidas; INFERÊNCIA sobre relevância prática — no uso real todos os ids vêm do banco).
5. **Igualdade de ids:** por `equals`/`hashCode` do tipo `ID` (`Long` no uso real).
6. **Ordem do resultado:** a de `ordered`, não a de `unordered`.
7. **Construtor:** a classe é `final`, mas **não** tem construtor privado; `new IdentifiableUtils()` compila (CONFIRMADO). Nenhum código faz isso.

## 6. Dependências [CONFIRMADO]

| Dependência | Uso |
|-------------|-----|
| `domain.Identifiable<ID>` | único contrato exigido (`ID getId()`); implementado por `Product` (`Identifiable<Long>`) e estendido por `ProductProjection` |
| JDK (`List`, `ArrayList`, `Map`, `HashMap`) | estruturas internas |
| Spring / Hibernate / JPA / Jakarta | **nenhuma** dependência direta; nenhuma anotação |

Comportamento dependente de infraestrutura: nenhum **no util**. Ele depende, indiretamente, de o Hibernate devolver instâncias com `getId()` já preenchido (o `JOIN FETCH` devolve entidades gerenciadas com id) e de a projeção nativa entregar `getId()` (alias da coluna `id`) (INFERÊNCIA — a projeção é resolvida pelo Spring Data; B-2).

## 7. Callers e uso no sistema

```text
IdentifiableUtils
   ↓
reorderByReference(List<T>, List<? extends Identifiable<ID>>)
   ↓
ProductService.findAllPaged(String name, String categoryId, Pageable pageable)   [ProductService.java, linha 200]
   ↓
products = IdentifiableUtils.reorderByReference(products /* List<Product> */, page.getContent() /* List<ProductProjection> */)
```

| Pergunta | Resposta |
|----------|----------|
| Callers em produção | **1**: `ProductService.findAllPaged` (CONFIRMADO por busca em `src/main`) |
| Caller acima do service | `ProductController.findAll` → `GET /api/v1/products` (público) |
| Chamada direta ou indireta | direta a partir de `ProductService`; indireta a partir do endpoint |
| Usado por testes diretamente | **não**: nenhuma referência a `IdentifiableUtils`/`reorderByReference` em `src/test` (0 ocorrências) |
| Exercitado indiretamente por testes | **sim, em alguma medida**: `ProductControllerIT` (`GET /products`, verde no baseline B-0) passa pelo endpoint real. `ProductServiceTest` mocka o repositório; `ProductServiceIT` chama `search` (não `findAllPaged`); `ProductControllerTest` mocka `search` (falha no baseline por causa disso — B-9 §16). Não há teste unitário do próprio util |
| Usado por múltiplas camadas | não: só service (com a projeção vinda do repository e a entidade do domain) |
| Métodos sem caller | nenhum (há um único método e ele tem um caller) |

Referências apenas em JavaDoc: `ProductProjection`, `ProductRepository`, `ProductService` (comentários).

## 8. Relação com as demais camadas e documentos anteriores

| Fase | O que dizia sobre `util` | Situação atual |
|------|--------------------------|----------------|
| B-0 §3/§4.3/§12.5 | "1 arquivo; utilitário de reordenação por referência; usado por `ProductService`"; "exercitada por `ProductService`; `ProductServiceTest` mocka o repositório" | **Confirmado.** Refinamento: o caller é somente `findAllPaged`; o único teste que atravessa o util com dados reais é `ProductControllerIT` (`ProductServiceIT` chama `search`); não há teste dedicado |
| B-1 | `Identifiable` existe para "reordenar uma lista de entidades segundo a ordem de outra lista" | **Confirmado**; o consumidor é este util |
| B-2 §10/§13 | `ProductProjection` estende `Identifiable` "por isso aceita `List<? extends Identifiable<ID>>`"; `reorderByReference` "indexa por id, duplicatas colapsam" | **Confirmado**, com um detalhe: quando há duplicatas em `unordered`, o **último** prevalece |
| B-3 §4.3 | passo 5 de `findAllPaged`: "indexa por id, percorre a ordem da página, descarta ids sem produto" | **Confirmado** |
| B-9 §12 | `IdentifiableUtils.reorderByReference` "descarta ids sem produto" | **Confirmado**; refinamento: também descarta produtos cujo id não está na página |

Conclusões anteriores não foram apagadas; nenhuma foi refutada.

## 9. Observações confirmadas [CONFIRMADO]
1. Package com **um** arquivo, **uma** classe, **um** método, **um** caller de produção.
2. A classe não tinha JavaDoc antes desta etapa (o método também não).
3. `final`, sem estado, sem construtor privado (instanciável).
4. Descarta elementos em ambos os sentidos; duplicatas: último em `unordered`, repetição em `ordered`.
5. NPE para listas ou elementos nulos; nenhuma validação/exceção própria.
6. Sem dependência de Spring/Hibernate/JPA; sem log; sem efeitos colaterais.
7. Não há teste unitário nem referência direta em `src/test`.
8. Nenhum uso fora de `ProductService.findAllPaged`.

## 10. Inferências [INFERÊNCIA]
- O util existe porque a consulta `IN` de `searchProductsWithCategories` não tem `ORDER BY` e, portanto, não preserva a ordem da página.
- Em `findAllPaged`, a lista `products` só perde elementos se a segunda consulta não retornar algum id da página (por exemplo, produto removido entre as duas consultas ou produto sem categoria — o `JOIN FETCH` é interno); nesse caso a página de resposta teria menos itens que `page.getContent()`, mas `totalElements` continuaria o da primeira consulta.
- Como todos os ids vêm de colunas `id` (não nulos), o caso de id `null` não deve ocorrer no fluxo real.
- Para páginas pequenas (padrão 20) o custo é desprezível.

## 11. Hipóteses [HIPÓTESE]
- Ordem efetiva devolvida pelo PostgreSQL para `IN` com `JOIN FETCH` (o util a mascara; sem ele a ordem poderia divergir — precisa de execução para saber se divergiria).
- Comportamento com página vazia: `findAllPaged` chama `searchProductsWithCategories` com lista vazia (B-3/B-9) e depois o util com duas listas vazias (retorna lista vazia, sem erro — este último passo é CONFIRMADO pelo código; a etapa anterior é a hipótese).
- Comportamento sob concorrência entre a consulta da página e a de carga (inconsistência entre as duas leituras).

## 12. Pontos para a futura fase de testes
- Teste unitário direto do util: ordem correta; ids ausentes em qualquer dos lados; duplicatas em cada lista; listas vazias; `null` (NPE); `ordered` de tipo diferente (projeção × entidade).
- Cobertura via `findAllPaged` com dados reais em H2 e, quando existir, PostgreSQL: ordenação por `name` asc/desc, página vazia, produtos com várias categorias.
- Registrar que `ProductControllerTest` mocka `search` e nunca chega ao util (B-9 §16).

## 13. JavaDoc adicionado

| Elemento | Conteúdo |
|----------|----------|
| Classe `IdentifiableUtils` | responsabilidade, motivo de existir (`findAllPaged`), estado (nenhum), ausência de construtor privado |
| Método `reorderByReference` | funcionamento, descarte silencioso, duplicatas, `null`, imutabilidade das entradas, thread-safety condicionada; `@param` (`unordered`, `ordered`, `<ID>`, `<T>`), `@return`, `@throws NullPointerException` |

`doclint` (`-Xdoclint:all,-missing`) sem avisos; o código, comparado ao HEAD sem comentários, é idêntico.

## 14. O que não foi feito
Nenhuma alteração de lógica, assinatura, visibilidade ou imports; não foi criado construtor privado nem teste; nenhum arquivo em `src/test/**`, `pom.xml`, `src/main/resources/**` ou outro package de produção alterado; nenhum teste executado; nenhum commit.
