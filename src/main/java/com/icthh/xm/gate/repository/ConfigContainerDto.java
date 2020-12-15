package com.icthh.xm.gate.repository;

import com.icthh.xm.gate.dto.idp.PrivateIdpClientConfigDto;
import com.icthh.xm.gate.dto.idp.PublicIdpClientConfigDto;
import lombok.Data;

@Data
public class ConfigContainerDto {

    private PublicIdpClientConfigDto publicIdpClientConfigDto;
    private PrivateIdpClientConfigDto privateIdpClientConfigDto;

    public boolean isApplicable() {
        return publicIdpClientConfigDto != null && privateIdpClientConfigDto != null;
    }
}
