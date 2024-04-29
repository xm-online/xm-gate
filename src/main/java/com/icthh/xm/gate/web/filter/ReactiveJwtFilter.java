package com.icthh.xm.gate.web.filter;

import com.icthh.xm.commons.security.jwt.TokenProvider;
import com.icthh.xm.gate.utils.ServerRequestUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ReactiveJwtFilter implements WebFilter {

    private final TokenProvider tokenProvider;

    public ReactiveJwtFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String jwt = ServerRequestUtils.resolveTokenFromRequest(request);

        if (StringUtils.hasText(jwt) && this.tokenProvider.validateToken(jwt)) {
            return Mono.fromCallable(() -> this.tokenProvider.getAuthentication(request, jwt))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(authentication -> {
                    return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                });
        }

        return chain.filter(exchange);
    }
}
