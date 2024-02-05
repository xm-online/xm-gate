package com.icthh.xm.gate.security;

import com.icthh.xm.commons.permission.access.ResourceFactory;
import org.springframework.stereotype.Component;

@Component
public class GateResourceFactory implements ResourceFactory {

    @Override
    public Object getResource(Object resourceId, String objectType) {
        return null;
    }
}
