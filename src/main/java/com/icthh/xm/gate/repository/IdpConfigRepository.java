package com.icthh.xm.gate.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.gate.dto.IdpClientConfigDto;
import com.icthh.xm.gate.dto.IdpDto;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IdpConfigRepository implements RefreshableConfiguration {

    private static final String publicSettingsConfigPath = "/config/tenants/{tenant}/webapp/settings-public.yml";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final AntPathMatcher matcher = new AntPathMatcher();

    private Boolean directLogin;

    private ConcurrentHashMap<String, IdpClientConfigDto> idpClientConfigs = new ConcurrentHashMap<>();

    private CustomInMemoryClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    public IdpConfigRepository(CustomInMemoryClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    public IdpClientConfigDto getIdpConfigByKey(String idpKey) {
        return idpClientConfigs.get(idpKey);
    }

    public Boolean getDirectLogin() {
        return directLogin;
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        updateIdpConfigs(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(publicSettingsConfigPath, updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        updateIdpConfigs(configKey, configValue);
    }

    @SneakyThrows
    private void updateIdpConfigs(String configKey, String config) {
        System.out.println("configKey: " + configKey);
        System.out.println("config: " + config);

        IdpDto idpDto = objectMapper.readValue(config, IdpDto.class);
        if (idpDto.getIdp() == null) {
            return;
        }
        IdpDto.IdpConfigDto idp = idpDto.getIdp();
        this.directLogin = idp.getDirectLogin();

        idp.getClients().forEach(idpClientConfigDto -> idpClientConfigs.put(idpClientConfigDto.getKey(), idpClientConfigDto));
        List<ClientRegistration> clientRegistrations = idp.getClients().stream()
            .map(idpClientConfigDto -> clientRegistration(idpClientConfigDto).build())
            .collect(Collectors.toUnmodifiableList());
        clientRegistrationRepository.setRegistrations(clientRegistrations);
    }

    private ClientRegistration.Builder clientRegistration(IdpClientConfigDto idpClientConfigDto) {

        return ClientRegistration.withRegistrationId(idpClientConfigDto.getKey())//same as key in settings-public/private
            .redirectUriTemplate(idpClientConfigDto.getRedirectUri())
            .clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .scope("read:user")
            .authorizationUri(idpClientConfigDto.getAuthorizationEndpoint().getUri())
            .tokenUri(idpClientConfigDto.getTokenEndpoint().getUri())
            .jwkSetUri("https://ticino-dev-co.eu.auth0.com/.well-known/jwks.json")//todo set from private-settings name
            .userInfoUri(idpClientConfigDto.getUserinfoEndpoint().getUri())
            .clientName("Dynamic Client Name")//todo set from public-settings name
            .clientId(idpClientConfigDto.getClientId())
            .clientSecret("NHOsxzwEBgflBHuGF-mF9NkF8HI5kotVkBJYrpTPsZf0s9Js5klBrJ5bdROjMHLZ") //todo set from private-settings name
            ;
    }


}
