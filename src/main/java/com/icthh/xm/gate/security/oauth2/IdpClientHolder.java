package com.icthh.xm.gate.security.oauth2;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;

import com.icthh.xm.commons.tenant.TenantContextHolder;

import java.util.Collection;
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
// FIXME - suggest renaming to IdpClientRepository
// FIXME: Why we do not have a unit test for this class?
public class IdpClientHolder implements ClientRegistrationRepository {

    private final TenantContextHolder tenantContextHolder;

    // FIXME: seems it worth to use ConcurrentHashMap here.
    private final Map<String, Map<String, ClientRegistration>> clientsHolder = new HashMap<>();

    /**
     * Register IDP clients for specified tenant
     *
     * @param tenantKey     Tenant key
     * @param registrations IDP clients
     */
    public void setRegistrations(String tenantKey, List<ClientRegistration> registrations) {
        Assert.notEmpty(registrations, "registrations cannot be empty");
        this.clientsHolder.put(tenantKey, createClientRegistrationIdToClientRegistration(registrations));
        //FIXME: suggest add names of the registered clients:
        //  log.info("IDP clients for tenant [{}] registered: [{}]", tenantKey, registrations.stream().map(ClientRegistration::getClientName).collect(Collectors.toList()));
        log.info("IDP clients for tenant [{}] registered", tenantKey);
    }

    public void removeTenantClientRegistrations(String tenantKey) {
        clientsHolder.remove(tenantKey);
    }

    // FIXME: suggest renaming to mapRegistrationIdToClientRegistration
    private static Map<String, ClientRegistration> createClientRegistrationIdToClientRegistration(
        Collection<ClientRegistration> registrations) {
        // FIXME: you can use Collectors.toUnmodifiableMap directly
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
            // FIXME: suggest increase logger level to WARN
            log.info("IDP clients for tenant [{}] not registered", tenantKey);
            return null;
        }

        ClientRegistration clientRegistration = tenantClientsRegistration.get(registrationId);
        if (clientRegistration == null) {
            // FIXME: suggest increase logger level to WARN
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
    // FIXME: Suggest making method package private as it is used only in tests
    public Map<String, ClientRegistration> findByTenantKey(String tenantKey) {
        Map<String, ClientRegistration> tenantClientsRegistration = clientsHolder.get(tenantKey);

        if (CollectionUtils.isEmpty(tenantClientsRegistration)) {
            // FIXME: suggest increase logger level to WARN
            log.info("IDP clients for tenant [{}] not registered", tenantKey);
        }
        return tenantClientsRegistration;
    }
}
