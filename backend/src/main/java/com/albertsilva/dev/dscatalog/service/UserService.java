package com.albertsilva.dev.dscatalog.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.albertsilva.dev.dscatalog.domain.user.Role;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.dscatalog.mapper.user.UserMapper;
import com.albertsilva.dev.dscatalog.projection.UserDetailsProjection;
import com.albertsilva.dev.dscatalog.repository.RoleRepository;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;

/**
 * Serviço responsável pelas operações de negócio relacionadas à entidade
 * {@link User}.
 *
 * <p>
 * Gerencia usuários, centralizando regras de negócio,
 * validações, persistência e tratamento transacional.
 * </p>
 *
 * <p>
 * <b>Responsabilidades:</b>
 * </p>
 * <ul>
 * <li>Operações de CRUD de usuários</li>
 * <li>Paginação e filtros de busca</li>
 * <li>Conversão entre entidades e DTOs</li>
 * <li>Tratamento de exceções de negócio</li>
 * <li>Garantia de integridade e consistência dos dados</li>
 * </ul>
 *
 * @implNote
 *           Atua como camada de serviço (Service Layer), intermediando
 *           Controller, Repository e Mapper dentro da arquitetura Spring Boot.
 *
 * @apiNote
 *          Esta implementação exemplifica conceitos fundamentais de aplicações
 *          corporativas,
 *          como Service Layer, arquitetura em camadas, DTO Pattern,
 *          persistência com JPA, paginação e regras de negócio centralizadas.
 */
@Service
public class UserService implements UserDetailsService {

