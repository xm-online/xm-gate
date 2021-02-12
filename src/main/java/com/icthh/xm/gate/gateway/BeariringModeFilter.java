package com.icthh.xm.gate.gateway;

import com.icthh.xm.commons.domain.idp.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.gate.security.oauth2.IdpConfigRepository;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import javax.servlet.http.HttpServletRequest;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;

@Component
@RequiredArgsConstructor
public class BeariringModeFilter extends ZuulFilter {

    private final TenantContextHolder tenantContextHolder;
    private final IdpConfigRepository idpConfigRepository;
    private final XmAuthenticationContextHolder authenticationContextHolder;
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public Object run() {
        String tenantKey = getRequiredTenantKeyValue(tenantContextHolder);

        XmAuthenticationContext xmAuthenticationContext = authenticationContextHolder.getContext();
        if (xmAuthenticationContext.hasAuthentication()) {
            IdpPublicClientConfig.Features features = getIdpClientConfig(tenantKey).getFeatures();

            if (features.isStateful()) {
                // TODO Stateful not implemented for now
                throw new UnsupportedOperationException("Stateful mode not supported yet");
            }
        }

        return null;
    }

    @Override
    public boolean shouldFilter() {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        String requestURI = request.getRequestURI();
        // check "/api/xm-ms-*" path          matcher.match("/entity/api/xm-entity-*", requestURI);
        return matcher.match("/entity/api/*", requestURI);
    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 10000;
    }

    private IdpPublicClientConfig getIdpClientConfig(String tenantKey) {
        return idpConfigRepository.getTenantIdpPublicClientConfig(tenantKey, "clientRegistrationId");
    }
}
