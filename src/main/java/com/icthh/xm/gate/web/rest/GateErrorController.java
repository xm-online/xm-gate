package com.icthh.xm.gate.web.rest;

import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.gate.web.rest.vm.ErrorVM;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class GateErrorController implements ErrorController {

    @Value("${error.path:/error}")
    private String errorPath;

    @Override
    public String getErrorPath() {
        return errorPath;
    }

    @RequestMapping(value = "${error.path:/error}")
    public @ResponseBody
    ResponseEntity error(HttpServletRequest request) {
        final int status = getErrorStatus(request);
        final String errorMessage = getErrorMessage(request);
        return ResponseEntity.status(status).body(new ErrorVM(ErrorConstants.ERR_BUSINESS,
            errorMessage != null ? errorMessage : HttpStatus.valueOf(status).getReasonPhrase()));
    }

    private int getErrorStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        return statusCode != null ? statusCode : HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    @SneakyThrows
    private String getErrorMessage(HttpServletRequest request) {
        final Throwable exc = (Throwable) request.getAttribute("javax.servlet.error.exception");
        return exc != null ? exc.getMessage() : null;
    }
}
