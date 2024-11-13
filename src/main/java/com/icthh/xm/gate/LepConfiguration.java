package com.icthh.xm.gate;

import com.icthh.xm.commons.lep.groovy.GroovyLepEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LepConfiguration extends GroovyLepEngineConfiguration {
    public LepConfiguration(@Value("${spring.application.name}") String appName) {
        super(appName);
    }
}
