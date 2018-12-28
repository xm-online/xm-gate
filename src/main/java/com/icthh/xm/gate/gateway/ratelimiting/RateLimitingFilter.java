package com.icthh.xm.gate.gateway.ratelimiting;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.gate.config.ApplicationProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import io.github.bucket4j.*;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.jcache.JCache;
import io.github.jhipster.config.JHipsterProperties;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

/**
 * Zuul filter for limiting the number of HTTP calls per client.
 * See the Bucket4j documentation at https://github.com/vladimir-bukhtoyarov/bucket4j
 * https://github.com/vladimir-bukhtoyarov/bucket4j/blob/master/doc-pages/jcache-usage
 * .md#example-1---limiting-access-to-http-server-by-ip-address
 */
@Slf4j
public class RateLimitingFilter extends ZuulFilter {

    private static final String GATEWAY_RATE_LIMITING_CACHE_NAME = "gateway-rate-limiting";

    private static final long BASE_REQUESTS_LIMIT = 100000;

    private static final int BASE_DURATION_IN_SECONDS = 3600;

    private final JHipsterProperties jhipsterProperties;

    private final XmAuthenticationContextHolder authenticationContextHolder;

    private final ApplicationProperties applicationProperties;

    private ProxyManager<String> buckets;

    public RateLimitingFilter(JHipsterProperties jhipsterProperties,
                              XmAuthenticationContextHolder authenticationContextHolder,
                              ApplicationProperties applicationProperties) {
        this.jhipsterProperties = jhipsterProperties;
        this.authenticationContextHolder = authenticationContextHolder;
        this.applicationProperties = applicationProperties;

        CachingProvider cachingProvider = Caching.getCachingProvider();
        CacheManager cacheManager = cachingProvider.getCacheManager();
        CompleteConfiguration<String, GridBucketState> config =
            new MutableConfiguration<String, GridBucketState>()
                .setTypes(String.class, GridBucketState.class);

        Cache<String, GridBucketState> cache = cacheManager.createCache(GATEWAY_RATE_LIMITING_CACHE_NAME, config);
        this.buckets = Bucket4j.extension(JCache.class).proxyManagerForCache(cache);
    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        // specific APIs can be filtered out using
        // if (RequestContext.getCurrentContext().getRequest().getRequestURI().startsWith("/api")) { ... }
        return true;
    }

    @Override
    public Object run() {
        boolean byClientId = false;
        if (StringUtils.isNotBlank(applicationProperties.getClientId())) {
            byClientId = true;
        }
        String bucketId = getId(RequestContext.getCurrentContext().getRequest(), byClientId);
        Bucket bucket = buckets.getProxy(bucketId, getConfigSupplier(byClientId));
        if (bucket.tryConsume(1)) {
            // the limit is not exceeded
            log.debug("API rate limit OK for client id {}", bucketId);
        } else {
            // limit is exceeded
            log.info("API rate limit exceeded for client id {}", bucketId);
            apiLimitExceeded();
        }
        return null;
    }

    private Supplier<BucketConfiguration> getConfigSupplier(boolean byClientId) {
        return () -> {
            long limit = BASE_REQUESTS_LIMIT;
            int duration = BASE_DURATION_IN_SECONDS;

            if (byClientId) {
                JHipsterProperties.Gateway.RateLimiting rateLimitingProperties =
                    jhipsterProperties.getGateway().getRateLimiting();
                limit = rateLimitingProperties.getLimit();
                duration = rateLimitingProperties.getDurationInSeconds();
            }

            return Bucket4j.configurationBuilder()
                .addLimit(Bandwidth.simple(limit,
                    Duration.ofSeconds(duration)))
                .buildConfiguration();
        };
    }

    /**
     * Create a Zuul response error when the API limit is exceeded.
     */
    private static void apiLimitExceeded() {
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setResponseStatusCode(HttpStatus.TOO_MANY_REQUESTS.value());
        if (ctx.getResponseBody() == null) {
            ctx.setResponseBody("API rate limit exceeded");
            ctx.setSendZuulResponse(false);
        }
    }

    /**
     * The ID that will identify the limit: the client id or the user IP address.
     */
    private String getId(HttpServletRequest httpServletRequest, boolean byClientId) {

        if (byClientId) {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            if (a instanceof OAuth2Authentication) {
                Optional<String> clientId = Optional.of(((OAuth2Authentication) a).getOAuth2Request().getClientId());
                return clientId.orElse(httpServletRequest.getRemoteAddr());
            }
        }
        return authenticationContextHolder.getContext().getLogin().orElse(httpServletRequest.getRemoteAddr());
    }

}
