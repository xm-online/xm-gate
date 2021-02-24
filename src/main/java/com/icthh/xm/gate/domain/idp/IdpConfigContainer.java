package com.icthh.xm.gate.domain.idp;

import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.Features;

import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class IdpConfigContainer {

    private IdpPublicClientConfig idpPublicClientConfig;
    private IdpPrivateClientConfig idpPrivateClientConfig;
    private Features features;

    /**
     * Method checks is both public and private configs are valid for processing.
     *
     * @return true if both configs are valid, otherwise false
     */
    public boolean isApplicable() {
        return idpPublicClientConfig != null && idpPrivateClientConfig != null;
    }
}
