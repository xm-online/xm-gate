package com.icthh.xm.gate.security.oauth2;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.security.oauth2.OAuth2Properties;
import com.icthh.xm.gate.config.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

@Slf4j
@Service
public class XmConfigServerService {

    private final RestClient loadBalancedRestClient;
    private final OAuth2Properties oauth2Properties;

    public XmConfigServerService(@Qualifier("loadBalancedRestClient") RestClient loadBalancedRestClient,
                                 OAuth2Properties oauth2Properties) {
        this.loadBalancedRestClient = loadBalancedRestClient;
        this.oauth2Properties = oauth2Properties;
    }

    @IgnoreLogginAspect
    public RSAPublicKey getPublicKeyFromConfigServer() throws CertificateException, IOException {
        String tokenEndpointUrl = oauth2Properties.getSignatureVerification().getPublicKeyEndpointUri();
        String content = loadBalancedRestClient
            .get()
            .uri(tokenEndpointUrl)
            .retrieve()
            .body(String.class);

        if (StringUtils.isBlank(content)) {
            throw new CertificateException("Received empty certificate from config.");
        }

        try (InputStream fin = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {

            CertificateFactory f = CertificateFactory.getInstance(Constants.CERTIFICATE);
            X509Certificate certificate = (X509Certificate) f.generateCertificate(fin);
            PublicKey pk = certificate.getPublicKey();
            if (pk instanceof RSAPublicKey) {
                return (RSAPublicKey) pk;
            }
            throw new CertificateException("Public key is not an RSA key");
        }
    }
}
