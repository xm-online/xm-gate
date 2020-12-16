package com.icthh.xm.gate.domain.idp;

import lombok.Data;

@Data
public class IdpConfigContainer {

    private IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig idpPublicClientConfig;
    private IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig idpPrivateClientConfig;

    public boolean isApplicable() {
        return idpPublicClientConfig != null && idpPrivateClientConfig != null;
    }
}
