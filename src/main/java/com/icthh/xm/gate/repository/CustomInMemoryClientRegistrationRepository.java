package com.icthh.xm.gate.repository;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CustomInMemoryClientRegistrationRepository implements
    ClientRegistrationRepository, Iterable<ClientRegistration> {

    private Map<String, ClientRegistration> registrations;

    public Map<String, ClientRegistration> getRegistrations() {
        return registrations;
    }

    public void setRegistrations(Map<String, ClientRegistration> registrations) {
        Assert.notEmpty(registrations, "registrations cannot be empty");
        this.registrations = registrations;
    }

    public void setRegistrations(List<ClientRegistration> registrations) {
        Assert.notEmpty(registrations, "registrations cannot be empty");
        this.registrations = createClientRegistrationIdToClientRegistration(registrations);
    }

    private static Map<String, ClientRegistration> createClientRegistrationIdToClientRegistration(Collection<ClientRegistration> registrations) {
        return Collections.unmodifiableMap(registrations.stream()
            .peek(registration -> Assert.notNull(registration, "registrations cannot contain null values"))
            .collect(Collectors.toMap(ClientRegistration::getRegistrationId, Function.identity())));
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");
        return this.registrations.get(registrationId);
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return this.registrations.values().iterator();
    }

}
