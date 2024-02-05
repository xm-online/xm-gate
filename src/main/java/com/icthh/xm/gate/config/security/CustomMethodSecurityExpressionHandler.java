package com.icthh.xm.gate.config.security;

import com.icthh.xm.commons.permission.access.XmPermissionEvaluator;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

// todo: remove handler after spring cloud release: https://github.com/spring-cloud/spring-cloud-release/milestone/140
@Primary
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class CustomMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    @Lazy
    @Autowired
    private XmPermissionEvaluator permissionEvaluator;

    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication,
                                                                              MethodInvocation invocation) {
        return createSecurityExpressionRoot(() -> authentication, invocation);
    }

    private MethodSecurityExpressionOperations createSecurityExpressionRoot(Supplier<Authentication> authentication,
                                                                            MethodInvocation invocation) {
        CustomMethodSecurityExpressionRoot root = new CustomMethodSecurityExpressionRoot(authentication);
        root.setThis(invocation.getThis());
        root.setPermissionEvaluator(permissionEvaluator);
        root.setTrustResolver(getTrustResolver());
        root.setRoleHierarchy(getRoleHierarchy());
        root.setDefaultRolePrefix(getDefaultRolePrefix());
        return root;
    }
}
