package com.icthh.xm.gate.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.gate.idp.IdpConfigContainer;
import com.icthh.xm.gate.idp.IdpPrivateConfig;
import com.icthh.xm.gate.idp.IdpPrivateConfig.PrivateIdpConfigDto.IdpPrivateClientConfig;
import com.icthh.xm.gate.idp.IdpPublicConfig.PublicIdpConfigDto.IdpPublicClientConfig;
import com.icthh.xm.gate.idp.IdpPublicConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import org.springframework.stereotype.Component;

import org.springframework.util.AntPathMatcher;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdpConfigRepository implements RefreshableConfiguration {

    private static final String PUBLIC_SETTINGS_CONFIG_PATH_PATTERN = "/config/tenants/{tenant}/webapp/settings-public.yml";
    private static final String PRIVATE_SETTINGS_CONFIG_PATH_PATTERN = "/config/tenants/{tenant}/idp-config.yml";
    private static final String KEY_TENANT = "tenant";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final AntPathMatcher matcher = new AntPathMatcher();

    private ConcurrentHashMap<String, IdpConfigContainer> idpClientConfigs = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, IdpConfigContainer> tmpIdpClientConfigs = new ConcurrentHashMap<>();

    private final IdpClientHolder clientRegistrationRepository;

    @Override
    public void onRefresh(String updatedKey, String config) {
        updateIdpConfigs(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, updatedKey) ||
            matcher.match(PRIVATE_SETTINGS_CONFIG_PATH_PATTERN, updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        updateIdpConfigs(configKey, configValue);
    }

    @SneakyThrows
    private void updateIdpConfigs(String configKey, String config) {
        String tenantKey = getTenantKey(configKey);

        if (!processPublicConfiguration(tenantKey, configKey, config)) {
            return;
        }

        if (!processPrivateConfiguration(tenantKey, configKey, config)) {
            return;
        }

        List<IdpConfigContainer> applicablyConfigs = tmpIdpClientConfigs
            .values()
            .stream()
            .filter(IdpConfigContainer::isApplicable)
            .collect(Collectors.toList());

        if (tmpIdpClientConfigs.size() != applicablyConfigs.size()) {
            log.info("IDP configs not fully loaded or has configuration lack");
            return;
        }

        clientRegistrationRepository.setRegistrations(buildClientRegistrations());

        updateInMemoryConfig();
    }

    private String getTenantKey(String configKey) {
        if (matcher.match(PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, configKey)) {
            return extractTenantKeyFromPath(configKey, PUBLIC_SETTINGS_CONFIG_PATH_PATTERN);
        } else {
            return extractTenantKeyFromPath(configKey, PRIVATE_SETTINGS_CONFIG_PATH_PATTERN);
        }
    }

    @SneakyThrows
    private boolean processPublicConfiguration(String tenantKey, String configKey, String config) {
        if (matcher.match(PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, configKey)) {
            IdpPublicConfig idpPublicConfig = objectMapper.readValue(config, IdpPublicConfig.class);
            if (idpPublicConfig.getConfig() == null) {
                return false;
            }
            idpPublicConfig
                .getConfig()
                .getClients()
                .forEach(publicIdpConf -> {
                        String compositeKey = IdpUtils.buildCompositeIdpKey(tenantKey, publicIdpConf.getKey());

                        IdpConfigContainer idpConfigContainer = getIdpConfigContainer(compositeKey);
                        idpConfigContainer.setIdpPublicClientConfig(publicIdpConf);

                        tmpIdpClientConfigs.put(compositeKey, idpConfigContainer);
                    }
                );
        }
        return true;
    }

    @SneakyThrows
    private boolean processPrivateConfiguration(String tenantKey, String configKey, String config) {
        if (matcher.match(PRIVATE_SETTINGS_CONFIG_PATH_PATTERN, configKey)) {
            IdpPrivateConfig idpPrivateConfig = objectMapper.readValue(config, IdpPrivateConfig.class);
            if (idpPrivateConfig.getConfig() == null) {
                return false;
            }
            idpPrivateConfig
                .getConfig()
                .getClients()
                .forEach(privateIdpConf -> {
                        String compositeKey = IdpUtils.buildCompositeIdpKey(tenantKey, privateIdpConf.getKey());

                        IdpConfigContainer idpConfigContainer = getIdpConfigContainer(compositeKey);

                        idpConfigContainer.setIdpPrivateClientConfig(privateIdpConf);

                        tmpIdpClientConfigs.put(compositeKey, idpConfigContainer);
                    }
                );
        }
        return true;
    }

    private void updateInMemoryConfig() {
        removeInMemoryClientRecords();

        updateConfig();
    }

    private void updateConfig() {
        idpClientConfigs.putAll(tmpIdpClientConfigs);
        tmpIdpClientConfigs.clear();
    }

    /**
     * Basing on input configuration method removes all previously registered tenants clients
     * to avoid redundant clients registration presence
     */
    private void removeInMemoryClientRecords() {
        //extract tenant prefixes
        List<String> tenantsPrefixKeys = tmpIdpClientConfigs
            .keySet()
            .stream()
            .map(key -> key.split(IdpUtils.KEY_SEPARATOR))
            .map(data -> data[0])
            .collect(Collectors.toList());
        //remove all client records which are related to specified tenant
        List<String> tenantClientsKeysToDelete = new ArrayList<>();

        tenantsPrefixKeys.forEach(tenantClientKey -> {
            tenantClientsKeysToDelete.addAll(idpClientConfigs
                .keySet()
                .stream()
                .filter(configContainerDto -> configContainerDto.startsWith(IdpUtils.buildIdpKeyPrefix(tenantClientKey)))
                .collect(Collectors.toList()));
        });


        tenantClientsKeysToDelete.forEach(keyToDelete -> idpClientConfigs.remove(keyToDelete));
    }

    private String extractTenantKeyFromPath(String configKey, String settingsConfigPath) {
        Map<String, String> configKeyParams = matcher.extractUriTemplateVariables(settingsConfigPath, configKey);

        return configKeyParams.get(KEY_TENANT);
    }

    private IdpConfigContainer getIdpConfigContainer(String compositeKey) {
        IdpConfigContainer idpConfigContainer = tmpIdpClientConfigs.get(compositeKey);
        if (idpConfigContainer == null) {
            idpConfigContainer = new IdpConfigContainer();
        }
        return idpConfigContainer;
    }

    private List<ClientRegistration> buildClientRegistrations() {
        return tmpIdpClientConfigs
            .entrySet()
            .stream()
            .map(entry -> createClientRegistration(
                entry.getKey(),
                entry.getValue().getIdpPublicClientConfig(),
                entry.getValue().getIdpPrivateClientConfig()
            )).collect(Collectors.toList());
    }

    private ClientRegistration createClientRegistration(String compositeRegistrationId,
                                                        IdpPublicClientConfig idpPublicClientConfig,
                                                        IdpPrivateClientConfig privateIdpConfig) {

        return ClientRegistration.withRegistrationId((compositeRegistrationId))
            .redirectUriTemplate(idpPublicClientConfig.getRedirectUri())
            .clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationUri(idpPublicClientConfig.getAuthorizationEndpoint().getUri())
            .tokenUri(idpPublicClientConfig.getTokenEndpoint().getUri())
            .userInfoUri(idpPublicClientConfig.getUserinfoEndpoint().getUri())
            .clientName(idpPublicClientConfig.getName())
            .clientId(idpPublicClientConfig.getClientId())
//            .jwkSetUri(idpPublicClientConfig.getJwksEndpoint().getUri())
            .clientSecret(privateIdpConfig.getClientSecret())
            .scope(privateIdpConfig.getScope())
            .build();
    }

}
