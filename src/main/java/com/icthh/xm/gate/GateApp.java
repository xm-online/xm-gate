package com.icthh.xm.gate;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.gate.config.properties.ApplicationProperties;
import com.icthh.xm.gate.config.CRLFLogConverter;
import com.icthh.xm.gate.utils.DefaultProfileUtil;
import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import static com.icthh.xm.gate.config.Constants.SPRING_PROFILE_CLOUD;
import static com.icthh.xm.gate.config.Constants.SPRING_PROFILE_DEVELOPMENT;
import static com.icthh.xm.gate.config.Constants.SPRING_PROFILE_PRODUCTION;

@Slf4j
@SpringBootApplication
@ComponentScan(
    basePackages = "com.icthh.xm",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.icthh.xm.commons.web.spring.config.WebMvcConfig.class
    )
)
@EnableConfigurationProperties({ ApplicationProperties.class })
@EnableDiscoveryClient
@Import({TenantContextConfiguration.class})
public class GateApp {

    private final Environment env;
    private final TenantContextHolder tenantContextHolder;

    public GateApp(Environment env, TenantContextHolder tenantContextHolder) {
        this.env = env;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Initializes xm-gate-java25.
     * <p>
     * Spring profiles can be configured with a program argument --spring.profiles.active=your-active-profile
     * <p>
     * You can find more information on how profiles work with JHipster on <a href="https://www.jhipster.tech/profiles/">https://www.jhipster.tech/profiles/</a>.
     */
    @PostConstruct
    public void initApplication() {
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (activeProfiles.contains(SPRING_PROFILE_DEVELOPMENT) &&
            activeProfiles.contains(SPRING_PROFILE_PRODUCTION)) {
            log.error(
                "You have misconfigured your application! It should not run " +
                    "with both the 'dev' and 'prod' profiles at the same time.");
        }
        if (activeProfiles.contains(SPRING_PROFILE_DEVELOPMENT) &&
            activeProfiles.contains(SPRING_PROFILE_CLOUD)) {
            log.error(
                "You have misconfigured your application! It should not " +
                    "run with both the 'dev' and 'cloud' profiles at the same time.");
        }
        initContexts();
    }

    private void initContexts() {
        // init tenant context, by default this is XM super tenant
        TenantContextUtils.setTenant(tenantContextHolder, TenantKey.SUPER);

        // init logger MDC context
        MdcUtils.putRid(MdcUtils.generateRid() + "::" + TenantKey.SUPER.getValue());
    }

    @PreDestroy
    public void destroyApplication() {
        log.info("\n----------------------------------------------------------\n\t"
                + "Application {} is closing"
                + "\n----------------------------------------------------------",
            env.getProperty("spring.application.name"));
    }

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        MdcUtils.putRid();

        var app = new SpringApplication(GateApp.class);
        DefaultProfileUtil.addDefaultProfile(app);
        Environment env = app.run(args).getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = Optional.ofNullable(env.getProperty("server.ssl.key-store"))
            .map(key -> "https")
            .orElse("http");
        String applicationName = env.getProperty("spring.application.name");
        String serverPort = env.getProperty("server.port");
        String contextPath = Optional.ofNullable(env.getProperty("server.servlet.context-path"))
            .filter(StringUtils::isNotBlank)
            .orElse("/");
        var hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }
        log.info(
            CRLFLogConverter.CRLF_SAFE_MARKER,
            """

            ----------------------------------------------------------
            \tApplication '{}' is running! Access URLs:
            \tLocal: \t\t{}://localhost:{}{}
            \tExternal: \t{}://{}:{}{}
            \tProfile(s): \t{}
            ----------------------------------------------------------""",
            applicationName,
            protocol,
            serverPort,
            contextPath,
            protocol,
            hostAddress,
            serverPort,
            contextPath,
            env.getActiveProfiles().length == 0 ? env.getDefaultProfiles() : env.getActiveProfiles()
        );

        String configServerStatus = env.getProperty("configserver.status");
        if (configServerStatus == null) {
            configServerStatus = "Not found or not setup for this application";
        }
        log.info(
            CRLFLogConverter.CRLF_SAFE_MARKER,
            "\n----------------------------------------------------------\n\t" +
                "Config Server: \t{}\n----------------------------------------------------------",
            configServerStatus
        );
    }
}
