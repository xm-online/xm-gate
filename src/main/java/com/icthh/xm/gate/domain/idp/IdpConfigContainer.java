package com.icthh.xm.gate.domain.idp;

import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
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
    public boolean isApplicable(String tenantKey) {
        String clientKey = idpPublicClientConfig != null ? idpPublicClientConfig.getKey() :
            idpPrivateClientConfig != null ? idpPrivateClientConfig.getKey() : "EMPTY";

        if (idpPublicClientConfig == null) {
            log.warn("For tenant [{}] public idp config not specified for client with key [{}].", tenantKey, clientKey);
            return false;
        }
        if (idpPrivateClientConfig == null) {
            log.warn("For tenant [{}] private idp config not specified for client with key [{}].", tenantKey, clientKey);
            return false;
        }
        return true;
    }
}
