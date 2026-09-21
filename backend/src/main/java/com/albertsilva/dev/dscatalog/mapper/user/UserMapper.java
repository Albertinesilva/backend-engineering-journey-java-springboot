package com.albertsilva.dev.dscatalog.mapper.user;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.albertsilva.dev.dscatalog.domain.user.Role;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.dto.role.response.RoleResponse;
import com.albertsilva.dev.dscatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.UserRegisterRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.user.response.UserResponse;

/**
 * Conversor manual (sem MapStruct) entre os DTOs de usuário e a entidade
 * {@link User}. Componente sem estado, usado por {@code UserService} e
 * {@code AccountService}.
 *
 * <p>
 * <b>Entrada:</b> os dois {@code toEntity} copiam nome, e-mail e senha
 * <b>como recebidos</b> (sem {@code trim}, sem conversão de caixa e
 * <b>sem codificar a senha</b>: o service a codifica logo depois) e recebem as
 * roles já resolvidas como entidades, sem ler {@code roleIds}. O
 * {@code updateEntity} copia apenas nome, sobrenome e e-mail; senha e roles da
 * atualização são tratadas por {@code UserService}. O fluxo
 * {@code AccountService.updateAuthenticatedUser} <b>não usa</b> este mapper.
 * </p>
 *
 * <p>
 * <b>Saída:</b> as respostas nunca incluem a senha (nem o hash) nem
 * {@code User.tokens}; percorrem {@code User.getRoles()} (carregada sob
 * demanda, possível consulta adicional por usuário) e convertem cada role em
 * {@link RoleResponse}.
 * </p>
 */
@Component
public class UserMapper {

  /**
   * Cria um novo {@link User} (ainda sem id) a partir do request de criação por
   * administrador, com as roles informadas.
   *
   * <p>
   * Copia {@code firstName}, {@code lastName}, {@code email} e {@code password}
   * (<b>texto puro</b>, sem codificar) e adiciona as roles recebidas em
   * {@code roles}, se não for {@code null}. {@code request.roleIds()} <b>não é
   * lido</b>: a resolução dos ids em entidades é feita antes, por
   * {@code UserService}. O indicador {@code active} não é definido (valor padrão
   * {@code false}; o service chama {@code activate()}).
   * </p>
   *
   * @param request dados de criação
   * @param roles   roles já resolvidas (pode ser {@code null})
   * @return nova entidade (não persistida) ou {@code null} se {@code request} for
   *         {@code null}
   */
  public User toEntity(UserCreateRequest request, Set<Role> roles) {

    if (request == null) {
      return null;
    }

    User user = new User();
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setEmail(request.email());
    user.setPassword(request.password());

    if (roles != null) {
      user.getRoles().addAll(roles);
    }

    return user;
  }

  /**
   * Cria um novo {@link User} (ainda sem id) a partir do request de registro
   * público, com as roles informadas.
   *
   * <p>
   * Mesmo comportamento de {@link #toEntity(UserCreateRequest, Set)}: copia nome,
   * e-mail e senha como recebidos (senha em texto puro), adiciona as roles
   * recebidas e não define {@code active} (o {@code AccountService.register}
   * chama {@code deactivate()} e passa apenas {@code ROLE_OPERATOR}).
   * </p>
   *
   * @param request dados do registro
   * @param roles   roles já resolvidas (pode ser {@code null})
   * @return nova entidade (não persistida) ou {@code null} se {@code request} for
   *         {@code null}
   */
  public User toEntity(UserRegisterRequest request, Set<Role> roles) {

    if (request == null) {
      return null;
    }

    User user = new User();
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setEmail(request.email());
    user.setPassword(request.password());

    if (roles != null) {
      user.getRoles().addAll(roles);
    }

    return user;
  }

  /**
   * Aplica ao usuário os campos básicos do request de atualização:
   * {@code firstName}, {@code lastName} e {@code email}.
   *
   * <p>
   * Ao contrário de {@code CategoryMapper} e {@code ProductMapper}, <b>não
   * verifica {@code null}</b>: os três valores sempre são gravados, mesmo que
   * {@code null}, e o e-mail não é normalizado (o DTO exige valores não vazios).
   * Uma entrada {@code request} ou {@code entity} {@code null} lança
   * {@link NullPointerException}. <b>Não altera</b> senha, roles, {@code active}
   * nem tokens: {@code UserService} trata senha e roles à parte.
   * </p>
   *
   * @param request dados de atualização
   * @param entity  usuário a ser modificado
   */
  public void updateEntity(UserUpdateRequest request, User entity) {

    entity.setFirstName(request.firstName());
    entity.setLastName(request.lastName());
    entity.setEmail(request.email());
  }

  /**
   * Converte o usuário em resposta: {@code id}, nomes, e-mail e roles como
   * {@link RoleResponse}. Não inclui senha, {@code active} nem tokens.
   *
   * <p>
   * Percorre {@code entity.getRoles()} (carregada sob demanda quando a entidade
   * vem do banco). As roles são coletadas em um {@link LinkedHashSet}, que apenas
   * preserva a ordem de iteração da coleção de origem (um {@code HashSet}, sem
   * ordem definida).
   * </p>
   *
   * @param entity usuário
   * @return resposta ou {@code null} se {@code entity} for {@code null}
   */
  public UserResponse toResponse(User entity) {

    if (entity == null) {
      return null;
    }

    Set<RoleResponse> roles = entity.getRoles().stream().map(this::toRoleResponse)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    return new UserResponse(entity.getId(), entity.getFirstName(), entity.getLastName(), entity.getEmail(), roles);
  }

  /**
   * Converte o usuário em resposta detalhada: os campos de
   * {@link #toResponse(User)} mais o indicador {@code active}. Percorre
   * {@code entity.getRoles()} da mesma forma. Não inclui senha nem tokens.
   *
   * @param entity usuário
   * @return resposta ou {@code null} se {@code entity} for {@code null}
   */
  public UserDetailsResponse toDetailsResponse(User entity) {

    if (entity == null) {
      return null;
    }

    Set<RoleResponse> roles = entity.getRoles().stream().map(this::toRoleResponse)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    return new UserDetailsResponse(entity.getId(), entity.getFirstName(), entity.getLastName(), entity.getEmail(),
        roles, entity.isActive());
  }

  /**
   * Converte uma página de usuários em página de {@link UserResponse} aplicando
   * {@link #toResponse(User)} a cada elemento e preservando os metadados de
   * paginação.
   *
   * @param entities página de entidades
   * @return página de respostas
   */
  public Page<UserResponse> toResponsePage(Page<User> entities) {
    return entities.map(this::toResponse);
  }

  /**
   * Converte uma {@link Role} em {@link RoleResponse} (id e authority).
   */
  private RoleResponse toRoleResponse(Role role) {
    return new RoleResponse(role.getId(), role.getAuthority());
  }
}