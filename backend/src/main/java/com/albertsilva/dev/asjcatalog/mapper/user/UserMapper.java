package com.albertsilva.dev.asjcatalog.mapper.user;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.albertsilva.dev.asjcatalog.dto.role.response.RoleResponse;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.asjcatalog.entity.Role;
import com.albertsilva.dev.asjcatalog.entity.User;

@Component
public class UserMapper {

  /*
   * *
   * Converte um {@link UserCreateRequest} em uma entidade {@link User}.
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Cria uma nova instância de {@link User}</li>
   * <li>Mapeia os campos básicos (firstName, lastName, email, password)</li>
   * <li>Associa os papéis (roles) com base nos IDs fornecidos</li>
   * </ul>
   *
   * @param request dados recebidos na requisição de criação
   * 
   * @param roles conjunto de entidades {@link Role} associadas ao usuário
   * 
   * @return entidade pronta para persistência ou {@code null} se o request for
   * nulo
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

  /*
   * *
   * *
   * Atualiza uma entidade {@link User} com os dados de um
   * {@link UserUpdateRequest}.
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Atualiza os campos básicos (firstName, lastName, email)</li>
   * <li>Substitui os papéis (roles) pelos novos fornecidos</li>
   * </ul>
   *
   * @param request dados recebidos na requisição de atualização
   * 
   * @param entity entidade a ser atualizada
   * 
   * @param roles conjunto de entidades {@link Role} associadas ao usuário
   */
  public void updateEntity(UserUpdateRequest request, User entity, Set<Role> roles) {

    entity.setFirstName(request.firstName());
    entity.setLastName(request.lastName());
    entity.setEmail(request.email());

    entity.getRoles().clear();
    entity.getRoles().addAll(roles);
  }

  /*
   * *
   * Converte uma entidade {@link User} em um DTO de resposta {@link
   * UserResponse}.
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Cria uma nova instância de {@link UserResponse}</li>
   * <li>Mapeia os campos básicos (id, firstName, lastName, email)</li>
   * <li>Converte os papéis (roles) para um conjunto de strings representando as
   * autoridades</li>
   * </ul>
   *
   * @param entity entidade a ser convertida
   * 
   * @return DTO de resposta ou {@code null} se a entidade for nula
   */
  public UserResponse toResponse(User entity) {

    if (entity == null) {
      return null;
    }

    Set<String> roles = entity.getRoles().stream().map(Role::getAuthority)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    return new UserResponse(entity.getId(), entity.getFirstName(), entity.getLastName(), entity.getEmail(), roles);
  }

  /*
   * *
   * Converte uma entidade {@link User} em um DTO de resposta detalhado
   * {@link UserDetailsResponse}.
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Cria uma nova instância de {@link UserDetailsResponse}</li>
   * <li>Mapeia os campos básicos (id, firstName, lastName, email)</li>
   * <li>Converte os papéis (roles) para um conjunto de {@link RoleResponse}
   * contendo id e autoridade</li>
   * </ul>
   *
   * @param entity entidade a ser convertida
   * 
   * @return DTO de resposta detalhado ou {@code null} se a entidade for nula
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
   * Converte uma página de entidades {@link User} em uma página de
   * {@link UserResponse}.
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Utiliza a função {@link #toResponse(User)} para mapear cada elemento</li>
   * <li>Mantém as informações de paginação (total, páginas, etc.)</li>
   * </ul>
   *
   * @param entities página de entidades
   * @return página de DTOs de resposta
   */
  public Page<UserResponse> toResponsePage(Page<User> entities) {
    return entities.map(this::toResponse);
  }

  /**
   * Converte uma entidade {@link Role} em {@link RoleResponse}.
   *
   * @param role entidade role
   * @return DTO correspondente
   */
  private RoleResponse toRoleResponse(Role role) {
    return new RoleResponse(role.getId(), role.getAuthority());
  }
}