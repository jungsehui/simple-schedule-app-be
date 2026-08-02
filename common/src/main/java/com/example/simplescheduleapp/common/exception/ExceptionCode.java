package com.example.simplescheduleapp.common.exception;

public interface ExceptionCode {

    ErrorKind getKind();

    String getCode();

    String getMessage();
}
