package com.icthh.xm.gate.security.oauth2;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import org.springframework.stereotype.Component;

import org.springframework.util.Assert;


@Component
@RequiredArgsConstructor
public class IdpClientHolder implements
    ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final TenantContextHolder tenantContextHolder;

    private final Map<String, ClientRegistration> clientsHolder = new HashMap<>();

    public void setRegistrations(List<ClientRegistration> registrations) {
        Assert.notEmpty(registrations, "registrations cannot be empty");
        this.clientsHolder.putAll(createClientRegistrationIdToClientRegistration(registrations));
    }

    public void removeRegistration(String registrationId) {
        clientsHolder.remove(registrationId);
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

        String tenantKey = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        String compositeKey = IdpUtils.buildCompositeIdpKey(tenantKey, registrationId);

        return this.clientsHolder.get(compositeKey);
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return this.clientsHolder.values().iterator();
    }

}
