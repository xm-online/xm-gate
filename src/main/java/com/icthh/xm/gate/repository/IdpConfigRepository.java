package com.icthh.xm.gate.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.gate.dto.idp.PrivateIdpClientConfigDto;
import com.icthh.xm.gate.dto.idp.PrivateIdpDto;
import com.icthh.xm.gate.dto.idp.PublicIdpClientConfigDto;
import com.icthh.xm.gate.dto.idp.PublicIdpDto;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IdpConfigRepository implements RefreshableConfiguration {

    private static final String publicSettingsConfigPath = "/config/tenants/{tenant}/webapp/settings-public.yml";
    private static final String privateSettingsConfigPath = "/config/tenants/{tenant}/idp-config.yml";
    private static final String KEY_TENANT = "tenant";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final AntPathMatcher matcher = new AntPathMatcher();

    private Boolean directLogin;

    private ConcurrentHashMap<String, ConfigContainer> idpClientConfigs = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConfigContainer> tmpIdpClientConfigs = new ConcurrentHashMap<>();

    private CustomInMemoryClientRegistrationRepository clientRegistrationRepository;

    private final TenantContextHolder tenantContextHolder;

    @Autowired
    public IdpConfigRepository(CustomInMemoryClientRegistrationRepository clientRegistrationRepository,
                               TenantContextHolder tenantContextHolder) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    public PublicIdpClientConfigDto getPublicIdpConfigByKey(String idpKey) {
        ConfigContainer configContainer = idpClientConfigs.get(idpKey);
        return configContainer.getPublicIdpClientConfigDto();
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
        return matcher.match(publicSettingsConfigPath, updatedKey) || matcher.match(privateSettingsConfigPath, updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        updateIdpConfigs(configKey, configValue);
    }

    @SneakyThrows
    private void updateIdpConfigs(String configKey, String config) {
        System.out.println("configKey: " + configKey);
        System.out.println("config: " + config);

        String tenantKey = getTenantKey(configKey);
        String idpKeyPrefix = buildIdpKeyPrefix(tenantKey);

        if (matcher.match(publicSettingsConfigPath, configKey)) {
            PublicIdpDto publicIdpDto = objectMapper.readValue(config, PublicIdpDto.class);
            if (publicIdpDto.getIdp() == null) {
                return;
            }
            directLogin = publicIdpDto.getIdp().getDirectLogin();

            publicIdpDto.getIdp().getClients()
                .forEach(publicIdpClientConfigDto -> {
                        String compositeKey = (idpKeyPrefix + publicIdpClientConfigDto.getKey()).toLowerCase();

                        ConfigContainer configContainer = tmpIdpClientConfigs.get(compositeKey);
                        if (configContainer == null) {
                            configContainer = new ConfigContainer();
                        }
                        configContainer.setPublicIdpClientConfigDto(publicIdpClientConfigDto);
                        tmpIdpClientConfigs.put(compositeKey, configContainer);
                    }
                );
        }

        if (matcher.match(privateSettingsConfigPath, configKey)) {
            PrivateIdpDto privateIdpDto = objectMapper.readValue(config, PrivateIdpDto.class);
            if (privateIdpDto.getIdp() == null) {
                return;
            }
            privateIdpDto.getIdp().getClients()
                .forEach(privateIdpClientConfigDto -> {
                        String compositeKey = (idpKeyPrefix + privateIdpClientConfigDto.getKey()).toLowerCase();

                        ConfigContainer configContainer = tmpIdpClientConfigs.get(compositeKey);
                        if (configContainer == null) {
                            configContainer = new ConfigContainer();
                        }

                        configContainer.setPrivateIdpClientConfigDto(privateIdpClientConfigDto);

                        tmpIdpClientConfigs.put(compositeKey, configContainer);
                    }
                );
        }

        List<ConfigContainer> applicablyConfigs = tmpIdpClientConfigs.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(idpKeyPrefix))
            .map(Map.Entry::getValue)
            .filter(ConfigContainer::isApplicable)
            .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(applicablyConfigs)) {
            log.info("Config not fully loaded");
            return;
        }

        idpClientConfigs.putAll(tmpIdpClientConfigs);
        tmpIdpClientConfigs.clear();

        List<ClientRegistration> clientRegistrations = buildClientRegistrations(idpKeyPrefix, applicablyConfigs);

        clientRegistrationRepository.setRegistrations(clientRegistrations);
    }

    private String getTenantKey(String configKey) {
        if (matcher.match(publicSettingsConfigPath, configKey)) {
            return extractTenantKeyFromPath(configKey, publicSettingsConfigPath);
        } else {
            return extractTenantKeyFromPath(configKey, privateSettingsConfigPath);
        }
    }

    private String buildIdpKeyPrefix(String tenantKey) {
        return (tenantKey + "_").toLowerCase();
    }

    private String extractTenantKeyFromPath(String configKey, String settingsConfigPath) {
        Map<String, String> configKeyParams = matcher.extractUriTemplateVariables(settingsConfigPath, configKey);

        return configKeyParams.get(KEY_TENANT);
    }

    private List<ClientRegistration> buildClientRegistrations(String idpKeyPrefix, List<ConfigContainer> applicablyConfigs) {

        return applicablyConfigs.stream()
            .map(config ->
                createClientRegistration(
                    idpKeyPrefix,
                    config.getPublicIdpClientConfigDto(),
                    config.getPrivateIdpClientConfigDto()
                ))
            .collect(Collectors.toUnmodifiableList());
    }

    private ClientRegistration createClientRegistration(String idpKeyPrefix,
                                                        PublicIdpClientConfigDto publicIdpClientConfigDto,
                                                        PrivateIdpClientConfigDto privateIdpConfig) {

        return ClientRegistration.withRegistrationId((idpKeyPrefix + publicIdpClientConfigDto.getKey()).toLowerCase())//same as key in settings-public/private
            .redirectUriTemplate(publicIdpClientConfigDto.getRedirectUri())
            .clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .scope(privateIdpConfig.getScope())
            .authorizationUri(publicIdpClientConfigDto.getAuthorizationEndpoint().getUri())
            .tokenUri(publicIdpClientConfigDto.getTokenEndpoint().getUri())
            .jwkSetUri("https://ticino-dev-co.eu.auth0.com/.well-known/jwks.json")//todo set from private-settings
            .userInfoUri(publicIdpClientConfigDto.getUserinfoEndpoint().getUri())
            .clientName(publicIdpClientConfigDto.getName())
            .clientId(publicIdpClientConfigDto.getClientId())
            .clientSecret(privateIdpConfig.getClientSecret())
            .build();
    }


}
