package com.albertsilva.dev.dscatalog.security.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.service.exception.AuthenticatedUserNotFoundException;

/**
 * Serviço responsável pela resolução do usuário autenticado na aplicação.
 *
 * <p>
 * A identidade do usuário é obtida exclusivamente a partir do JWT presente
 * no {@link SecurityContextHolder}. O claim {@code userId} representa o
 * identificador único e estável do usuário no domínio.
 * </p>
 *
 * <p>
 * O serviço não utiliza atributos mutáveis como e-mail ou username para
 * localizar o usuário autenticado, evitando inconsistências após alterações
 * cadastrais.
 * </p>
 *
 * <p>
 * Fluxo de resolução:
 * </p>
 *
 * <pre>
 * JWT
 *  |
 *  |-- userId
 *       |
 *       v
 * UserRepository.findById()
 *       |
 *       v
 * Usuário autenticado
 * </pre>
 *
 * @author Albert Silva
 */
@Service
public class AuthenticatedUserService {

  private final UserRepository userRepository;

  public AuthenticatedUserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Recupera o usuário atualmente autenticado.
   *
   * <p>
   * O método valida a existência da autenticação, garante que o principal
   * seja um JWT válido e extrai o {@code userId} utilizado para buscar
   * o usuário persistido.
   * </p>
   *
   * @return usuário autenticado encontrado no banco de dados
   *
   * @throws AuthenticatedUserNotFoundException quando não existe autenticação,
   *                                            o principal não é um JWT válido, o
   *                                            claim {@code userId} não está
   *                                            presente ou o usuário não foi
   *                                            encontrado
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
}