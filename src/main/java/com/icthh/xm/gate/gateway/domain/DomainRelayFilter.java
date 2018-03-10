package com.icthh.xm.gate.gateway.domain;

import static com.icthh.xm.gate.config.Constants.HEADER_DOMAIN;
import static com.icthh.xm.gate.config.Constants.HEADER_PORT;
import static com.icthh.xm.gate.config.Constants.HEADER_SCHEME;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static com.icthh.xm.gate.config.Constants.HEADER_WEBAPP_URL;

import com.icthh.xm.gate.service.TenantMappingService;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;

/**
 * Zuul filter to proxy subdomain to UAA.
 */
@Slf4j
@AllArgsConstructor
@Component
public class DomainRelayFilter extends ZuulFilter {

    private final TenantMappingService tenantMappingService;

    @Override
    public Object run() {
        try {

            RequestContext ctx = RequestContext.getCurrentContext();
            String protocol = ctx.getRequest().getScheme();
            ctx.addZuulRequestHeader(HEADER_SCHEME, protocol);
            String domain = ctx.getRequest().getServerName();
            ctx.addZuulRequestHeader(HEADER_DOMAIN, domain);
            String port = String.valueOf(ctx.getRequest().getServerPort());
            ctx.addZuulRequestHeader(HEADER_PORT, port);

            ctx.addZuulRequestHeader(HEADER_TENANT, tenantMappingService.getTenantKey(domain));
            ctx.addZuulRequestHeader(HEADER_WEBAPP_URL, getRefererUri(ctx.getRequest()));

            return null;
        } catch (Exception e) {
            log.error("Exception during filtering", e);
            return null;
        }
    }

    private static String getRefererUri(HttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (StringUtils.isNotBlank(referer)) {
            try {
                URL url = new URL(referer);
                URI uri = new URI(url.getProtocol(), url.getAuthority(), null, null, null);
                return uri.toString();
            } catch (Exception e) {
                log.debug("Error while converting referer header to URI, referer={}", referer, e);
            }
        }
        return null;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

}
