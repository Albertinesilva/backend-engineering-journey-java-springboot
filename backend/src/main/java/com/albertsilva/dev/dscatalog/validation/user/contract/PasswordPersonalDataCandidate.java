package com.albertsilva.dev.dscatalog.validation.user.contract;

/**
 * Contrato dos DTOs que podem ser verificados por
 * {@link com.albertsilva.dev.dscatalog.validation.user.annotation.PasswordPersonalData}:
 * expõe os quatro dados que o validator compara.
 *
 * <p>
 * É implementado, hoje, por {@code UserCreateRequest} e
 * {@code UserRegisterRequest} (os componentes do {@code record} satisfazem os
 * métodos abaixo). Não é usado por {@code UserUpdateRequest}, cuja verificação
 * equivalente está em {@link com.albertsilva.dev.dscatalog.validation.user.validator.UserUpdateValidator}.
 * </p>
 */
public interface PasswordPersonalDataCandidate {

  /**
   * @return primeiro nome; pode ser {@code null}. Só é considerado se, após
   *         {@code trim}, tiver 3 ou mais caracteres
   */
  String firstName();

  /**
   * @return sobrenome; pode ser {@code null}. Só é considerado se, após
   *         {@code trim}, tiver 3 ou mais caracteres
   */
  String lastName();

  /**
   * @return e-mail; apenas o trecho anterior ao primeiro {@code @} é comparado
   *         com a senha (se tiver 3 ou mais caracteres). Ignorado se for
   *         {@code null} ou não contiver {@code @}
   */
  String email();

  /**
   * @return senha em texto a ser verificada; se for {@code null}, nenhuma
   *         comparação é feita
   */
  String password();
}