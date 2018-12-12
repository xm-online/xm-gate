package com.icthh.xm.gate.web.rest.vm;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class ErrorVM implements Serializable {

    private static final long serialVersionUID = 1L;
    protected final String error;
    protected final String error_description;

    public ErrorVM(String error) {
        this(error, null);
    }

    public ErrorVM(String error, String error_description) {
        this.error = error;
        this.error_description = error_description;
    }

}
