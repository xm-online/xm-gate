package com.icthh.xm.gate.service.file.download;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadFileConfigService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";
    private static final String FILE_DOWNLOAD_SPEC_PATTERN = "/config/tenants/{tenantName}/file-download.yml";

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ObjectMapper ymlMapper = new ObjectMapper(new YAMLFactory());

    // tenant -> key -> spec
    private final Map<String, Map<String, DownloadFileSpec>> specs = new ConcurrentHashMap<>();

    private final TenantContextHolder tenantContextHolder;

    @Override
    public void onRefresh(String updatedKey, String config) {
        log.info("Refresh download file configuration: updatedKey={}", updatedKey);
        String tenantKey = extractTenant(updatedKey);

        if (StringUtils.isEmpty(config)) {
            specs.remove(tenantKey);
        } else {
            readSpec(updatedKey, config).ifPresentOrElse(s -> {
                var specMap = s.getPatterns().stream().collect(toMap(DownloadFileSpec::getKey, identity()));
                specs.put(tenantKey, specMap);
            }, () -> log.warn("Skip configuration processing: [{}]. Specification is null", updatedKey));
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(FILE_DOWNLOAD_SPEC_PATTERN, updatedKey);
    }

    public DownloadFileSpec getSpecByKey(String key) {
        String tenantKey = tenantContextHolder.getTenantKey();
        Optional<DownloadFileSpec> spec = Optional.ofNullable(specs.get(tenantKey)).map(e -> e.get(key));
        if (spec.isEmpty()) {
            log.warn("Spec not found for key [{}]", key);
            return new DownloadFileSpec();
        }
        return spec.get();
    }

    private String extractTenant(final String updatedKey) {
        return matcher.extractUriTemplateVariables(FILE_DOWNLOAD_SPEC_PATTERN, updatedKey).get(TENANT_NAME);
    }

    private Optional<DownloadFileSpec.DownloadPatternsList> readSpec(String updatedKey, String config) {
        try {
            return Optional.of(ymlMapper.readValue(config, DownloadFileSpec.DownloadPatternsList.class));

        } catch (Exception e) {
            log.error("Error when read download file spec from path: {}", updatedKey, e);
            return Optional.empty();
        }
    }
}
