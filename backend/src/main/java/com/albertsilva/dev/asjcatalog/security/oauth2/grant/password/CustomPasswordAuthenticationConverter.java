package com.albertsilva.dev.asjcatalog.security.oauth2.grant.password;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Conversor de requisições OAuth2 para tokens de autenticação customizados.
 *
 * <p>
 * Esta classe implementa {@link AuthenticationConverter} para converter
 * requisições HTTP
 * com grant type "password" (Resource Owner Password Credentials) em tokens de
 * autenticação
 * que podem ser processados pelo provedor de autenticação customizado.
 * </p>
 *
 * <p>
 * <b>Responsabilidade:</b>
 * </p>
 * <p>
 * Extrair e validar parâmetros de uma requisição de token OAuth2,
 * convertendo-os
 * em um objeto {@link CustomPasswordAuthenticationToken} que encapsula as
 * credenciais
 * do usuário e os escopos solicitados.
 * </p>
 *
 * <p>
 * <b>Fluxo de conversão:</b>
 * </p>
 * <ol>
 * <li>Verifica se o grant type é "password"</li>
 * <li>Extrai e valida parâmetros obrigatórios: username e password</li>
 * <li>Extrai e valida parâmetro opcional: scope</li>
 * <li>Coleta parâmetros adicionais não padronizados</li>
 * <li>Cria e retorna um {@link CustomPasswordAuthenticationToken}</li>
 * </ol>
 *
 * <p>
 * <b>Parâmetros esperados:</b>
 * </p>
 * <ul>
 * <li>{@code grant_type}: "password" (obrigatório, validado)</li>
 * <li>{@code username}: nome de usuário (obrigatório)</li>
 * <li>{@code password}: senha do usuário (obrigatório)</li>
 * <li>{@code scope}: escopos solicitados separados por espaço (opcional)</li>
 * <li>Parâmetros adicionais: capturados e passados para o token</li>
 * </ul>
 *
 * <p>
 * <b>Integração com Spring Security:</b>
 * </p>
 * <p>
 * Este conversor é registrado no {@code AuthorizationServerConfig} como parte
 * do pipeline de token endpoint, permitindo que clientes obtenham tokens JWT
 * fornecendo credenciais de usuário diretamente.
 * </p>
 *
 * @implNote
 *           O conversor retorna {@code null} se o grant type não for
 *           "password",
 *           permitindo que outros conversores processem a requisição.
 *           Validações falhas lançam {@link OAuth2AuthenticationException}.
 *
 * @apiNote
 *          Este conversor implementa o fluxo "password" customizado, adaptado
 *          para aplicações que requerem autenticação com credenciais de usuário
 *          em vez do padrão OAuth2 com redirecionamento.
 *
 * @see CustomPasswordAuthenticationToken
 * @see CustomPasswordAuthenticationProvider
 */
public class CustomPasswordAuthenticationConverter implements AuthenticationConverter {

	/**
	 * Converte uma requisição HTTP em um token de autenticação.
	 *
	 * <p>
	 * Processa requisições POST para o endpoint de token com grant type "password",
	 * extraindo e validando credenciais e escopos do usuário.
	 * </p>
	 *
	 * <p>
	 * <b>Comportamento:</b>
	 * </p>
	 * <ul>
	 * <li>Retorna {@code null} se o grant type não for "password"</li>
	 * <li>Lança {@link OAuth2AuthenticationException} se parâmetros obrigatórios
	 * faltarem</li>
	 * <li>Lança {@link OAuth2AuthenticationException} se houver múltiplos valores
	 * para parâmetros únicos</li>
	 * <li>Retorna {@link CustomPasswordAuthenticationToken} com credenciais
	 * validadas</li>
	 * </ul>
	 *
	 * @param request requisição HTTP contendo parâmetros de token
	 * @return {@link CustomPasswordAuthenticationToken} com as credenciais
	 *         extraídas,
	 *         ou {@code null} se o grant type não for "password"
	 * @throws OAuth2AuthenticationException se validação de parâmetros falhar
	 *
	 * @implNote
	 *           O principal do cliente (autenticação do cliente OAuth2) é
	 *           recuperado
	 *           do {@link SecurityContextHolder} e incluído no token retornado.
	 */
	@Nullable
	@Override
	public Authentication convert(HttpServletRequest request) {

		String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);

		if (!"password".equals(grantType)) {
			return null;
		}

		MultiValueMap<String, String> parameters = getParameters(request);

		// scope (OPTIONAL)
		String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
		if (StringUtils.hasText(scope) && parameters.get(OAuth2ParameterNames.SCOPE).size() != 1) {
			throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
		}

		// username (REQUIRED)
		String username = parameters.getFirst(OAuth2ParameterNames.USERNAME);
		if (!StringUtils.hasText(username) || parameters.get(OAuth2ParameterNames.USERNAME).size() != 1) {
			throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
		}

		// password (REQUIRED)
		String password = parameters.getFirst(OAuth2ParameterNames.PASSWORD);
		if (!StringUtils.hasText(password) || parameters.get(OAuth2ParameterNames.PASSWORD).size() != 1) {
			throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
		}

		Set<String> requestedScopes = null;
		if (StringUtils.hasText(scope)) {
			requestedScopes = new HashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
		}

		Map<String, Object> additionalParameters = new HashMap<>();
		parameters.forEach((key, value) -> {
			if (!key.equals(OAuth2ParameterNames.GRANT_TYPE) && !key.equals(OAuth2ParameterNames.SCOPE)
					&& !key.equals(OAuth2ParameterNames.USERNAME) && !key.equals(OAuth2ParameterNames.PASSWORD)) {
				additionalParameters.put(key, value.get(0));
			}
		});

		Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
		return new CustomPasswordAuthenticationToken(clientPrincipal, requestedScopes, additionalParameters, username,
				password);
	}

	/**
	 * Extrai parâmetros da requisição HTTP.
	 *
	 * <p>
	 * Converte o mapa de parâmetros da requisição em um {@link MultiValueMap},
	 * permitindo acesso a múltiplos valores por chave de forma segura.
	 * </p>
	 *
	 * @param request requisição HTTP
	 * @return {@link MultiValueMap} contendo todos os parâmetros da requisição
	 *
	 * @implNote
	 *           Este método utiliza {@link LinkedMultiValueMap} para manter
	 *           a ordem dos parâmetros e suportar múltiplos valores por chave.
	 */
	private static MultiValueMap<String, String> getParameters(HttpServletRequest request) {

		Map<String, String[]> parameterMap = request.getParameterMap();
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(parameterMap.size());
		parameterMap.forEach((key, values) -> {
			if (values.length > 0) {
				for (String value : values) {
					parameters.add(key, value);
				}
			}
		});
		return parameters;
	}
}
