package com.icthh.xm.gate.repository;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CustomInMemoryClientRegistrationRepository implements
    ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final TenantContextHolder tenantContextHolder;

    private Map<String, ClientRegistration> registrations = new HashMap<>();

    public CustomInMemoryClientRegistrationRepository(TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    public Map<String, ClientRegistration> getRegistrations() {
        return registrations;
    }

    public void setRegistrations(Map<String, ClientRegistration> registrations) {
        Assert.notEmpty(registrations, "registrations cannot be empty");
        this.registrations.putAll(registrations);
    }

    public void setRegistrations(List<ClientRegistration> registrations) {
        Assert.notEmpty(registrations, "registrations cannot be empty");
        this.registrations.putAll(createClientRegistrationIdToClientRegistration(registrations));
    }

    private static Map<String, ClientRegistration> createClientRegistrationIdToClientRegistration(Collection<ClientRegistration> registrations) {
        return Collections.unmodifiableMap(registrations.stream()
            .peek(registration -> Assert.notNull(registration, "registrations cannot contain null values"))
            .collect(Collectors.toMap(ClientRegistration::getRegistrationId, Function.identity())));
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");

        TenantKey tenantKey = tenantContextHolder.getContext()
            .getTenantKey().orElseThrow(() -> new IllegalArgumentException("tenantKey not found in context!"));

        String compositeKey = (tenantKey.getValue() + "_" + registrationId).toLowerCase();

        return this.registrations.get(compositeKey);
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return this.registrations.values().iterator();
    }

}