  private static final Logger logger = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  /**
   * Constrói o serviço de usuários com suas dependências principais.
   *
   * @param userRepository  repositório de usuários
   * @param roleRepository  repositório de papéis
   * @param userMapper      responsável pela conversão entre DTOs e entidades
   * @param passwordEncoder codificador de senhas
   */
  public UserService(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Busca paginada de usuários, com opção de filtro por nome.
   *
   * <p>
   * Permite busca parcial e case insensitive,
   * utilizando correspondência por conteúdo textual.
   * </p>
   *
   * @param firstName termo de busca para o nome do usuário
   * @param pageable  informações de paginação
   * @return página de usuários encontrados
   *
   * @implNote
   *           Utiliza consulta derivada do Spring Data JPA:
   *           {@code findByFirstNameContainingIgnoreCase}.
   *
   *           <p>
   *           Essa abordagem reduz necessidade
   *           de implementação manual de queries.
   *           </p>
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          consultas derivadas, filtros dinâmicos,
   *          paginação e busca textual eficiente.
   */
  @Transactional(readOnly = true)
  public Page<UserResponse> search(String firstName, Pageable pageable) {

    String filter = StringUtils.hasText(firstName) ? firstName.trim() : null;

    logger.debug("Buscando usuários | filtro={} | page={} | size={} | sort={}", filter, pageable.getPageNumber(),
        pageable.getPageSize(), pageable.getSort());

    Page<User> usersPage = (filter != null) ? userRepository.findByFirstNameContainingIgnoreCase(filter, pageable)
        : userRepository.findAll(pageable);

    logger.info("Busca concluída | totalElements={} | totalPages={}", usersPage.getTotalElements(),
        usersPage.getTotalPages());

    return userMapper.toResponsePage(usersPage);
  }

  /**
   * Busca um usuário pelo seu identificador.
   *
   * <p>
   * Retorna os dados completos do usuário,
   * garantindo validação segura da existência do registro.
   * </p>
   *
   * @param id identificador do usuário
   * @return dados do usuário
   * @throws ResourceNotFoundException caso o usuário não exista
   *
   * @implNote
   *           Utiliza {@code findById(id)}, realizando consulta imediata no
   *           banco.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          Optional, tratamento de exceções e busca segura de entidades.
   */
  @Transactional(readOnly = true)
  public UserDetailsResponse findById(Long id) {
    return userMapper.toDetailsResponse(findEntityById(id));
  }

  /**
   * Insere um novo usuário no sistema.
   *
   * <p>
   * Converte o DTO de entrada em entidade
   * e persiste os dados no banco.
   * </p>
   *
   * @param userCreateRequest dados para criação do usuário
   * @return usuário criado
   *
   * @implNote
   *           Utiliza conversão DTO → Entity,
   *           garantindo separação entre camada de apresentação e persistência.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          DTO Pattern, persistência e criação de entidades em APIs RESTful.
   */
  @Transactional
  public UserResponse create(UserCreateRequest request) {
    logger.debug("Criando novo usuário - email: {}", request.email());

    User entity = userMapper.toEntity(request, findRolesByIdsOrThrow(request.roleIds()));
    entity.setPassword(passwordEncoder.encode(request.password()));
    entity.activate();

    entity = userRepository.save(entity);
    logger.info("Usuário criado com sucesso. id: {}", entity.getId());
    return userMapper.toResponse(entity);
  }

  /**
   * Atualiza parcialmente um usuário existente.
   *
   * <p>
   * Permite modificar apenas campos informados,
   * preservando dados não enviados.
   * </p>
   *
   * @param id                identificador do usuário
   * @param userUpdateRequest dados para atualização parcial
   * @return usuário atualizado
   * @throws ResourceNotFoundException caso o usuário não exista
   *
   * @implNote
   *           Utiliza {@code getReferenceById(id)} para obter uma referência lazy
   *           (proxy) da entidade, evitando consulta imediata ao banco.
   *
   *           <p>
   *           O proxy será inicializado somente quando atributos forem acessados.
   *           </p>
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          JPA Proxy, Lazy Loading, atualização parcial e Contexto de
   *          Persistência.
   */
  @Transactional
  public UserResponse update(Long id, UserUpdateRequest request) {

    logger.debug("Atualizando usuário. id: {}", id);

    try {

      User entity = userRepository.getReferenceById(id);

      userMapper.updateEntity(request, entity);

      updateRolesIfPresent(request, entity);
      updatePasswordIfPresent(request, entity);

      entity = userRepository.save(entity);

      logger.info("Usuário atualizado com sucesso. id: {}", id);

      return userMapper.toResponse(entity);

    } catch (EntityNotFoundException e) {

      logger.warn("Falha ao atualizar. Usuário não encontrado. id: {}", id);

      throw new ResourceNotFoundException("error.user.notFound");
    }
  }

  /**
   * Ativa um usuário existente.
   *
   * <p>
   * Altera o status do usuário para ativo,
   * permitindo que ele seja exibido e utilizado.
   * </p>
   *
   * @param id identificador do usuário
   * @throws ResourceNotFoundException caso o usuário não exista
   *
   * @implNote
   *           Realiza atualização parcial do status do usuário,
   *           mantendo as demais informações inalteradas.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          atualização parcial, status de entidade e regras de negócio.
   */
  @Transactional
  public void activate(Long id) {
    User entity = findEntityById(id);

    if (entity.isActive() == true) {
      logger.debug("Status já definido | id={} | active={}", id, true);
      return;
    }

    entity.activate();

    logger.info("Status alterado | id={} | active={}", id, true);
  }

  /*
   * Desativa um usuário existente.
   *
   * <p>
   * Altera o status do usuário para inativo,
   * ocultando-o de listagens e impedindo sua utilização.
   * </p>
   *
   * @param id identificador do usuário
   * 
   * @throws ResourceNotFoundException caso o usuário não exista
   *
   * @implNote
   * Realiza atualização parcial do status do usuário,
   * mantendo as demais informações inalteradas.
   *
   * @apiNote
   * Esta implementação reforça conceitos importantes como:
   * atualização parcial, status de entidade e regras de negócio.
   */
  @Transactional
  public void deactivate(Long id) {
    User entity = findEntityById(id);

    if (entity.isActive() == false) {
      logger.debug("Status já definido | id={} | active={}", id, false);
      return;
    }

    entity.deactivate();

    logger.info("Status alterado | id={} | active={}", id, false);
  }

  /**
   * Deleta um usuário existente.
   *
   * <p>
   * Remove o usuário do banco de dados,
   * garantindo que ele não seja mais acessível.
   * </p>
   *
   * @param id identificador do usuário
   * @throws ResourceNotFoundException caso o usuário não exista
   *
   * @implNote
   *           Realiza deleção física da entidade, removendo-a completamente
   *           do banco de dados.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          deleção de entidades, integridade referencial e regras de negócio.
   */
  @Transactional
  public void delete(Long id) {
    logger.debug("Deletando usuário. id: {}", id);

    User entity = findEntityById(id);
    userRepository.delete(entity);
    logger.info("Usuário deletado com sucesso. id: {}", id);
  }

  /**
   * Busca um usuário pelo seu identificador.
   *
   * <p>
   * Retorna os dados completos do usuário,
   * garantindo validação segura da existência do registro.
   * </p>
   *
   * @param id identificador do usuário
   * @return dados do usuário
   * @throws ResourceNotFoundException caso o usuário não exista
   *
   * @implNote
   *           Utiliza {@code findById(id)}, realizando consulta imediata no
   *           banco.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          Optional, tratamento de exceções e busca segura de entidades.
   */
  @Transactional(readOnly = true)
  private User findEntityById(Long id) {
    logger.debug("Buscando usuário por id: {}", id);

    return userRepository.findById(id).orElseThrow(() -> {
      logger.warn("Usuário não encontrado. id: {}", id);
      return new ResourceNotFoundException("error.user.notFound");
    });
  }

  /**
   * Busca um conjunto de papéis (roles) por seus identificadores.
   *
   * <p>
   * Valida que todos os IDs fornecidos correspondem a papéis existentes,
   * garantindo integridade referencial antes de associar a um usuário.
   * </p>
   *
   * @param roleIds conjunto de IDs de papéis
   * @return conjunto de entidades {@link Role} correspondentes
   * @throws ResourceNotFoundException caso algum ID não corresponda a um papel
   *                                   existente
   *
   * @implNote
   *           Utiliza {@code findAllById(roleIds)} para consulta eficiente
   *           e validação de existência em lote, evitando múltiplas consultas
   *           individuais.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          validação de dados, integridade referencial,
   *          otimização de consultas e tratamento de exceções.
   */
  private Set<Role> findRolesByIdsOrThrow(Set<Long> roleIds) {

    if (roleIds == null || roleIds.isEmpty()) {
      return Collections.emptySet();
    }

    Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));

