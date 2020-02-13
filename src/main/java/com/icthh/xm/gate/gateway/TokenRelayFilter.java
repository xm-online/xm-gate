package com.icthh.xm.gate.gateway;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TokenRelayFilter extends ZuulFilter {

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        @SuppressWarnings("unchecked")
        Set<String> headers = (Set<String>) ctx.get("ignoredHeaders");
        // We need our JWT tokens relayed to resource servers
        if (headers != null) {
            headers.remove("authorization");
            headers.remove("set-cookie");
            headers.remove("cookie");
        }

        return null;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 10000;
    }
}
