package com.icthh.xm.gate.gateway.domain;

import static com.icthh.xm.gate.config.Constants.HEADER_DOMAIN;
import static com.icthh.xm.gate.config.Constants.HEADER_PORT;
import static com.icthh.xm.gate.config.Constants.HEADER_SCHEME;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static com.icthh.xm.gate.config.Constants.HEADER_WEBAPP_URL;

import com.icthh.xm.gate.service.TenantMappingService;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

/**
 * Zuul filter to proxy subdomain to UAA.
 */
@Slf4j
@AllArgsConstructor
@Component
public class DomainRelayFilter extends ZuulFilter {

    private static final String DEFAULT_TENANT = "XM";

    private final TenantMappingService tenantMappingService;

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        String protocol = ctx.getRequest().getScheme();
        ctx.addZuulRequestHeader(HEADER_SCHEME, protocol);
        String domain = ctx.getRequest().getServerName();
        ctx.addZuulRequestHeader(HEADER_DOMAIN, domain);
        String port = String.valueOf(ctx.getRequest().getServerPort());
        ctx.addZuulRequestHeader(HEADER_PORT, port);

        String tenant = tenantMappingService.getTenants().get(domain);
        if (StringUtils.isBlank(tenant)) {
            log.debug("Domain Proxy Filter: no mapping for domain: {}", domain);
            tenant = DEFAULT_TENANT;
        }
        ctx.addZuulRequestHeader(HEADER_TENANT, tenant);
        String referer = ctx.getRequest().getHeader("referer");
        String webapp = null;
        try {
            URL url = new URL(referer);
            URI uri = new URI(url.getProtocol(), url.getAuthority(), null, null, null);
            webapp = uri.toString();
        } catch (Exception e) {
            log.debug("Error while running", e);
        }
        ctx.addZuulRequestHeader(HEADER_WEBAPP_URL, webapp);

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
