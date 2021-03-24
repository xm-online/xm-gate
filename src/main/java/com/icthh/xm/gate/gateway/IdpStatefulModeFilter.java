package com.icthh.xm.gate.gateway;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;

import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.Features;

import com.icthh.xm.commons.security.XmAuthenticationContext;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.gate.security.oauth2.IdpConfigRepository;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdpStatefulModeFilter extends ZuulFilter {

    private static final int FILTER_ORDER_PRIORITY = 10000;//filter order priority
    private final TenantContextHolder tenantContextHolder;
    private final IdpConfigRepository idpConfigRepository;
    private final XmAuthenticationContextHolder authenticationContextHolder;

    @Override
    public Object run() {
        XmAuthenticationContext xmAuthenticationContext = authenticationContextHolder.getContext();

        if (xmAuthenticationContext.hasAuthentication()) {
            String tenantKey = getRequiredTenantKeyValue(tenantContextHolder);
            Features features = idpConfigRepository.getTenantFeatures(tenantKey);

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
        return requestURI.contains("/api/");
    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return FILTER_ORDER_PRIORITY;
    }

}
