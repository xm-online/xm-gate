package com.icthh.xm.gate.security.oauth2;

import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_PRIVATE_SETTINGS_CONFIG_PATH_PATTERN;
import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.domain.idp.IdpConfigUtils;
import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.Features;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.gate.domain.idp.IdpConfigContainer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * This class reads and process both IDP clients public and private configuration for each tenant.
 * Tenant IDP clients created for each successfully loaded config. If config not fully loaded it skipped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdpConfigRepository implements RefreshableConfiguration {

    private static final String KEY_TENANT = "tenant";
    public static final String IDP_EMPTY_CONFIG = "idp:";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * In memory storage for storing information tenant IDP clients public/private configuration.
     * We need to store this information in memory cause public/private configuration could be loaded and processed in random order.
     * For correct tenant IDP clients registration both configs should be loaded and processed.
     */
    private final Map<String, Map<String, IdpConfigContainer>> idpClientConfigs = new ConcurrentHashMap<>();

    /**
     * In memory storage.
     * Stores information about only valid tenant IDP clients public/private configuration that currently in process.
     * We need to store this information in memory cause:
     * - public/private configuration could be loaded and processed in random order.
     * - to avoid corruption previously registered in-memory tenant clients config
     * For correct tenant IDP clients registration both configs should be loaded and processed.
     */
    private final Map<String, Map<String, IdpConfigContainer>> tmpValidIdpClientConfigs = new ConcurrentHashMap<>();

    /**
     * In memory storage for storing information is tenant public/private configuration process state.
     * Generally speaking this information allows to understand is tenant public & private configuration loaded and processed.
     * We need to store this information in memory cause public/private configuration could be loaded and processed in random order.
     * Map key respond for tenant name, map value respond for config process state.
     * Left pair value relates to public config process state, right pair value relates to private config process state.
     */
    private final Map<String, MutablePair<Boolean, Boolean>> idpClientConfigProcessingState = new ConcurrentHashMap<>();

    private final Map<String, Features> idpTenantFeaturesHolder = new ConcurrentHashMap<>();

    private final IdpClientRepository clientRegistrationRepository;

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return isPrivateIdpConfig(updatedKey)
            || isPublicIdpConfig(updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        updateIdpConfigs(configKey, configValue);
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        updateIdpConfigs(updatedKey, config);
    }

    private void updateIdpConfigs(String configKey, String config) {
        String tenantKey = getTenantKey(configKey);

        processPublicConfiguration(tenantKey, configKey, config);

        processPrivateConfiguration(tenantKey, configKey, config);

        Map<String, IdpConfigContainer> applicablyIdpConfigs = tmpValidIdpClientConfigs
            .computeIfAbsent(tenantKey, key -> new HashMap<>())
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().isApplicable(tenantKey))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (CollectionUtils.isEmpty(applicablyIdpConfigs)) {
            MutablePair<Boolean, Boolean> configProcessingState = idpClientConfigProcessingState.get(tenantKey);

            boolean isPublicConfigProcessed = configProcessingState.getLeft() != null && configProcessingState.getLeft();
            boolean isPrivateConfigProcess = configProcessingState.getRight() != null && configProcessingState.getRight();
            boolean isValidClientConfigurationsEmpty = CollectionUtils.isEmpty(tmpValidIdpClientConfigs.get(tenantKey));

            // if both public and private tenant configs processed
            // and client configuration not present at all then all tenant client registrations should be removed
            if (isPublicConfigProcessed && isPrivateConfigProcess && isValidClientConfigurationsEmpty) {
                log.warn("For tenant [{}] IDP client configs not specified. "
                    + "Removing all previously registered IDP clients.", tenantKey);
                clientRegistrationRepository.removeTenantClientRegistrations(tenantKey);
                idpClientConfigProcessingState.remove(tenantKey);
                idpClientConfigs.remove(tenantKey);
            } else {
                log.warn("For tenant [{}] IDP configs not fully loaded or it has lack of configuration", tenantKey);
            }

            return;
        }

        clientRegistrationRepository.setRegistrations(tenantKey, buildClientRegistrations(applicablyIdpConfigs));
        updateInMemoryConfig(tenantKey, applicablyIdpConfigs);
    }

    private String getTenantKey(String configKey) {
        if (isPublicIdpConfig(configKey)) {
            return extractTenantKeyFromPath(configKey, IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN);
        } else {
            return extractTenantKeyFromPath(configKey, IDP_PRIVATE_SETTINGS_CONFIG_PATH_PATTERN);
        }
    }

    private void processPublicConfiguration(String tenantKey, String configKey, String config) {
        if (!isPublicIdpConfig(configKey)) {
            return;
        }
        log.info("Processing public idp configuration for tenant [{}]", tenantKey);
        processFeatures(tenantKey, config);

        processPublicClientsConfiguration(tenantKey, config);

        idpClientConfigProcessingState.computeIfAbsent(tenantKey, key -> new MutablePair<>()).setLeft(true);

    }

    private void processPublicClientsConfiguration(String tenantKey, String config) {
        Optional.ofNullable(parseConfig(tenantKey, config, IdpPublicConfig.class))
            .map(IdpPublicConfig::getConfig)
            .map(IdpPublicConfig.IdpConfigContainer::getClients)
            .orElseGet(Collections::emptyList)
            .stream()
            .filter(IdpConfigUtils::isPublicClientConfigValid)
            .forEach(publicIdpConf -> setIdpPublicClientConfig(tenantKey, publicIdpConf));
    }

    @SneakyThrows
    private void processFeatures(String tenantKey, String config) {
        IdpPublicConfig idpPublicConfig = parseConfig(tenantKey, config, IdpPublicConfig.class);

        Features validFeatures = new Features();//instantiate default idp features configuration

        if (idpPublicConfig != null && idpPublicConfig.getConfig() != null) {
            Features tenantFeaturesConfig = idpPublicConfig.getConfig().getFeatures();

            if (!IdpConfigUtils.isTenantFeaturesConfigValid(tenantFeaturesConfig)) {
                log.error("Features configuration invalid for tenant [{}]. Applying default config." +
                    "See log for more details", tenantKey);
            } else {
                validFeatures = tenantFeaturesConfig;
            }
        } else {
            log.warn("Features configuration not specified for tenant [{}]. Applying default config.", tenantKey);
        }
        idpTenantFeaturesHolder.put(tenantKey, validFeatures);
    }

    private void processPrivateConfiguration(String tenantKey, String configKey, String config) {

        if (!isPrivateIdpConfig(configKey)) {
            return;
        }
        log.info("Processing private idp configuration for tenant [{}]", tenantKey);

        Optional.ofNullable(parseConfig(tenantKey, config, IdpPrivateConfig.class))
            .map(IdpPrivateConfig::getConfig)
            .map(IdpPrivateConfig.IdpConfigContainer::getClients)
            .orElseGet(Collections::emptyList)
            .stream()
            .filter(IdpConfigUtils::isPrivateClientConfigValid)
            .forEach(privateIdpConf -> setIdpPrivateClientConfig(tenantKey, privateIdpConf));

        idpClientConfigProcessingState.computeIfAbsent(tenantKey, key -> new MutablePair<>()).setRight(true);

    }

    private boolean isPublicIdpConfig(String configKey) {
        return matcher.match(IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN, configKey);
    }

    private boolean isPrivateIdpConfig(String configKey) {
        return matcher.match(IDP_PRIVATE_SETTINGS_CONFIG_PATH_PATTERN, configKey);
    }

    @SneakyThrows
    private <T> T parseConfig(String tenantKey, String config, Class<T> configType) {
        T parsedConfig;

        try {
            parsedConfig = objectMapper.readValue(config, configType);
        } catch (JsonProcessingException e) {
            log.error("Something went wrong during attempt to read config [{}] for tenant [{}]. " +
                "Creating default config.", config, tenantKey, e);
            parsedConfig = objectMapper.readValue(IDP_EMPTY_CONFIG, configType);
        }

        return parsedConfig;
    }

    /**
     * <p>
     * Basing on input configuration method removes all previously registered clients for specified tenant
     * to avoid redundant clients registration presence
     * </p>
     *
     * @param tenantKey         tenant key
     * @param applicablyConfigs fully loaded valid configs for processing
     */
    private void updateInMemoryConfig(String tenantKey, Map<String, IdpConfigContainer> applicablyConfigs) {
        tmpValidIdpClientConfigs.remove(tenantKey);
        idpClientConfigProcessingState.remove(tenantKey);
        idpClientConfigs.put(tenantKey, applicablyConfigs);
    }

    private String extractTenantKeyFromPath(String configKey, String settingsConfigPath) {
        return matcher.extractUriTemplateVariables(settingsConfigPath, configKey).get(KEY_TENANT);

    }

    private void setIdpPublicClientConfig(String tenantKey, IdpPublicClientConfig publicConfig) {
        getIdpConfigContainer(tenantKey, publicConfig.getKey())
            .setIdpPublicClientConfig(publicConfig);
    }

    private void setIdpPrivateClientConfig(String tenantKey, IdpPrivateClientConfig privateConfig) {
        getIdpConfigContainer(tenantKey, privateConfig.getKey())
            .setIdpPrivateClientConfig(privateConfig);
    }

    private IdpConfigContainer getIdpConfigContainer(String tenantKey, String registrationId) {
        return tmpValidIdpClientConfigs.computeIfAbsent(tenantKey, key -> new HashMap<>())
            .computeIfAbsent(registrationId, clientId -> new IdpConfigContainer());
    }

    private List<ClientRegistration> buildClientRegistrations(Map<String, IdpConfigContainer> applicablyConfigs) {
        return applicablyConfigs
            .entrySet()
            .stream()
            .map(entry -> createClientRegistration(
                entry.getKey(),
                entry.getValue().getIdpPublicClientConfig(),
                entry.getValue().getIdpPrivateClientConfig()
            ))
            .collect(Collectors.toList());
    }

    private ClientRegistration createClientRegistration(String registrationId,
                                                        IdpPublicClientConfig idpPublicClientConfig,
                                                        IdpPrivateClientConfig privateIdpConfig) {

        IdpPublicClientConfig.OpenIdConfig openIdConfig = idpPublicClientConfig.getOpenIdConfig();

        return ClientRegistration.withRegistrationId((registrationId))
            .redirectUri(idpPublicClientConfig.getRedirectUri())
            .clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
            .authorizationGrantType(new AuthorizationGrantType(openIdConfig.getTokenEndpoint().getGrantType()))
            .authorizationUri(openIdConfig.getAuthorizationEndpoint().getUri())
            .tokenUri(openIdConfig.getTokenEndpoint().getUri())
            .userInfoUri(openIdConfig.getUserinfoEndpoint().getUri())
            .userNameAttributeName(openIdConfig.getUserinfoEndpoint().getUserNameAttributeName())
            .clientName(idpPublicClientConfig.getName())
            .clientId(idpPublicClientConfig.getClientId())
            .jwkSetUri(openIdConfig.getJwksEndpoint().getUri())
            .clientSecret(privateIdpConfig.getClientSecret())
            .scope(privateIdpConfig.getScope())
            .build();
    }

    Map<String, Map<String, IdpConfigContainer>> getIdpClientConfigs() {
        return idpClientConfigs;
    }

    public Features getTenantFeatures(String tenantKey) {
        Features features = idpTenantFeaturesHolder.get(tenantKey);

        if (ObjectUtils.isEmpty(features)) {
            log.warn("Features configuration not specified for tenant [{}]. Setting default config.", tenantKey);
            features = new Features();
            idpTenantFeaturesHolder.put(tenantKey, features);
        }

        return features;
    }
}
