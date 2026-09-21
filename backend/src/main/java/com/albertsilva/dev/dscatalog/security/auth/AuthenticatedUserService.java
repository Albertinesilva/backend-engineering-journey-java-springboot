package com.albertsilva.dev.dscatalog.security.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.service.exception.AuthenticatedUserNotFoundException;

/**
 * Resolve o <b>usuário autenticado</b> da requisição a partir do JWT.
 *
 * <p>
 * <b>Fonte da identidade:</b> o {@code Authentication} do
 * {@link SecurityContextHolder}, cujo principal deve ser um {@link Jwt}. O
 * usuário é identificado pelo claim customizado <b>{@code userId}</b> (não pelo
 * e-mail, pelo claim {@code username}, por {@code sub} nem por {@code jti}) e
 * carregado com {@code UserRepository.findById}. No código atual, este é o
 * único ponto da aplicação que transforma o JWT em um {@code User}.
 * </p>
 *
 * <p>
 * <b>Usos:</b> {@code AccountService} (perfil e senha do próprio usuário),
 * {@code UniqueEmailForAuthenticatedUserValidator} e a expressão
 * {@code @authenticatedUserService.isCurrentUser(#id)} de
 * {@code UserController.findById}.
 * </p>
 *
 * <p>
 * <b>Não faz:</b> não verifica se o usuário está ativo (uma conta desativada
 * com token ainda válido é resolvida normalmente); não usa transação própria;
 * cada chamada faz uma consulta ao banco.
 * </p>
 *
 * <pre>
 * SecurityContext → Authentication.getPrincipal() (Jwt)
 *   → claim "userId" → UserRepository.findById → User
 * </pre>
 */
@Service
public class AuthenticatedUserService {

  private final UserRepository userRepository;

  /**
   * @param userRepository repositório usado para carregar o usuário pelo id do JWT
   */
  public AuthenticatedUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Devolve o usuário do JWT da requisição corrente.
   *
   * <p>
   * Lança {@link AuthenticatedUserNotFoundException} (com chave de mensagem) em
   * quatro situações: sem {@code Authentication} ou com principal que não seja
   * {@link Jwt} ({@code error.auth.invalid.principal}); claim {@code userId}
   * ausente ou menor ou igual a zero ({@code error.auth.userId.claim.notFound});
   * usuário inexistente ({@code error.auth.user.notFound}). O claim é lido como
   * {@code Long}.
   * </p>
   *
   * @return usuário persistido correspondente ao {@code userId} do token
   * @throws AuthenticatedUserNotFoundException nas situações acima
   */
  public User getAuthenticatedUser() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      throw new AuthenticatedUserNotFoundException("error.auth.invalid.principal");
    }

    Long userId = jwt.getClaim("userId");

    if (userId == null || userId <= 0) {
      throw new AuthenticatedUserNotFoundException("error.auth.userId.claim.notFound");
    }

    return userRepository.findById(userId)
        .orElseThrow(() -> new AuthenticatedUserNotFoundException("error.auth.user.notFound"));
  }

  /**
   * Indica se {@code userId} é o id do usuário do JWT. Chama
   * {@link #getAuthenticatedUser()}, portanto lança a mesma exceção quando não há
   * identidade válida.
   *
   * @param userId id a comparar (um valor {@code null} resulta em
   *               {@code false})
   * @return {@code true} se for o usuário autenticado
   */
  public boolean isCurrentUser(Long userId) {
    return getAuthenticatedUser().getId().equals(userId);
  }
}