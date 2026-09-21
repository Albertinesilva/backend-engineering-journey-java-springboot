package com.albertsilva.dev.dscatalog.security.oauth2.grant.password;

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
 * Converte a requisição do <em>token endpoint</em> com {@code grant_type=password}
 * em um {@link CustomPasswordAuthenticationToken}. É o primeiro passo do login
 * ({@code POST /oauth2/token}), registrado em
 * {@code AuthorizationServerConfig}.
 *
 * <p>
 * <b>Comportamento de {@link #convert(HttpServletRequest)}:</b>
 * </p>
 * <ol>
 * <li>{@code grant_type} diferente de {@code password} (inclusive
 * {@code refresh_token}) ⇒ devolve {@code null}, deixando a requisição para os
 * outros conversores do framework;</li>
 * <li>{@code scope}: opcional; se informado, deve aparecer <b>uma única vez</b>;</li>
 * <li>{@code username} e {@code password}: obrigatórios, não em branco
 * ({@code StringUtils.hasText}) e cada um <b>uma única vez</b>;</li>
 * <li>violação de qualquer regra acima ⇒
 * {@link OAuth2AuthenticationException} com {@code invalid_request};</li>
 * <li>os escopos solicitados viram um {@code Set} (separados por espaço);
 * todos os <b>outros</b> parâmetros (por exemplo, {@code client_id}) vão para
 * {@code additionalParameters}, com o primeiro valor;</li>
 * <li>o cliente já autenticado é lido de {@code SecurityContextHolder} e passado
 * como principal do token.</li>
 * </ol>
 *
 * <p>
 * Não autentica o usuário nem consulta o banco; apenas extrai e valida a forma
 * dos parâmetros. Os valores {@code username} e {@code password} não recebem
 * {@code trim}. Os escopos solicitados são guardados no token, mas o provider
 * atual não os utiliza.
 * </p>
 */
public class CustomPasswordAuthenticationConverter implements AuthenticationConverter {

	/**
	 * Converte os parâmetros da requisição em {@link CustomPasswordAuthenticationToken},
	 * conforme descrito na documentação da classe.
	 *
	 * @param request requisição HTTP do token endpoint
	 * @return token de autenticação do grant {@code password}, ou {@code null} se o
	 *         {@code grant_type} não for {@code password}
	 * @throws OAuth2AuthenticationException ({@code invalid_request}) se
	 *                                       {@code username} ou {@code password}
	 *                                       faltarem, estiverem em branco ou
	 *                                       repetidos, ou se {@code scope} estiver
	 *                                       repetido
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
	 * Copia os parâmetros da requisição para um {@link MultiValueMap}, mantendo
	 * todos os valores de cada nome (necessário para detectar parâmetros
	 * repetidos).
	 *
	 * @param request requisição HTTP
	 * @return mapa com todos os parâmetros
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
