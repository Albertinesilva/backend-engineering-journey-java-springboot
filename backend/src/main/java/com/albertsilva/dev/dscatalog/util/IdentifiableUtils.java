package com.albertsilva.dev.dscatalog.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.albertsilva.dev.dscatalog.domain.Identifiable;

/**
 * Utilitário estático para trabalhar com objetos que possuem identificador
 * ({@link Identifiable}).
 *
 * <p>
 * Contém um único método, {@link #reorderByReference(List, List)}, que
 * reordena uma lista de entidades seguindo a ordem de uma segunda lista de
 * referência. No backend ele existe para um caso específico:
 * {@code ProductService.findAllPaged} obtém primeiro a página de produtos
 * (ids e nomes, já na ordem pedida pelo cliente) e depois carrega as entidades
 * com as categorias em uma consulta {@code IN}, cuja ordem de retorno não é
 * garantida; este método restaura a ordem da página.
 * </p>
 *
 * <p>
 * A classe é {@code final}, não guarda estado e só tem métodos estáticos. Ela
 * <b>não declara construtor privado</b>, portanto o construtor padrão público
 * continua disponível (a classe não impede ser instanciada).
 * </p>
 */
public final class IdentifiableUtils {

  /**
   * Reordena {@code unordered} segundo a ordem dos identificadores de
   * {@code ordered}.
   *
   * <p>
   * Funcionamento: indexa os elementos de {@code unordered} por
   * {@code getId()} e, em seguida, percorre {@code ordered}; para cada
   * identificador encontrado no índice, acrescenta o elemento correspondente ao
   * resultado. A ordem final é, portanto, a de {@code ordered}.
   * </p>
   *
   * <p>
   * Comportamento a conhecer:
   * </p>
   * <ul>
   * <li>Elementos de {@code ordered} sem correspondente em {@code unordered} são
   * <b>descartados</b> em silêncio, assim como elementos de {@code unordered}
   * cujo id não aparece em {@code ordered}; o resultado pode ter menos elementos
   * que qualquer uma das listas.</li>
   * <li>Se {@code unordered} tiver dois elementos com o mesmo id, prevalece o
   * <b>último</b>; se {@code ordered} repetir um id, o elemento correspondente
   * aparece repetido no resultado.</li>
   * <li>Os ids são comparados com {@code equals}/{@code hashCode} do tipo
   * {@code ID}. Um id {@code null} é aceito como chave e pode ser
   * correspondido.</li>
   * <li>As listas recebidas não são modificadas; o retorno é uma nova
   * {@link ArrayList} (mutável), com custo proporcional ao tamanho das duas
   * listas.</li>
   * <li>Não sincroniza nada: é seguro para uso concorrente desde que as listas
   * recebidas não sejam alteradas por outra thread durante a chamada.</li>
   * </ul>
   *
   * @param unordered lista de elementos a reordenar (não pode ser {@code null}
   *                  nem conter elementos {@code null})
   * @param ordered   lista que define a ordem desejada, por identificador
   *                  (não pode ser {@code null} nem conter elementos
   *                  {@code null}); pode ser de outro tipo, desde que seja
   *                  {@link Identifiable} com o mesmo {@code ID}
   * @param <ID>      tipo do identificador
   * @param <T>       tipo dos elementos a reordenar
   * @return nova lista com os elementos de {@code unordered} na ordem de
   *         {@code ordered}
   * @throws NullPointerException se alguma das listas ou algum de seus elementos
   *                              for {@code null}
   */
  public static <ID, T extends Identifiable<ID>> List<T> reorderByReference(List<T> unordered,
      List<? extends Identifiable<ID>> ordered) {

    Map<ID, T> map = new HashMap<>();

    for (T entity : unordered) {
      map.put(entity.getId(), entity);
    }

    List<T> result = new ArrayList<>();

    for (Identifiable<ID> identifiable : ordered) {
      T matched = map.get(identifiable.getId());

      if (matched != null) {
        result.add(matched);
      }
    }

    return result;
  }

}
