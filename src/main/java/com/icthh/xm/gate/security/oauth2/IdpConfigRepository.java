package com.icthh.xm.gate.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.gate.domain.idp.IdpConfigContainer;
import com.icthh.xm.gate.domain.idp.IdpPrivateConfig;
import com.icthh.xm.gate.domain.idp.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import com.icthh.xm.gate.domain.idp.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import com.icthh.xm.gate.domain.idp.IdpPublicConfig;

import java.util.HashMap;
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
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdpConfigRepository implements RefreshableConfiguration {

    private static final String PUBLIC_SETTINGS_CONFIG_PATH_PATTERN = "/config/tenants/{tenant}/webapp/settings-public.yml";
    private static final String PRIVATE_SETTINGS_CONFIG_PATH_PATTERN = "/config/tenants/{tenant}/idp-config.yml";
    private static final String KEY_TENANT = "tenant";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final AntPathMatcher matcher = new AntPathMatcher();

    private final Map<String, Map<String, IdpConfigContainer>> idpClientConfigs = new ConcurrentHashMap<>();
    private final Map<String, Map<String, IdpConfigContainer>> tmpIdpClientConfigs = new ConcurrentHashMap<>();

    private final IdpClientHolder clientRegistrationRepository;

    @Override
    public void onRefresh(String updatedKey, String config) {
        updateIdpConfigs(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, updatedKey)
            || matcher.match(PRIVATE_SETTINGS_CONFIG_PATH_PATTERN, updatedKey);
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

        Map<String, IdpConfigContainer> idpConfigContainers =
            tmpIdpClientConfigs.computeIfAbsent(tenantKey, key -> new HashMap<>());

        Map<String, IdpConfigContainer> applicablyIdpConfigs = idpConfigContainers
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().isApplicable())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (CollectionUtils.isEmpty(applicablyIdpConfigs)) {
            log.info("For tenant [{}] IDP configs not fully loaded or has configuration lack", tenantKey);
            return;
        }

        clientRegistrationRepository.setRegistrations(buildClientRegistrations(tenantKey, applicablyIdpConfigs));

        updateInMemoryConfig(tenantKey, applicablyIdpConfigs);
    }

    private String getTenantKey(String configKey) {
        if (matcher.match(PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, configKey)) {
            return extractTenantKeyFromPath(configKey, PUBLIC_SETTINGS_CONFIG_PATH_PATTERN);
        } else {
            return extractTenantKeyFromPath(configKey, PRIVATE_SETTINGS_CONFIG_PATH_PATTERN);
        }
    }

    //TODO processPrivateConfiguration and  processPublicConfiguration very similar, think how to combine them
    @SneakyThrows
    private boolean processPublicConfiguration(String tenantKey, String configKey, String config) {
        if (!matcher.match(PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, configKey)) {
            return true;
        }
        IdpPublicConfig idpPublicConfig = objectMapper.readValue(config, IdpPublicConfig.class);
        if (idpPublicConfig.getConfig() == null) {
            return false;
        }
        idpPublicConfig
            .getConfig()
            .getClients()
            .forEach(publicIdpConf -> {
                    String idpConfKey = publicIdpConf.getKey();

                    IdpConfigContainer idpConfigContainer = getIdpConfigContainer(tenantKey, idpConfKey);
                    idpConfigContainer.setIdpPublicClientConfig(publicIdpConf);
                }
            );

        return true;
    }

    @SneakyThrows
    private boolean processPrivateConfiguration(String tenantKey, String configKey, String config) {
        if (!matcher.match(PRIVATE_SETTINGS_CONFIG_PATH_PATTERN, configKey)) {
            return true;
        }
        IdpPrivateConfig idpPrivateConfig = objectMapper.readValue(config, IdpPrivateConfig.class);
        if (idpPrivateConfig.getConfig() == null) {
            return false;
        }
        idpPrivateConfig
            .getConfig()
            .getClients()
            .forEach(privateIdpConf -> {
                    String idpConfKey = privateIdpConf.getKey();

                    IdpConfigContainer idpConfigContainer = getIdpConfigContainer(tenantKey, idpConfKey);
                    idpConfigContainer.setIdpPrivateClientConfig(privateIdpConf);
                }
            );

        return true;
    }

    /**
     * <p>
     * Basing on input configuration method removes all previously registered clients for specified tenant
     * to avoid redundant clients registration presence
     * </p>
     *
     * @param tenantKey         tenant key
     * @param applicablyConfigs fully loaded configs for processing
     */
    private void updateInMemoryConfig(String tenantKey, Map<String, IdpConfigContainer> applicablyConfigs) {
        idpClientConfigs.put(tenantKey, applicablyConfigs);

        tmpIdpClientConfigs.computeIfPresent(tenantKey, (k, v) -> {
            applicablyConfigs.keySet().forEach(v::remove);

            if (CollectionUtils.isEmpty(v.values())) {
                return null;
            }
            return v;
        });
    }

    private String extractTenantKeyFromPath(String configKey, String settingsConfigPath) {
        Map<String, String> configKeyParams = matcher.extractUriTemplateVariables(settingsConfigPath, configKey);

        return configKeyParams.get(KEY_TENANT);
    }

    private IdpConfigContainer getIdpConfigContainer(String tenantKey, String registrationId) {
        Map<String, IdpConfigContainer> tmpIdpConfigContainers = tmpIdpClientConfigs.computeIfAbsent(tenantKey, key -> new HashMap<>());

        return tmpIdpConfigContainers.computeIfAbsent(registrationId, key -> new IdpConfigContainer());
    }

    private List<ClientRegistration> buildClientRegistrations(String tenantKey, Map<String, IdpConfigContainer> applicablyConfigs) {
        return applicablyConfigs
            .entrySet()
            .stream()
            .map(entry -> createClientRegistration(
                IdpUtils.buildCompositeIdpKey(tenantKey, entry.getKey()),
                entry.getValue().getIdpPublicClientConfig(),
                entry.getValue().getIdpPrivateClientConfig()
            ))
            .collect(Collectors.toList());
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
            .jwkSetUri(idpPublicClientConfig.getJwksEndpoint().getUri())
            .clientSecret(privateIdpConfig.getClientSecret())
            .scope(privateIdpConfig.getScope())
            .build();
    }
}
