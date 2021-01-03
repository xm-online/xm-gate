package com.icthh.xm.gate.domain.idp;

import com.icthh.xm.gate.domain.idp.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import com.icthh.xm.gate.domain.idp.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import lombok.Data;

@Data
public class IdpConfigContainer {

    private IdpPublicClientConfig idpPublicClientConfig;
    private IdpPrivateClientConfig idpPrivateClientConfig;

    public boolean isApplicable() {
        return idpPublicClientConfig != null && idpPrivateClientConfig != null;
    }
}
