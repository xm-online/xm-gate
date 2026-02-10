package com.icthh.xm.gate.config;

import lombok.NoArgsConstructor;
import org.springframework.core.Ordered;

/**
 * Application constants.
 */
@NoArgsConstructor
public final class Constants {

    public static final String SPRING_PROFILE_DEVELOPMENT = "dev";
    public static final String SPRING_PROFILE_PRODUCTION = "prod";
    public static final String SPRING_PROFILE_CLOUD = "cloud";
    public static final String SPRING_PROFILE_API_DOCS = "api-docs";

    public static final String HEADER_SCHEME = "x-scheme";
    public static final String HEADER_DOMAIN = "x-domain";
    public static final String HEADER_PORT = "x-port";
    public static final String HEADER_TENANT = "x-tenant";
    public static final String HEADER_WEBAPP_URL = "x-webapp-url";

    public static final String AUTH_RESPONSE_FIELD_IDP_TOKEN = "idp_id_token";
    public static final String AUTH_RESPONSE_FIELD_IDP_ACCESS_TOKEN_INCLUSION = "idpAccessTokenInclusion";

    public static final String CERTIFICATE = "X.509";
    public static final String PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----%n%s%n-----END PUBLIC KEY-----";

    public static final String DEFAULT_TENANT = "XM";

    public static final int FILTER_ORDER_TENANT_INIT = Ordered.HIGHEST_PRECEDENCE;
    public static final int FILTER_ORDER_IDP_STATE_FULL_MODE = 10000;
    public static final int FILTER_ORDER_SWAGGER_BASE_PATH_REWRITING = 100;
    public static final int FILTER_ORDER_DOMAIN_RELAY = 0;
    public static final int FILTER_ORDER_ACCESS_CONTROL = 0;
    public static final int FILTER_ORDER_RATE_LIMITING = 10;
    public static final int FILTER_ORDER_CUSTOM_ERROR = -1;
    public static final int FILTER_ORDER_TFA_TOKEN_DETECTION = FILTER_ORDER_TENANT_INIT + 1;

    public static final String JSESSIONID_COOKIE_NAME = "JSESSIONID";

    public static final String UPLOAD_PREFIX = "/upload/";
}
