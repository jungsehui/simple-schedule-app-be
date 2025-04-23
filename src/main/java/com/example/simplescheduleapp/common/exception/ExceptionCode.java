package com.example.simplescheduleappback.common.exception;

import org.springframework.http.HttpStatus;

public interface ExceptionCode {

    HttpStatus getHttpStatus();

    String getCode();

    String getMessage();
}
