package com.icthh.xm.gate.gateway.functions;

import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.gate.security.oauth2.idp.IdpConfigRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;

@Slf4j
public class IdpStatefulModeFilterFunctions {

    // todo: ignore filter until implemented
    public static HandlerFilterFunction<ServerResponse, ServerResponse> idpStatefulMode() {
        return (request, next) -> {
            HttpServletRequest servletRequest = request.servletRequest();

            if (shouldNotFilter(servletRequest)) {
                return next.handle(request);
            }

            TenantContextHolder tenantContextHolder = MvcUtils.getApplicationContext(request)
                .getBean(TenantContextHolder.class);
            IdpConfigRepository idpConfigRepository = MvcUtils.getApplicationContext(request)
                .getBean(IdpConfigRepository.class);
            XmAuthenticationContextHolder authenticationContextHolder = MvcUtils.getApplicationContext(request)
                .getBean(XmAuthenticationContextHolder.class);

            XmAuthenticationContext xmAuthenticationContext = authenticationContextHolder.getContext();

            if (xmAuthenticationContext.hasAuthentication()) {
                String tenantKey = getRequiredTenantKeyValue(tenantContextHolder);
                IdpPublicConfig.IdpConfigContainer.Features features = idpConfigRepository.getTenantFeatures(tenantKey);

                log.debug("Idp stateful mode enabled: {}", features.isStateful());
                if (features.isStateful()) {
                    // TODO Stateful not implemented for now
                    throw new UnsupportedOperationException("Stateful mode not supported yet");
                }
            }

            return next.handle(request);
        };
    }

    private static boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().contains("/api/");
    }

    public static class FilterSupplier extends SimpleFilterSupplier {
        public FilterSupplier() {
            super(IdpStatefulModeFilterFunctions.class);
        }
    }
}
