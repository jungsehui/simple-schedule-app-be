package cupid.common.exception;

import org.springframework.http.HttpStatus;

public enum InternalServerExceptionCode implements ExceptionCode {

    UNKNOWN_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, "ISE1", "알 수 없는 예외가 발생했습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    InternalServerExceptionCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
