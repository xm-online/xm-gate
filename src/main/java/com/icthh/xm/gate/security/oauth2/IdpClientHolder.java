package com.icthh.xm.gate.security.oauth2;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import org.springframework.stereotype.Component;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdpClientHolder implements ClientRegistrationRepository {

    private final TenantContextHolder tenantContextHolder;

    private final Map<String, Map<String, ClientRegistration>> clientsHolder = new HashMap<>();

    /**
     * Register IDP clients for specified tenant
     *
     * @param tenantKey     Tenant key
     * @param registrations IDP clients
     */
    public void setRegistrations(String tenantKey, List<ClientRegistration> registrations) {
        Assert.notEmpty(registrations, "registrations cannot be empty");
        this.clientsHolder.put(tenantKey.toLowerCase(), createClientRegistrationIdToClientRegistration(registrations));
        log.info("IDP clients for tenant [{}] registered", tenantKey);
    }

    public void removeTenantClientRegistrations(String tenantKey) {
        clientsHolder.remove(tenantKey.toLowerCase());
    }

    private static Map<String, ClientRegistration> createClientRegistrationIdToClientRegistration(
        Collection<ClientRegistration> registrations) {
        return Collections.unmodifiableMap(registrations
            .stream()
            .peek(registration -> Assert.notNull(registration, "registrations cannot contain null values"))
            .collect(Collectors.toMap(ClientRegistration::getRegistrationId, Function.identity())));
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");

        String tenantKey = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder).toLowerCase();
        Map<String, ClientRegistration> tenantClientsRegistration = clientsHolder.get(tenantKey);

        if (CollectionUtils.isEmpty(tenantClientsRegistration)) {
            log.info("IDP clients for tenant [{}] not registered", tenantKey);
            return null;
        }

        ClientRegistration clientRegistration = tenantClientsRegistration.get(registrationId.toLowerCase());
        if (clientRegistration == null) {
            log.info("IDP client with registrationId [{}] for tenant [{}] not found", registrationId, tenantKey);
        }
        return clientRegistration;
    }

    /**
     * Search all client registrations by tenant key
     *
     * @param tenantKey Tenant key
     * @return Returns client registrations for specified tenant if they are present. Otherwise returns null
     */
    public Map<String, ClientRegistration> findByTenantKey(String tenantKey) {
        Map<String, ClientRegistration> tenantClientsRegistration = clientsHolder.get(tenantKey.toLowerCase());

        if (CollectionUtils.isEmpty(tenantClientsRegistration)) {
            log.info("IDP clients for tenant [{}] not registered", tenantKey);
        }
        return tenantClientsRegistration;
    }

}
