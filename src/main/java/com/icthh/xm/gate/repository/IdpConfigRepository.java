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

    private ConcurrentHashMap<String, ConfigContainerDto> idpClientConfigs = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConfigContainerDto> tmpIdpClientConfigs = new ConcurrentHashMap<>();

    private CustomInMemoryClientRegistrationRepository clientRegistrationRepository;

    private final TenantContextHolder tenantContextHolder;

    @Autowired
    public IdpConfigRepository(CustomInMemoryClientRegistrationRepository clientRegistrationRepository,
                               TenantContextHolder tenantContextHolder) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    public PublicIdpClientConfigDto getPublicIdpConfigByKey(String idpKey) {
        ConfigContainerDto configContainerDto = idpClientConfigs.get(idpKey);
        return configContainerDto.getPublicIdpClientConfigDto();
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
            processPublicConfiguration(idpKeyPrefix, publicIdpDto);
        }

        if (matcher.match(privateSettingsConfigPath, configKey)) {
            PrivateIdpDto privateIdpDto = objectMapper.readValue(config, PrivateIdpDto.class);
            if (privateIdpDto.getIdp() == null) {
                return;
            }
            processPrivateConfiguration(idpKeyPrefix, privateIdpDto);
        }

        List<ConfigContainerDto> applicablyConfigs = tmpIdpClientConfigs.values().stream()
            .filter(ConfigContainerDto::isApplicable)
            .collect(Collectors.toList());

        if (tmpIdpClientConfigs.size() != applicablyConfigs.size()) {
            log.info("IDP configs not fully loaded or has wrong configuration");
            return;
        }

        clientRegistrationRepository.setRegistrations(buildClientRegistrations());

        updateInMemoryDataStorage();
    }

    private String getTenantKey(String configKey) {
        if (matcher.match(publicSettingsConfigPath, configKey)) {
            return extractTenantKeyFromPath(configKey, publicSettingsConfigPath);
        } else {
            return extractTenantKeyFromPath(configKey, privateSettingsConfigPath);
        }
    }

    private void processPublicConfiguration(String idpKeyPrefix, PublicIdpDto publicIdpDto) {
        publicIdpDto.getIdp().getClients()
            .forEach(publicIdpClientConfigDto -> {
                    String compositeKey = (idpKeyPrefix + publicIdpClientConfigDto.getKey()).toLowerCase();

                    ConfigContainerDto configContainerDto = tmpIdpClientConfigs.get(compositeKey);
                    if (configContainerDto == null) {
                        configContainerDto = new ConfigContainerDto();
                    }
                    configContainerDto.setPublicIdpClientConfigDto(publicIdpClientConfigDto);
                    tmpIdpClientConfigs.put(compositeKey, configContainerDto);
                }
            );
    }

    private void processPrivateConfiguration(String idpKeyPrefix, PrivateIdpDto privateIdpDto) {
        privateIdpDto.getIdp().getClients()
            .forEach(privateIdpClientConfigDto -> {
                    String compositeKey = (idpKeyPrefix + privateIdpClientConfigDto.getKey()).toLowerCase();

                    ConfigContainerDto configContainerDto = tmpIdpClientConfigs.get(compositeKey);
                    if (configContainerDto == null) {
                        configContainerDto = new ConfigContainerDto();
                    }

                    configContainerDto.setPrivateIdpClientConfigDto(privateIdpClientConfigDto);

                    tmpIdpClientConfigs.put(compositeKey, configContainerDto);
                }
            );
    }

    private void updateInMemoryDataStorage() {
        idpClientConfigs.putAll(tmpIdpClientConfigs);
        tmpIdpClientConfigs.clear();
    }

    private String buildIdpKeyPrefix(String tenantKey) {
        return (tenantKey + "_").toLowerCase();
    }

    private String extractTenantKeyFromPath(String configKey, String settingsConfigPath) {
        Map<String, String> configKeyParams = matcher.extractUriTemplateVariables(settingsConfigPath, configKey);

        return configKeyParams.get(KEY_TENANT);
    }

    private List<ClientRegistration> buildClientRegistrations() {
        return tmpIdpClientConfigs.entrySet().stream()
            .map(entry -> createClientRegistration(
                entry.getKey(),
                entry.getValue().getPublicIdpClientConfigDto(),
                entry.getValue().getPrivateIdpClientConfigDto()
            )).collect(Collectors.toList());
    }

    private ClientRegistration createClientRegistration(String compositeRegistrationId,
                                                        PublicIdpClientConfigDto publicIdpClientConfigDto,
                                                        PrivateIdpClientConfigDto privateIdpConfig) {

        return ClientRegistration.withRegistrationId((compositeRegistrationId))
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
