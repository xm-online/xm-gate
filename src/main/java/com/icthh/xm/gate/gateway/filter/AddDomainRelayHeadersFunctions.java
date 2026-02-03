package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.gate.service.TenantMappingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.net.URL;
import java.util.Optional;

import static com.icthh.xm.gate.config.Constants.HEADER_DOMAIN;
import static com.icthh.xm.gate.config.Constants.HEADER_PORT;
import static com.icthh.xm.gate.config.Constants.HEADER_SCHEME;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static com.icthh.xm.gate.config.Constants.HEADER_WEBAPP_URL;

@Slf4j
public class AddDomainRelayHeadersFunctions {

    public static HandlerFilterFunction<ServerResponse, ServerResponse> addDomainRelayHeaders() {
        return (request, next) -> {
            log.debug("Domain rel");
            try {
                HttpServletRequest servletRequest = request.servletRequest();

                TenantMappingService tenantMappingService = MvcUtils.getApplicationContext(request)
                    .getBean(TenantMappingService.class);
                String tenantKey = tenantMappingService.getTenantKey(servletRequest.getServerName());

                log.debug("Set domain related filters, tenant={}", tenantKey);
                ServerRequest.Builder builder = ServerRequest.from(request)
                    .header(HEADER_SCHEME, servletRequest.getScheme())
                    .header(HEADER_DOMAIN, servletRequest.getServerName())
                    .header(HEADER_PORT, String.valueOf(servletRequest.getServerPort()))
                    .header(HEADER_TENANT, tenantKey);

                getRefererUri(servletRequest).ifPresent(uri -> builder.header(HEADER_WEBAPP_URL, uri));

                return next.handle(builder.build());

            } catch (Exception e) {
                log.error("Exception during domain relay filtering", e);
                return next.handle(request);
            }
        };
    }

    private static Optional<String> getRefererUri(HttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (StringUtils.isBlank(referer)) {
            return Optional.empty();
        }
        try {
            URL url = new URL(referer);
            URI uri = new URI(url.getProtocol(), url.getAuthority(), null, null, null);
            return Optional.of(uri.toString());
        } catch (Exception e) {
            log.debug("Error while converting referer header to URI, referer={}", referer, e);
            return Optional.empty();
        }
    }

    public static class FilterSupplier extends SimpleFilterSupplier {
        public FilterSupplier() {
            super(AddDomainRelayHeadersFunctions.class);
        }
    }
}
