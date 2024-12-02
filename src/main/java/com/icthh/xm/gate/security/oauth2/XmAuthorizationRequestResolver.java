package com.icthh.xm.gate.security.oauth2;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * A custom implementation of {@link OAuth2AuthorizationRequestResolver} that modifies
 * the OAuth2 authorization request before redirecting to the authorization server.
 * <p>
 * This resolver appends custom query parameters to the redirect URI dynamically
 * based on the incoming HTTP request.
 */
public class XmAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public XmAuthorizationRequestResolver(ClientRegistrationRepository repo,
                                          String authorizationRequestBaseUri) {

        defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        if(authorizationRequest != null) {
            authorizationRequest = customizeAuthorizationRequest(authorizationRequest, request);
        }
        return authorizationRequest;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return defaultResolver.resolve(request, clientRegistrationId);
    }

    /**
     * Customizes the given {@link OAuth2AuthorizationRequest}.
     *
     * @param authorizationRequest the original {@link OAuth2AuthorizationRequest} resolved by the default resolver
     * @param request the current {@link HttpServletRequest}
     * @return the customized {@link OAuth2AuthorizationRequest}
     */
    private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                                                     HttpServletRequest request) {
        Map<String, String> queryParams = getNormalizedParametrMap(request);

        String redirectUri = getAppendedRedirectUri(authorizationRequest, queryParams);
        Map<String, Object> attributes = getAppendedAttributes(authorizationRequest, queryParams);

        return OAuth2AuthorizationRequest.from(authorizationRequest)
            .attributes(attributes)
            .redirectUri(redirectUri)
            .build();
    }

    private Map<String, String> getNormalizedParametrMap(HttpServletRequest request) {
        return request.getParameterMap().entrySet()
            .stream().collect(Collectors.toMap(Map.Entry::getKey, it -> String.join(",", it.getValue())));
    }

    private String getAppendedRedirectUri(OAuth2AuthorizationRequest authorizationRequest, Map<String, String> queryParams) {
        String redirectUri = authorizationRequest.getRedirectUri();

        if (isEmpty(queryParams)) {
            return redirectUri;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(redirectUri);
        queryParams.forEach(builder::queryParam);
        return builder.toUriString();
    }

    private Map<String, Object> getAppendedAttributes(OAuth2AuthorizationRequest authorizationRequest, Map<String, String> queryParams) {
        Map<String, Object> attributes = new HashMap<>(authorizationRequest.getAttributes());
        attributes.putAll(queryParams);
        return Collections.unmodifiableMap(attributes);
    }

}
