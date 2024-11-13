package com.icthh.xm.gate.utils;

import org.slf4j.MDC;

public final class MdcMonitoringUtils {

    public static final String HTTP_RESPONSE_STATUS_CODE = "http.response.status_code";
    public static final String HTTP_REQUEST_METHOD = "http.request.method";
    public static final String EVENT_DURATION = "event.duration";
    public static final String CLIENT_USER_NAME = "client.user.name";
    public static final String URL_PATH = "url.path";

    public static void setRequestMethod(String method) {
        MDC.put(HTTP_REQUEST_METHOD, method);
    }

    public static void setResponseStatusCode(Integer responseStatus) {
        MDC.put(HTTP_RESPONSE_STATUS_CODE, String.valueOf(responseStatus));
    }

    public static void setRequestDuration(long requestDuration) {
        MDC.put(EVENT_DURATION, String.valueOf(requestDuration));
    }

    public static void setClientUserName(String clientId) {
        MDC.put(CLIENT_USER_NAME, clientId);
    }

    public static void setUrlPath(String urlPath) {
        MDC.put(URL_PATH, urlPath);
    }

    public static void setMonitoringKeys(String requestMethod, Integer responseStatus, long requestDuration,
                                         String clientId, String urlPath) {
        setRequestMethod(requestMethod);
        setResponseStatusCode(responseStatus);
        setRequestDuration(requestDuration);
        setClientUserName(clientId);
        setUrlPath(urlPath);
    }

    public static void clearMonitoringKeys() {
        MDC.remove(HTTP_RESPONSE_STATUS_CODE);
        MDC.remove(HTTP_REQUEST_METHOD);
        MDC.remove(EVENT_DURATION);
        MDC.remove(CLIENT_USER_NAME);
        MDC.remove(URL_PATH);
    }
}
