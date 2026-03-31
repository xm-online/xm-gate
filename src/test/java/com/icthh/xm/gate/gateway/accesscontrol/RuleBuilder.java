package com.icthh.xm.gate.gateway.accesscontrol;

import static com.icthh.xm.gate.config.properties.ApplicationProperties.AuthRequestMatcherRule;

public class RuleBuilder {

    private final AuthRequestMatcherRule rule = new AuthRequestMatcherRule();

    public RuleBuilder(String pattern) {
        rule.setPathPattern(pattern);
    }

    public static RuleBuilder rule(String pattern) {
        return new RuleBuilder(pattern);
    }

    public AuthRequestMatcherRule permitAll() {
        rule.setPermitAll(true);
        return rule;
    }

    public AuthRequestMatcherRule authenticated() {
        rule.setPermitAll(false);
        return rule;
    }

    public AuthRequestMatcherRule authorities(String... authorities) {
        rule.setAuthorities(authorities);
        return rule;
    }
}
