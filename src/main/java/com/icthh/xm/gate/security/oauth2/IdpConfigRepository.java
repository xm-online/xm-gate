package com.icthh.xm.gate.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.gate.dto.idp.ConfigContainerDto;
import com.icthh.xm.gate.dto.idp.PrivateIdpClientConfigDto;
import com.icthh.xm.gate.dto.idp.PrivateIdpDto;
import com.icthh.xm.gate.dto.idp.PublicIdpClientConfigDto;
import com.icthh.xm.gate.dto.idp.PublicIdpDto;
import com.icthh.xm.gate.repository.ConfigContainerDto;
import com.icthh.xm.gate.security.oauth2.CustomInMemoryClientRegistrationRepository; // TODO unused import
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IdpConfigRepository implements RefreshableConfiguration {

    //TODO constant should have name in format CONFIG_NAME_BLA_BLA
    private static final String publicSettingsConfigPath = "/config/tenants/{tenant}/webapp/settings-public.yml";
    private static final String privateSettingsConfigPath = "/config/tenants/{tenant}/idp-config.yml";
    private static final String KEY_TENANT = "tenant";
    public static final String PREFIX_SEPARATOR = "_";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final AntPathMatcher matcher = new AntPathMatcher();

    private ConcurrentHashMap<String, ConfigContainerDto> idpClientConfigs = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConfigContainerDto> tmpIdpClientConfigs = new ConcurrentHashMap<>();

    //TODO should be final
    private CustomInMemoryClientRegistrationRepository clientRegistrationRepository;

    //TODO unused
    private final TenantContextHolder tenantContextHolder;

    @Autowired
    public IdpConfigRepository(CustomInMemoryClientRegistrationRepository clientRegistrationRepository,
                               TenantContextHolder tenantContextHolder) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    //TODO unused
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
        return matcher.match(publicSettingsConfigPath, updatedKey) ||
            matcher.match(privateSettingsConfigPath, updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        updateIdpConfigs(configKey, configValue);
    }

    @SneakyThrows
    private void updateIdpConfigs(String configKey, String config) {
        System.out.println("configKey: " + configKey); //TODO remove sout, use @Slf4j if logger is need
        System.out.println("config: " + config);

        String tenantKey = getTenantKey(configKey);
        String idpKeyPrefix = buildIdpKeyPrefix(tenantKey);


        //TODO let's move this logic to processPublicConfiguration
        if (matcher.match(publicSettingsConfigPath, configKey)) {
            PublicIdpDto publicIdpDto = objectMapper.readValue(config, PublicIdpDto.class);
            if (publicIdpDto.getIdp() == null) {
                return;
            }
            processPublicConfiguration(idpKeyPrefix, publicIdpDto);
        }

        //TODO let's move this logic to processPrivateConfiguration
        if (matcher.match(privateSettingsConfigPath, configKey)) {
            PrivateIdpDto privateIdpDto = objectMapper.readValue(config, PrivateIdpDto.class);
            if (privateIdpDto.getIdp() == null) {
                return;
            }
            processPrivateConfiguration(idpKeyPrefix, privateIdpDto);
        }

        List<ConfigContainerDto> applicablyConfigs = tmpIdpClientConfigs
            .values()
            .stream()
            .filter(ConfigContainerDto::isApplicable)
            .collect(Collectors.toList());

        if (tmpIdpClientConfigs.size() != applicablyConfigs.size()) {
            log.info("IDP configs not fully loaded or has configuration lack");
            return;
        }

        clientRegistrationRepository.setRegistrations(buildClientRegistrations());

        updateInMemoryConfig();
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
            .forEach(publicIdpConf -> {
                    String compositeKey = (idpKeyPrefix + publicIdpConf.getKey()).toLowerCase();

                    ConfigContainerDto configContainerDto = tmpIdpClientConfigs.get(compositeKey);
                    if (configContainerDto == null) {
                        configContainerDto = new ConfigContainerDto();
                    }
                    configContainerDto.setPublicIdpClientConfigDto(publicIdpConf);
                    tmpIdpClientConfigs.put(compositeKey, configContainerDto);
                }
            );
    }

    private void processPrivateConfiguration(String idpKeyPrefix, PrivateIdpDto privateIdpDto) {
        privateIdpDto.getIdp().getClients()
            .forEach(privateIdpConf -> {
                    String compositeKey = (idpKeyPrefix + privateIdpConf.getKey()).toLowerCase();

                    ConfigContainerDto configContainerDto = tmpIdpClientConfigs.get(compositeKey);
                    if (configContainerDto == null) {
                        configContainerDto = new ConfigContainerDto();
                    }

                    configContainerDto.setPrivateIdpClientConfigDto(privateIdpConf);

                    tmpIdpClientConfigs.put(compositeKey, configContainerDto);
                }
            );
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
        List<String> keys = tmpIdpClientConfigs.keySet().stream()
            .map(key -> key.split(PREFIX_SEPARATOR))
            .map(data -> data[0])
            .collect(Collectors.toList());
        //remove all client records which are related to specified tenant
        List<String> keysToDelete = new ArrayList<>();

        keys.forEach(key -> {
            keysToDelete.addAll(idpClientConfigs.keySet()
                .stream()
                .filter(configContainerDto -> configContainerDto.startsWith(key + PREFIX_SEPARATOR))
                .collect(Collectors.toList())) ;
        });

        keysToDelete.forEach(keyToDelete-> idpClientConfigs.remove(keyToDelete));
    }

    private String buildIdpKeyPrefix(String tenantKey) {
        return (tenantKey + PREFIX_SEPARATOR).toLowerCase();
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
