package com.philxin.interviewos.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final int code;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.code = status.value();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }
}
