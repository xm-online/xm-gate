package com.icthh.xm.gate.config;

import com.icthh.xm.commons.logging.aop.ServiceLoggingAspect;
import com.icthh.xm.commons.logging.config.LoggingConfigService;
import com.icthh.xm.commons.logging.util.BasePackageDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import tech.jhipster.config.JHipsterConstants;

@Configuration
@EnableAspectJAutoProxy
public class LoggingAspectConfiguration {

    @Bean
    @Profile(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)
    public ServiceLoggingAspect loggingAspect(LoggingConfigService loggingConfigService, BasePackageDetector basePackageDetector) {
        return new ServiceLoggingAspect(loggingConfigService, basePackageDetector);
    }
}
