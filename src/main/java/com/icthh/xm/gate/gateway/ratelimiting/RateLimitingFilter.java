package com.icthh.xm.gate.gateway.ratelimiting;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.gate.domain.properties.TenantProperties;
import com.icthh.xm.gate.service.TenantPropertiesService;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import io.github.bucket4j.*;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.jcache.JCache;
import io.github.jhipster.config.JHipsterProperties;

import java.time.Duration;
import java.util.Map;
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
import org.apache.commons.collections.MapUtils;
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

    private final JHipsterProperties jhipsterProperties;

    private final XmAuthenticationContextHolder authenticationContextHolder;

    private final TenantPropertiesService tenantPropertiesService;

    private ProxyManager<String> buckets;

    public RateLimitingFilter(JHipsterProperties jhipsterProperties,
                              XmAuthenticationContextHolder authenticationContextHolder,
                              TenantPropertiesService tenantPropertiesService) {
        this.jhipsterProperties = jhipsterProperties;
        this.authenticationContextHolder = authenticationContextHolder;
        this.tenantPropertiesService = tenantPropertiesService;

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

        String bucketId = getId(RequestContext.getCurrentContext().getRequest());
        Bucket bucket = buckets.getProxy(bucketId, getConfigSupplier(bucketId));
        if (bucket.tryConsume(1)) {
            // the limit is not exceeded
            log.debug("API rate limit OK for id {}", bucketId);
        } else {
            // limit is exceeded
            log.info("API rate limit exceeded for id {}", bucketId);
            apiLimitExceeded();
        }
        return null;
    }

    private Supplier<BucketConfiguration> getConfigSupplier(String clientId) {
        return () -> {
            JHipsterProperties.Gateway.RateLimiting rateLimitingProperties =
                jhipsterProperties.getGateway().getRateLimiting();
            long limit = rateLimitingProperties.getLimit();
            int duration = rateLimitingProperties.getDurationInSeconds();

            if (applyOauth2ClientRule()) {
                limit = getTenantRateLimitingConfig().get(clientId).getLimit();
                duration = getTenantRateLimitingConfig().get(clientId).getDurationInSeconds();
            }

            return Bucket4j.configurationBuilder()
                .addLimit(Bandwidth.simple(limit,
                    Duration.ofSeconds(duration)))
                .build();
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
    private String getId(HttpServletRequest httpServletRequest) {
        String id = authenticationContextHolder.getContext().getLogin().orElse(httpServletRequest.getRemoteAddr());
        Optional<String> client = getClientId();

        if (applyOauth2ClientRule() && client.isPresent()) {
            id = client.get();
        }

        return id;
    }

    private boolean applyOauth2ClientRule() {
        boolean apply = false;

        Optional<String> client = getClientId();
        if (client.isPresent()) {
            if (MapUtils.isNotEmpty(getTenantRateLimitingConfig())
                && getTenantRateLimitingConfig().containsKey(client.get())) {
                apply = true;
            }
        }

        return apply;
    }

    private Optional<String> getClientId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();

        if (a instanceof OAuth2Authentication) {
            String clientId = ((OAuth2Authentication) a).getOAuth2Request().getClientId();
            return Optional.of(clientId);
        }

        return Optional.empty();
    }

    private Map<String, TenantProperties.RateLimiting.RateLimitingConf> getTenantRateLimitingConfig() {
        return tenantPropertiesService.getTenantProps().getRateLimiting().getOauth2Client();
    }
}
