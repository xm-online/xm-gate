package com.icthh.xm.gate.idp;

import lombok.Data;

@Data
public class IdpConfigContainer {

    private IdpPublicConfig.PublicIdpConfigDto.IdpPublicClientConfig idpPublicClientConfig;
    private IdpPrivateConfig.PrivateIdpConfigDto.IdpPrivateClientConfig idpPrivateClientConfig;

    public boolean isApplicable() {
        return idpPublicClientConfig != null && idpPrivateClientConfig != null;
    }
}
