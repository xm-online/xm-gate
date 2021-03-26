package com.icthh.xm.gate.domain.idp;

import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class IdpConfigContainer {

    private static final String KEY_EMPTY = "EMPTY";

    private IdpPublicClientConfig idpPublicClientConfig;
    private IdpPrivateClientConfig idpPrivateClientConfig;

    /**
     * Method checks is both public and private configs are valid for processing.
     *
     * @return true if both configs are valid, otherwise false
     */
    public boolean isApplicable(String tenantKey) {
        String clientKey = idpPublicClientConfig != null ? idpPublicClientConfig.getKey() :
            idpPrivateClientConfig != null ? idpPrivateClientConfig.getKey() : KEY_EMPTY;

        if (KEY_EMPTY.equals(clientKey)) {
            log.warn("For tenant [{}] valid public and private idp configs not specified.", tenantKey);
            return false;
        }

        if (idpPublicClientConfig == null) {
            log.warn("For tenant [{}] valid public idp config not present for client with key [{}].", tenantKey, clientKey);
            return false;
        }
        if (idpPrivateClientConfig == null) {
            log.warn("For tenant [{}] valid private idp config not present for client with key [{}].", tenantKey, clientKey);
            return false;
        }
        return true;
    }
}
