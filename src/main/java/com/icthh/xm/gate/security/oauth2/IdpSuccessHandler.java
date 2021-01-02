package com.icthh.xm.gate.security.oauth2;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class IdpSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        //TODO implement stateful, see {@link org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler}

        PrintWriter out = response.getWriter();
        out.print(objectMapper.writeValueAsString("{}")); //TODO routing implementation
        out.flush();
    }
}
