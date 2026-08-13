package com.example.simplescheduleapp.common.exception;

import com.example.simplescheduleapp.common.exception.response.ExceptionResponse;
import com.example.simplescheduleapp.common.exception.response.MethodArgumentExceptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 → HTTP 응답 변환.
 *
 * <p><b>모든 응답에 {@code Content-Type: application/json}을 명시하는 이유.</b> Spring은 핸들러
 * 매핑이 정한 producible 미디어 타입을 요청 속성에 남겨 두고, 응답을 쓸 때 그것과 클라이언트
 * {@code Accept}를 협상한다. 그런데 {@code Content-Type}이 이미 구체적으로 정해져 있으면 협상을
 * 통째로 건너뛴다. 이 차이가 실제 장애를 만들었다: {@code produces = text/event-stream}인
 * {@code /sse-stream}을 SSE 클라이언트가 {@code Accept: text/event-stream}으로 부르면, 인증 실패
 * 401 JSON을 쓸 컨버터가 선택되지 못해 응답이 컨테이너 기본 에러 페이지(HTML)로 떨어졌다.
 * 오류 응답은 엔드포인트의 성공 응답 형식과 무관하게 언제나 우리 계약({@code {code, message}})이어야
 * 한다 — 클라이언트의 재연결·에러 처리 로직이 그것을 파싱한다.
 */
@Slf4j
@ControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(value = ApplicationException.class)
    public ResponseEntity<ExceptionResponse> handleApplicationException(ApplicationException exception) {
        ExceptionCode code = exception.getCode();
        HttpStatus status = toHttpStatus(code.getKind());
        if (status.is5xxServerError()) {
            log.error("ApplicationException occurred. code: {}, message: {}", code.getCode(), code.getMessage(), exception);
        } else {
            log.warn("ApplicationException occurred. code: {}, message: {}", code.getCode(), code.getMessage());
        }
        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ExceptionResponse.from(code));
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<MethodArgumentExceptionResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        BindingResult bindingResult = exception.getBindingResult();

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("MethodArgumentNotValidException occurred: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(MethodArgumentExceptionResponse.from(InternalServerExceptionCode.INVALID_INPUT_VALUE, errors));
    }

    /**
     * 읽을 수 없는 요청 본문 → 400. 클라이언트 귀책이므로 500이 아니다.
     *
     * <p>깨진 JSON, 빈 본문, 타입이 맞지 않는 값이 여기로 온다. 핸들러가 없으면 아래
     * {@code handleException}이 삼켜 <b>500 + ERROR 스택</b>이 나갔다. 두 가지가 동시에 나쁘다:
     * 클라이언트는 자기 오타를 서버 장애로 보고받고, 500은 알람을 울려야 하는 신호인데 남의
     * 깨진 요청 한 줄로 울려 알람이 무의미해진다.
     *
     * <p>코드는 {@link InternalServerExceptionCode#INVALID_INPUT_VALUE}(ISE3)를 재사용한다.
     * 필드 검증 실패와 본문 파싱 실패를 클라이언트가 구분하지 못한다는 대가가 있지만, 둘 다
     * "보낸 값이 잘못됐다"이고 조치도 같아서 코드표를 늘릴 값을 못 한다고 판단했다.
     *
     * <p>파싱 실패 원인은 응답에 싣지 않는다 — 역직렬화 예외 메시지에는 내부 타입명·필드
     * 구조가 그대로 담겨 있어 그대로 내보내면 스키마가 새어 나간다. 원인은 로그에만 남긴다.
     */
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        log.warn("Request body is not readable: {}", exception.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ExceptionResponse.from(InternalServerExceptionCode.INVALID_INPUT_VALUE));
    }

    /**
     * 존재하지 않는 경로 → 404. 클라이언트 귀책이므로 500이 아니다.
     *
     * <p>이게 없으면 아래 {@code handleException}이 삼켜 <b>500 + 스택트레이스</b>가 나간다.
     * 실제로 SSE 계약 변경 후 구 경로({@code /sse-stream/{memberId}})를 호출한 클라이언트가
     * 404 대신 500을 받았고, 서버 로그에는 매 요청마다 ERROR 스택이 쌓였다.
     */
    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<ExceptionResponse> handleNoResourceFound(NoResourceFoundException exception) {
        log.warn("No handler for request. path: {}", exception.getResourcePath());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ExceptionResponse.from(InternalServerExceptionCode.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ExceptionResponse> handleException(Exception exception) {
        log.error("Unhandled exception occurred.", exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ExceptionResponse.from(InternalServerExceptionCode.UNKNOWN_EXCEPTION));
    }

    /**
     * {@link ErrorKind} → HTTP 상태 변환 — 이 매핑은 웹 어댑터인 여기에만 존재한다.
     * switch는 망라형(exhaustive)이라 새 ErrorKind 추가 시 컴파일 에러로 매핑 누락을 잡는다.
     */
    private static HttpStatus toHttpStatus(ErrorKind kind) {
        return switch (kind) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
