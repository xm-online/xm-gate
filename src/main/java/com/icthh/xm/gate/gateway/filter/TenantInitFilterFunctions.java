package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.gate.service.TenantMappingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static com.icthh.xm.gate.config.Constants.DEFAULT_TENANT;

/**
 * Filter for setting {@link TenantContextHolder}.
 */
@Slf4j
public class TenantInitFilterFunctions {

    public static HandlerFilterFunction<ServerResponse, ServerResponse> tenantInit() {
        return (request, next) -> {
            HttpServletRequest servletRequest = request.servletRequest();

            TenantMappingService tenantMappingService = MvcUtils.getApplicationContext(request)
                .getBean(TenantMappingService.class);
            TenantContextHolder tenantContextHolder = MvcUtils.getApplicationContext(request)
                .getBean(TenantContextHolder.class);

            String domain = servletRequest.getServerName();
            String tenantKeyValue = tenantMappingService.getTenantKey(domain);

            if (!tenantMappingService.isTenantPresent(tenantKeyValue) && !DEFAULT_TENANT.equalsIgnoreCase(tenantKeyValue)) {
                log.error("Tenant {} is not present", tenantKeyValue);
                return errorResponse("SERVICE-NOT-FOUND");
            }

            if (!tenantMappingService.isTenantActive(tenantKeyValue) && !DEFAULT_TENANT.equalsIgnoreCase(tenantKeyValue)) {
                log.error("Tenant {} is not active", tenantKeyValue);
                return errorResponse("SERVICE-SUSPENDED");
            }

            log.info("Set tenant '{}' to tenant context holder", tenantKeyValue);
            TenantContextUtils.setTenant(tenantContextHolder, tenantKeyValue);
            try {
                return next.handle(request);

            } finally {
                String tenantKey = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
                log.info("Destroy tenant context holder with value '{}' ", tenantKey);
                tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
            }
        };
    }

    private static ServerResponse errorResponse(String code) {
        return ServerResponse.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"error\": \"" + code + "\"}");
    }

    public static class FilterSupplier extends SimpleFilterSupplier {
        public FilterSupplier() {
            super(TenantInitFilterFunctions.class);
        }
    }
}
