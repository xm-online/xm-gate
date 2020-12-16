package com.icthh.xm.gate.dto.idp;

import lombok.Data;

@Data
public class ConfigContainerDto {

    private PublicIdpClientConfigDto publicIdpClientConfigDto;
    private PrivateIdpClientConfigDto privateIdpClientConfigDto;

    public boolean isApplicable() {
        return publicIdpClientConfigDto != null && privateIdpClientConfigDto != null;
    }
}
