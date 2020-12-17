package com.icthh.xm.gate.security.oauth2;

public class IdpUtils {

    public static final String KEY_SEPARATOR = "_";

    public static String buildCompositeIdpKey(String tenantKey, String idpKey) {
        return (tenantKey + KEY_SEPARATOR + idpKey).toLowerCase();
    }
}
