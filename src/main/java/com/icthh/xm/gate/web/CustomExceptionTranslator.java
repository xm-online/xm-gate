package com.icthh.xm.gate.web;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.i18n.error.domain.vm.ErrorVM;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.nio.file.NoSuchFileException;

/*
    Move these handlers to commons after migrating to new commons version
 */
@Slf4j
@ControllerAdvice
public class CustomExceptionTranslator extends ExceptionTranslator {

    private final LocalizationMessageService localizationMessageService;

    public CustomExceptionTranslator(LocalizationMessageService localizationErrorMessageService) {
        super(localizationErrorMessageService);
        this.localizationMessageService = localizationErrorMessageService;
    }

    @ExceptionHandler(NoSuchFileException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorVM processNotFoundError(NoSuchFileException ex) {
        log.debug("File not found", ex);
        return new ErrorVM(ErrorConstants.ERR_NOTFOUND,
            localizationMessageService.getMessage(ErrorConstants.ERR_NOTFOUND));
    }
}
