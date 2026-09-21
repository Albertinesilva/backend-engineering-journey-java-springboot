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
 * Serviço de aplicação dos usuários ({@link User}) com dois papéis distintos:
 *
 * <ol>
 * <li><b>Administração de usuários</b>: listagem, consulta, criação,
 * atualização, ativação/desativação e exclusão, chamados por
 * {@code UserController};</li>
 * <li><b>Autenticação</b>: implementa {@link UserDetailsService}, e
 * {@link #loadUserByUsername(String)} é chamado no login (grant
 * {@code password}) por {@code CustomPasswordAuthenticationProvider}.</li>
 * </ol>
 *
 * <p>
 * Os fluxos da própria conta do usuário (registro, ativação, recuperação de
 * senha, perfil e troca de senha) ficam em {@code AccountService}.
 * </p>
 *
 * <p>
 * <b>Dependências:</b> {@code UserRepository}, {@code RoleRepository},
 * {@code UserMapper} e {@code PasswordEncoder}. Este service não chama
 * validators: unicidade e formato do e-mail, força da senha, dados pessoais na
 * senha e existência das roles ({@code @ValidRoles}) são validados antes, na
 * camada web.
 * </p>
 *
 * <p>
 * <b>Regras aplicadas aqui:</b> a senha é codificada na criação e, se
 * informada, na atualização; o usuário criado por este service nasce ativo;
 * as roles são resolvidas por id e todas devem existir; o e-mail é gravado
 * como recebido (sem {@code trim} nem conversão de caixa).
 * </p>
 *
 * <p>
 * <b>Transações:</b> os métodos administrativos são transacionais (leituras
 * com {@code readOnly = true}); {@code loadUserByUsername} não declara
 * transação. <b>Autorização:</b> não é feita aqui, e sim nos controllers
 * ({@code @PreAuthorize}).
 * </p>
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
   * Lista usuários de forma paginada, com filtro opcional por primeiro nome.
   *
   * <p>
   * O termo recebe {@code trim}; nulo, vazio ou só com espaços significa "sem
   * filtro" e usa {@code findAll(pageable)}. Com filtro, usa
   * {@code findByFirstNameContainingIgnoreCase} (apenas {@code firstName}; o
   * sobrenome não participa).
   * </p>
   *
   * <p>
   * A conversão para {@link UserResponse} acessa as roles de cada usuário, que
   * são carregadas sob demanda (padrão N+1 provável, dentro da transação de
   * leitura).
   * </p>
   *
   * @param firstName termo procurado no primeiro nome (opcional)
   * @param pageable  página, tamanho e ordenação
   * @return página de usuários
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
   * Retorna os dados de um usuário, incluindo o indicador {@code active} e as
   * roles.
   *
   * @param id identificador do usuário
   * @return detalhes do usuário
   * @throws ResourceNotFoundException ({@code error.user.notFound}) se o
   *                                   usuário não existir
   */
  @Transactional(readOnly = true)
  public UserDetailsResponse findById(Long id) {
    return userMapper.toDetailsResponse(findEntityById(id));
  }

  /**
   * Cria um usuário <b>ativo</b>, com as roles indicadas, a pedido de um
   * administrador.
   *
   * <p>
   * Sequência: resolve as roles por id ({@code findRolesByIdsOrThrow}); o
   * mapper cria a entidade; a senha é codificada; {@code activate()}; o
   * usuário é salvo.
   * </p>
   *
   * <p>
   * <b>Usuário sem roles:</b> {@code roleIds} nulo ou vazio é aceito e resulta
   * em usuário sem roles (o validator {@code @ValidRoles} também aceita esse
   * caso). Como {@code searchUserAndRolesByEmail} usa junção interna, esse
   * usuário não é encontrado por {@link #loadUserByUsername(String)} e, na
   * prática, não consegue autenticar.
   * </p>
   *
   * <p>
   * O e-mail é gravado como recebido. A unicidade é validada antes, por
   * {@code @UniqueEmail}; o banco também impõe e-mail único.
   * </p>
   *
   * @param request dados do novo usuário
   * @return usuário criado
   * @throws ResourceNotFoundException ({@code error.role.ids.notFound}) se
   *                                   alguma role não existir
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
   * Atualiza nome, sobrenome e e-mail de um usuário e, se informados, suas
   * roles e sua senha.
   *
   * <p>
   * Usa {@code getReferenceById} (referência preguiçosa): a existência só é
   * verificada quando o proxy é acessado, dentro do bloco {@code try}; se o
   * usuário não existir, o {@code EntityNotFoundException} da JPA é convertido
   * em {@link ResourceNotFoundException}.
   * </p>
   *
   * <ul>
   * <li>{@code UserMapper.updateEntity} copia nome, sobrenome e e-mail sem
   * verificar nulos (o DTO exige valores não vazios); o e-mail não é
   * normalizado</li>
   * <li>{@code roleIds} nulo mantém as roles; um conjunto (mesmo vazio)
   * substitui as atuais, portanto um conjunto vazio remove todas</li>
   * <li>{@code password} nulo mantém a senha; caso contrário é codificada</li>
   * <li>o indicador {@code active} não é alterado</li>
   * </ul>
   *
   * <p>
   * A unicidade do e-mail (excluindo o próprio usuário) e a regra de dados
   * pessoais na senha são validadas antes, por {@code @UserUpdateValid}.
   * </p>
   *
   * @param id      identificador do usuário
   * @param request dados da atualização
   * @return usuário atualizado
   * @throws ResourceNotFoundException se o usuário
   *                                   ({@code error.user.notFound}) ou alguma
   *                                   role ({@code error.role.ids.notFound})
   *                                   não existir
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
   * Marca o usuário como ativo. Operação idempotente: se já estiver ativo,
   * nada é alterado.
   *
   * <p>
   * Não chama {@code save}: a alteração da entidade gerenciada é gravada no
   * commit. Na autenticação, o indicador é o que {@code User#isEnabled()}
   * devolve.
   * </p>
   *
   * @param id identificador do usuário
   * @throws ResourceNotFoundException se o usuário não existir
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

  /**
   * Marca o usuário como inativo. Operação idempotente: se já estiver
   * inativo, nada é alterado.
   *
   * <p>
   * Não chama {@code save}: a alteração da entidade gerenciada é gravada no
   * commit. O usuário deixa de conseguir obter novos tokens (o provedor do
   * grant {@code password} recusa contas com {@code isEnabled() == false}),
   * mas o método não invalida tokens já emitidos nem oculta o usuário de
   * {@link #search(String, Pageable)}.
   * </p>
   *
   * @param id identificador do usuário
   * @throws ResourceNotFoundException se o usuário não existir
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
   * Remove fisicamente um usuário.
   *
   * <p>
   * Carrega o usuário com {@code findById} (404 se não existir) e chama
   * {@code delete}. Como {@code User.tokens} tem {@code cascade = ALL}, os
   * tokens do usuário são removidos junto, e os vínculos em
   * {@code tb_user_role} também (lado dono). Não há tratamento de
   * {@code DataIntegrityViolationException} neste método nem verificação de
   * que o usuário removido seja o próprio solicitante.
   * </p>
   *
   * @param id identificador do usuário
   * @throws ResourceNotFoundException se o usuário não existir
   */
  @Transactional
  public void delete(Long id) {
    logger.debug("Deletando usuário. id: {}", id);

    User entity = findEntityById(id);
    userRepository.delete(entity);
    logger.info("Usuário deletado com sucesso. id: {}", id);
  }

  /**
   * Carrega a entidade {@link User} pelo identificador ou lança exceção.
   *
   * <p>
   * A anotação {@code @Transactional(readOnly = true)} neste método privado
   * provavelmente não é aplicada pelo proxy do Spring (chamadas internas e
   * métodos privados não passam pelo proxy); a execução ocorre na transação do
   * método público que o chamou.
   * </p>
   *
   * @param id identificador do usuário
   * @return usuário encontrado
   * @throws ResourceNotFoundException ({@code error.user.notFound}) se não
   *                                   existir
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
   * Resolve os ids de roles em entidades, exigindo que todas existam.
   *
   * <p>
   * Conjunto nulo ou vazio devolve um conjunto vazio (sem erro). Caso
   * contrário, busca as roles com {@code findAllById} e compara a quantidade
   * encontrada com a de ids recebidos; se diferirem, lança
   * {@link ResourceNotFoundException}.
   * </p>
   *
   * @param roleIds ids das roles (pode ser nulo ou vazio)
   * @return roles correspondentes (vazio se {@code roleIds} for nulo ou vazio)
   * @throws ResourceNotFoundException ({@code error.role.ids.notFound}) se
   *                                   alguma role não existir
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
   * Carrega os dados de autenticação do usuário cujo e-mail é
   * {@code username}, para o fluxo de login.
   *
   * <p>
   * <b>Quem chama:</b> {@code CustomPasswordAuthenticationProvider}, no grant
   * {@code password}; este é o único chamador em {@code src/main} (o
   * {@code UserDetailsService} é injetado em {@code AuthorizationServerConfig}
   * e repassado ao provider).
   * </p>
   *
   * <p>
   * <b>O que faz:</b> executa uma única consulta nativa,
   * {@code searchUserAndRolesByEmail}, que devolve uma linha por role. Não
   * declara transação e não usa carregamento tardio.
   * </p>
   *
   * <p>
   * <b>User parcial:</b> o objeto devolvido é uma instância nova de
   * {@link User}, nunca persistida, com apenas id, e-mail, senha (hash),
   * {@code active} e uma {@code Role} (id e authority) por linha.
   * {@code firstName}, {@code lastName} e {@code tokens} não são preenchidos.
   * No fluxo de login o provider usa o resultado para conferir a senha
   * ({@code getPassword}), o status ({@code isEnabled} e demais
   * {@code isAccount...}), as authorities (escopos) e o id
   * ({@code ((User) userDetails).getId()}); não foi encontrada nenhuma leitura
   * de nome ou sobrenome desse objeto.
   * </p>
   *
   * <p>
   * <b>Casos limite:</b> o e-mail é comparado exatamente como gravado (sem
   * {@code trim} nem ignorar caixa); usuário inexistente ou sem nenhuma role
   * gera lista vazia e {@link UsernameNotFoundException}, que o provider
   * converte em {@code invalid_grant} ("Invalid credentials"); usuário inativo
   * é carregado normalmente, e a recusa ocorre depois, no provider.
   * </p>
   *
   * @param username e-mail do usuário
   * @return detalhes do usuário para autenticação
   * @throws UsernameNotFoundException se não houver linhas para o e-mail
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    List<UserDetailsProjection> result = userRepository.searchUserAndRolesByEmail(username);

    if (result.isEmpty()) {
      logger.warn("Usuário não encontrado para email: {}", username);
      throw new UsernameNotFoundException("User not found with email: " + username);
    }

    User user = new User();
    user.setId(result.get(0).getId());
    user.setEmail(result.get(0).getUsername());
    user.setPassword(result.get(0).getPassword());
    user.setActive(result.get(0).getActive());

    for (UserDetailsProjection projection : result) {
      user.addRole(new Role(projection.getRoleId(), projection.getAuthority()));
    }

    return user;
  }

  /**
   * Codifica e grava a nova senha somente se {@code request.password()} for
   * diferente de nulo; caso contrário, mantém a senha atual.
   */
  private void updatePasswordIfPresent(UserUpdateRequest request, User entity) {

    if (request.password() != null) {
      entity.setPassword(passwordEncoder.encode(request.password()));
    }
  }

  /**
   * Substitui as roles do usuário somente se {@code request.roleIds()} for
   * diferente de nulo: resolve as roles (todas devem existir), limpa o conjunto
   * atual e adiciona as novas. Um conjunto vazio remove todas as roles.
   *
   * @throws ResourceNotFoundException se alguma role não existir
   */
  private void updateRolesIfPresent(UserUpdateRequest request, User entity) {

    if (request.roleIds() == null) {
      return;
    }

    Set<Role> roles = findRolesByIdsOrThrow(request.roleIds());

    entity.getRoles().clear();
    entity.getRoles().addAll(roles);
  }

}