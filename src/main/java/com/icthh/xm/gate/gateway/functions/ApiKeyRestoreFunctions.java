package com.icthh.xm.gate.gateway.functions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static com.icthh.xm.gate.config.Constants.HEADER_X_API_KEY;
import static com.icthh.xm.gate.utils.ServerRequestUtils.BEARER_PREFIX;

/**
 * Restores original API key into Authorization header after internal JWT substitution.
 */
@Slf4j
public class ApiKeyRestoreFunctions {

    public static HandlerFilterFunction<ServerResponse, ServerResponse> restoreApiKey() {
        return (request, next) -> {
            String apiKey = request.headers().firstHeader(HEADER_X_API_KEY);

            if (apiKey == null || apiKey.isBlank()) {
                log.debug("Skip API key restoration: header '{}' is missing for {} {}",
                    HEADER_X_API_KEY, request.method(), request.path());

                return next.handle(request);
            }

            log.debug("Restoring API key Authorization header for {} {}", request.method(), request.path());

            ServerRequest modifiedRequest = ServerRequest.from(request)
                .headers(headers -> {
                    headers.remove(HttpHeaders.AUTHORIZATION);
                    headers.add(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + apiKey);
                }).build();

            return next.handle(modifiedRequest);
        };
    }

    public static class FilterSupplier extends SimpleFilterSupplier {
        public FilterSupplier() {
            super(ApiKeyRestoreFunctions.class);
        }
    }
}
