package com.albertsilva.dev.dscatalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.albertsilva.dev.dscatalog.domain.recovery.Token;
import com.albertsilva.dev.dscatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.dscatalog.domain.user.User;

/**
 * Repositório Spring Data JPA da entidade {@link Token} (tabela
 * {@code tb_token}), com chave primária {@code Long}.
 *
 * <p>
 * Declara duas consultas derivadas, ambas usadas por {@code TokenService}:
 * {@link #findByToken(String)} (validação de um token recebido) e
 * {@link #findByUserAndTypeAndDisabledFalse(User, TokenType)} (desabilitar os
 * tokens ainda em aberto de um usuário). Nenhuma delas considera a expiração:
 * essa verificação é feita pela própria entidade
 * ({@code Token.validate(TokenType)}).
 * </p>
 */
@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

  /**
   * Busca um token pelo valor do campo {@code token} (o UUID enviado ao
   * usuário), com comparação exata.
   *
   * <p>
   * <b>Consulta derivada.</b> O valor é único na tabela ({@code UNIQUE}), então
   * há no máximo um resultado. A consulta <b>não</b> verifica tipo, expiração
   * nem se o token está desabilitado; isso é responsabilidade de quem chama
   * (ver {@code Token.validate(TokenType)}).
   * </p>
   *
   * <p>
   * O usuário dono ({@code Token.user}) tem carregamento tardio: só é lido do
   * banco quando acessado, com sessão JPA aberta.
   * </p>
   *
   * <p>
   * <b>Uso atual:</b> {@code TokenService.findAndValidateToken}.
   * </p>
   *
   * @param token valor do token a ser buscado
   * @return {@link Optional} com o token, ou vazio se não existir
   */
  Optional<Token> findByToken(String token);

  /**
   * Lista os tokens de um usuário, de um determinado tipo, que ainda não foram
   * desabilitados ({@code disabled = false}).
   *
   * <p>
   * <b>Consulta derivada</b> ({@code User} + {@code Type} +
   * {@code DisabledFalse}). O usuário é comparado pelo relacionamento
   * ({@code user_id}, isto é, pelo identificador da entidade informada). Sem
   * paginação e sem ordenação definida.
   * </p>
   *
   * <p>
   * Atenção ao significado de "em aberto": a consulta <b>não</b> filtra por
   * expiração, portanto tokens vencidos, mas ainda não desabilitados, também
   * são retornados.
   * </p>
   *
   * <p>
   * <b>Uso atual:</b> {@code TokenService.disableAllActivationTokens}
   * (chamado por {@code AccountService.resendActivationEmail}) e
   * {@code TokenService.disableAllPasswordRecoveryTokens} (sem chamadores
   * atualmente).
   * </p>
   *
   * @param user o usuário dono dos tokens
   * @param type o tipo dos tokens
   * @return tokens do usuário, do tipo informado, não desabilitados (lista
   *         vazia se não houver)
   */
  List<Token> findByUserAndTypeAndDisabledFalse(User user, TokenType type);

}
