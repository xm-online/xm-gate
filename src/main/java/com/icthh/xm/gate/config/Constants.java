package com.icthh.xm.gate.config;

import org.springframework.core.Ordered;

/**
 * Application constants.
 */
public final class Constants {

    public static final String HEADER_SCHEME = "x-scheme";
    public static final String HEADER_DOMAIN = "x-domain";
    public static final String HEADER_PORT = "x-port";
    public static final String HEADER_TENANT = "x-tenant";
    public static final String HEADER_WEBAPP_URL = "x-webapp-url";

    public static final String AUTH_RESPONSE_FIELD_IDP_TOKEN = "idp_id_token";
    // FIXME: think how to rename the feature. Variants:
    //  IDP token exposure
    //  IDP Access token exposure
    //  IDP Access token inclusion
    //  IDP Access token communication
    //  IDP Access token transfer
    //  IDP Access token supplement
    //  IDP Access token extension
    //  IDP Access token injection
    public static final String AUTH_RESPONSE_FIELD_BEARING = "bearing";

    public static final String CERTIFICATE = "X.509";
    public static final String PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----%n%s%n-----END PUBLIC KEY-----";

    public static final String DEFAULT_TENANT = "XM";

    public static final int FILTER_ORDER_TENANT_INIT = Ordered.HIGHEST_PRECEDENCE;
    public static final int FILTER_ORDER_TFA_TOKEN_DETECTION = FILTER_ORDER_TENANT_INIT + 1;

    private Constants() {
    }
}
