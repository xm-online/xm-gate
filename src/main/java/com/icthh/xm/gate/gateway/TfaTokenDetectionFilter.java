package com.icthh.xm.gate.gateway;

import static com.icthh.xm.gate.config.Constants.FILTER_ORDER_TFA_TOKEN_DETECTION;

import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The {@link TfaTokenDetectionFilter} class.
 */
@Order(FILTER_ORDER_TFA_TOKEN_DETECTION)
@Component
public class TfaTokenDetectionFilter implements Filter {

    private final TokenStore tokenStore;
    private TokenExtractor tokenExtractor = new BearerTokenExtractor();

    public TfaTokenDetectionFilter(TokenStore tokenStore) {
        this.tokenStore = Objects.requireNonNull(tokenStore, "tokenStore can't be null");
    }

    public void setTokenExtractor(TokenExtractor tokenExtractor) {
        this.tokenExtractor = Objects.requireNonNull(tokenExtractor, "tokenExtractor can't be null");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nop
    }

    @Override
    public void destroy() {
        // nop
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            if (shouldFilter(httpRequest)) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                                       "Forbidden, inappropriate TFA access token use");
                return;
            }
        }

        // continue execution
        chain.doFilter(request, response);
    }

    private boolean shouldFilter(HttpServletRequest request) {
        // don't check "/uaa/oauth/token" path
        if ("/uaa/oauth/token".equals(request.getRequestURI())) {
            return false;
        }

        Authentication auth = tokenExtractor.extract(request);
        if (auth == null) {
            return false;
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof String)) {
            return false;
        }

        String tokenValue = (String) principal;

        // check is this TFA access token
        OAuth2AccessToken oAuth2AccessToken = tokenStore.readAccessToken(tokenValue);
        return isTfaAccessToken(oAuth2AccessToken);
    }

    private static boolean isTfaAccessToken(OAuth2AccessToken oAuth2AccessToken) {
        Map<String, Object> additionalInformation = oAuth2AccessToken.getAdditionalInformation();
        return !CollectionUtils.isEmpty(additionalInformation)
            && additionalInformation.containsKey("tfaVerificationKey");
    }

}
