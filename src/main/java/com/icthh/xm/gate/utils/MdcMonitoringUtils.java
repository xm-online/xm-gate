package com.icthh.xm.gate.utils;

import org.slf4j.MDC;

public final class MdcMonitoringUtils {

    public static final String HTTP_RESPONSE_STATUS_CODE = "http.response.status_code";
    public static final String HTTP_REQUEST_METHOD = "http.request.method";
    public static final String EVENT_DURATION = "event.duration";

    public static void setRequestMethod(String method) {
        MDC.put(HTTP_REQUEST_METHOD, method);
    }

    public static void setResponseStatusCode(Integer responseStatus) {
        MDC.put(HTTP_RESPONSE_STATUS_CODE, String.valueOf(responseStatus));
    }

    public static void setRequestDuration(long requestDuration) {
        MDC.put(EVENT_DURATION, String.valueOf(requestDuration));
    }

    public static void setMonitoringKeys(String requestMethod, Integer responseStatus, long requestDuration) {
        setRequestMethod(requestMethod);
        setResponseStatusCode(responseStatus);
        setRequestDuration(requestDuration);
    }

    public static void clearMonitoringKeys() {
        MDC.remove(HTTP_RESPONSE_STATUS_CODE);
        MDC.remove(HTTP_REQUEST_METHOD);
        MDC.remove(EVENT_DURATION);
    }
}
