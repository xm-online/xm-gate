package com.icthh.xm.gate.domain.idp;

import com.icthh.xm.commons.domain.idp.IdpConfigUtils;
import com.icthh.xm.commons.domain.idp.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import com.icthh.xm.commons.domain.idp.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class IdpConfigContainer {

    private IdpPublicClientConfig idpPublicClientConfig;
    private IdpPrivateClientConfig idpPrivateClientConfig;

    /**
     * Method checks is both public and private configs are valid for processing.
     *
     * @return true if both configs are valid, otherwise false
     */
    public boolean isApplicable() {
        return idpPublicClientConfig != null && idpPrivateClientConfig != null;
    }
}
