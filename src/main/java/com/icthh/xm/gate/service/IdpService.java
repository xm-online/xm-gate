package com.icthh.xm.gate.service;

import com.icthh.xm.gate.dto.IdpClientConfigDto;
import com.icthh.xm.gate.repository.IdpConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdpService {

    private final IdpConfigRepository idpConfigRepository;

    public String getAuthUrlByIdpKey(String idpKey) {
        IdpClientConfigDto idpConfigByKey = idpConfigRepository.getIdpConfigByKey(idpKey);

        return buildAuthUrl(idpConfigByKey);
    }

    private String buildAuthUrl(IdpClientConfigDto clientConfigDto) {
        IdpClientConfigDto.AuthorizationEndpoint authorizationEndpoint = clientConfigDto.getAuthorizationEndpoint();
        String authorizationEndpointUri = authorizationEndpoint.getUri();

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(authorizationEndpointUri);
        uriComponentsBuilder.queryParam("response_type", authorizationEndpoint.getResponseType());
        uriComponentsBuilder.queryParam("client_id", clientConfigDto.getClientId());
        uriComponentsBuilder.queryParam("redirect_uri", clientConfigDto.getRedirectUri());

        return uriComponentsBuilder.build().toString();
    }
}