    if (roles.size() != roleIds.size()) {
      throw new ResourceNotFoundException("error.role.ids.notFound");
    }

    return roles;
  }

  /**
   * Carrega um usuário pelo seu email para autenticação.
   *
   * <p>
   * Busca o usuário e suas roles associadas, convertendo-os em um objeto
   * {@link UserDetails} para uso pelo Spring Security.
   * </p>
   *
   * @param username email do usuário a ser autenticado
   * @return detalhes do usuário para autenticação
   * @throws UsernameNotFoundException caso o usuário não seja encontrado
   *
   * @implNote
   *           Utiliza uma consulta personalizada para buscar o usuário e suas
   *           roles em uma única operação, otimizando o processo de
   *           autenticação.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          autenticação, UserDetailsService, consultas personalizadas e
   *          integração com Spring Security.
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    List<UserDetailsProjection> result = userRepository.searchUserAndRolesByEmail(username);

    if (result.isEmpty()) {
      logger.warn("Usuário não encontrado para email: {}", username);
      throw new UsernameNotFoundException("User not found with email: " + username);
    }

    User user = new User();
    user.setEmail(result.get(0).getUsername());
    user.setPassword(result.get(0).getPassword());
    user.setActive(result.get(0).getActive());

    for (UserDetailsProjection projection : result) {
      user.addRole(new Role(projection.getRoleId(), projection.getAuthority()));
    }

    return user;
  }

  private void updatePasswordIfPresent(UserUpdateRequest request, User entity) {

    if (request.password() != null) {
      entity.setPassword(passwordEncoder.encode(request.password()));
    }
  }

  private void updateRolesIfPresent(UserUpdateRequest request, User entity) {

    if (request.roleIds() == null) {
      return;
    }

    Set<Role> roles = findRolesByIdsOrThrow(request.roleIds());

    entity.getRoles().clear();
    entity.getRoles().addAll(roles);
  }

}