package com.icthh.xm.gate.web.rest;

import static org.apache.commons.lang3.time.StopWatch.createStarted;

import com.icthh.xm.commons.gen.api.TenantsApiDelegate;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.gate.service.TenantMappingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class TenantResource implements TenantsApiDelegate {

    private final TenantMappingService tenantMappingService;

    @Override
    public ResponseEntity<Void> addTenant(Tenant tenant) {
        StopWatch stopWatch = createStarted();
        log.info("START - SETUP:CreateTenant: tenantKey: {}", tenant.getTenantKey());
        try {
            tenantMappingService.addTenant(tenant);
        } catch (Exception e) {
            log.error("STOP  - ERROR:CreateTenant: tenantKey: {}, result: {}", tenant.getTenantKey(), e);
            throw e;
        }
        log.info("STOP  - SETUP:CreateTenant:  tenantKey: {}, result: OK, time = {} ms",
            tenant.getTenantKey(), stopWatch.getTime());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteTenant(String tenantKey) {
        StopWatch stopWatch = createStarted();
        log.info("START - SETUP:DeleteTenant: tenantKey: {}", tenantKey);
        try {
            tenantMappingService.deleteTenant(tenantKey);
        } catch (Exception e) {
            log.error("STOP  - ERROR:DeleteTenant: tenantKey: {}, result: {}",tenantKey, e);
            throw e;
        }
        log.info("STOP  - SETUP:DeleteTenant: tenantKey: {}, result: OK, time = {} ms", tenantKey, stopWatch.getTime());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Tenant>> getAllTenantInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResponseEntity<Tenant> getTenant(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResponseEntity<Void> manageTenant(String tenantKey, String state) {
        StopWatch stopWatch = createStarted();
        log.info("START - SETUP:ManageTenant: tenantKey: {}, state: {}", tenantKey, state);
        try {
            tenantMappingService.manageTenant(tenantKey.toLowerCase(), state.toUpperCase());
        } catch (Exception e) {
            log.error("STOP  - ERROR:ManageTenant: tenantKey: {}, state: {}, result: {}",tenantKey, state, e);
            throw e;
        }
        log.info("STOP  - SETUP:ManageTenant: tenantKey: {}, state: {}, result: OK, time = {} ms", tenantKey, state, stopWatch.getTime());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
