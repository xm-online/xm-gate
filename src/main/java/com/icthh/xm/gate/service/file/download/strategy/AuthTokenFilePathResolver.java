package com.icthh.xm.gate.service.file.download.strategy;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.gate.service.file.download.DownloadFileSpec;
import com.icthh.xm.gate.service.file.download.util.JwtParserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.icthh.xm.gate.service.file.download.DownloadFileSpec.PathPatternStrategy.XM_TOKEN_MATCHER;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenFilePathResolver implements FilePathResolver {

    private final XmAuthenticationContextHolder authenticationContextHolder;

    private final JwtParserUtils jwtParserUtils = new  JwtParserUtils();

    @Override
    public boolean supports(DownloadFileSpec.PathPatternStrategy strategy) {
        return XM_TOKEN_MATCHER == strategy;
    }

    @Override
    public String resolvePath(String pathPrefixPattern) {
        log.info("Resolve download file path with token for file path pattern: {}", pathPrefixPattern);

        XmAuthenticationContext context = authenticationContextHolder.getContext();
        Map<String, String> valuesMap = jwtParserUtils.flattenToken(context.getTokenValue());
        StringSubstitutor substitutor = new StringSubstitutor(valuesMap, "{", "}");

        return substitutor.replace(pathPrefixPattern);
    }
}
