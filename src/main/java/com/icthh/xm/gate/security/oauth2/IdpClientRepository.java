package com.icthh.xm.gate.security.oauth2;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;

import com.icthh.xm.commons.tenant.TenantContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
public class IdpClientRepository implements ClientRegistrationRepository {

    private final TenantContextHolder tenantContextHolder;

    private final Map<String, Map<String, ClientRegistration>> clientsHolder = new ConcurrentHashMap<>();

    /**
     * Register IDP clients for specified tenant
     *
     * @param tenantKey     Tenant key
     * @param registrations IDP clients
     */
    public void setRegistrations(String tenantKey, List<ClientRegistration> registrations) {
        Assert.notEmpty(registrations, "registrations cannot be empty");
        this.clientsHolder.put(tenantKey, mapRegistrationIdToClientRegistration(registrations));

        Map<String, String> clientsName = registrations.stream()
            .collect(Collectors.toMap(ClientRegistration::getRegistrationId, ClientRegistration::getClientName));

        log.info("IDP clients for tenant [{}] registered: {}", tenantKey, clientsName);
    }

    public void removeTenantClientRegistrations(String tenantKey) {
        clientsHolder.remove(tenantKey);
    }

    private static Map<String, ClientRegistration> mapRegistrationIdToClientRegistration(
        Collection<ClientRegistration> registrations) {
        return registrations
            .stream()
            .peek(registration -> Assert.notNull(registration, "registrations cannot contain null values"))
            .collect(Collectors.toUnmodifiableMap(ClientRegistration::getRegistrationId, Function.identity()));
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");

        String tenantKey = getRequiredTenantKeyValue(tenantContextHolder);
        Map<String, ClientRegistration> tenantClientsRegistration = clientsHolder.get(tenantKey);

        if (CollectionUtils.isEmpty(tenantClientsRegistration)) {
            log.warn("IDP clients for tenant [{}] not registered", tenantKey);
            return null;
        }

        ClientRegistration clientRegistration = tenantClientsRegistration.get(registrationId);
        if (clientRegistration == null) {
            log.warn("IDP client with registrationId [{}] for tenant [{}] not found", registrationId, tenantKey);
        }
        return clientRegistration;
    }

    /**
     * Search all client registrations by tenant key
     *
     * @param tenantKey Tenant key
     * @return Returns client registrations for specified tenant if they are present. Otherwise returns null
     */
    Map<String, ClientRegistration> findByTenantKey(String tenantKey) {
        Map<String, ClientRegistration> tenantClientsRegistration = clientsHolder.get(tenantKey);

        if (CollectionUtils.isEmpty(tenantClientsRegistration)) {
            log.warn("IDP clients for tenant [{}] not registered", tenantKey);
        }
        return tenantClientsRegistration;
    }
}
