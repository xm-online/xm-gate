package com.icthh.xm.gate.web.rest;

import com.icthh.xm.commons.gen.api.TenantsApiDelegate;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenantendpoint.TenantManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantResource implements TenantsApiDelegate {

    private final TenantManager tenantManager;

    @Override
    @PreAuthorize("hasPermission({'tenant':#tenant}, 'GATE.TENANT.CREATE')")
    public ResponseEntity<Void> addTenant(Tenant tenant) {
        tenantManager.createTenant(tenant);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasPermission({'tenantKey':#tenantKey}, 'GATE.TENANT.DELETE')")
    public ResponseEntity<Void> deleteTenant(String tenantKey) {
        tenantManager.deleteTenant(tenantKey.toLowerCase());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @PostAuthorize("hasPermission(null, 'GATE.TENANT.GET_LIST')")
    public ResponseEntity<List<Tenant>> getAllTenantInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'GATE.TENANT.GET_LIST.ITEM')")
    public ResponseEntity<Tenant> getTenant(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    @PreAuthorize("hasPermission({'tenant':#tenant, 'state':#state}, 'GATE.TENANT.UPDATE')")
    public ResponseEntity<Void> manageTenant(String tenantKey, String state) {
        tenantManager.manageTenant(tenantKey, state);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
