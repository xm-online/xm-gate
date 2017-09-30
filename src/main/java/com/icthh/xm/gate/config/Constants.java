package com.icthh.xm.gate.config;

/**
 * Application constants.
 */
public final class Constants {

    public static final String HEADER_SCHEME = "x-scheme";
    public static final String HEADER_DOMAIN = "x-domain";
    public static final String HEADER_PORT = "x-port";
    public static final String HEADER_TENANT = "x-tenant";
    public static final String HEADER_WEBAPP_URL = "x-webapp-url";

    public static final String CERTIFICATE = "X.509";
    public static final String PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----%n%s%n-----END PUBLIC KEY-----";

    private Constants() {
    }
}
