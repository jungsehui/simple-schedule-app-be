package com.example.simplescheduleapp.sse.presentation;

import com.example.simplescheduleapp.common.auth.BearerTokenExtractor;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.sse.application.SseConnectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SSE 엔드포인트의 <b>인증 실패 응답도</b> 우리 예외 계약({@code {code, message}} JSON)을 따라야 한다.
 *
 * <p>{@code /sse-stream}은 {@code produces = text/event-stream}이다. Spring은 핸들러 매핑에서
 * 정한 producible 미디어 타입을 요청 속성에 남기는데, 예외 핸들러가 JSON을 쓰려 할 때 그 속성이
 * 그대로 남아 있으면 협상이 실패한다. 그러면 우리 핸들러가 만든 401 JSON이 나가지 못하고
 * 컨테이너 기본 에러 페이지(HTML)로 떨어진다 — 클라이언트는 파싱 불가능한 응답을 받는다.
 *
 * <p>안드로이드의 OkHttp {@code EventSources}와 웹의 {@code EventSourcePolyfill} 모두 실패 응답을
 * 읽어 재연결 여부를 판단하므로, 이 계약이 깨지면 재연결 로직이 원인을 구분하지 못한다.
 */
@DisplayName("SSE 인증 실패 응답 은(는)")
@WebMvcTest(SseController.class)
class SseAuthFailureContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SseConnectionService sseConnectionService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private BearerTokenExtractor bearerTokenExtractor;

    @DisplayName("토큰 없이 구독하면 401과 JSON 본문을 주고, 스트림은 열리지 않는다")
    @Test
    void 토큰_없는_구독은_401_JSON이다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/sse-stream"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());

        // 게이트가 컨트롤러 진입 전에 끊었다는 증거 — Emitter가 만들어졌다면 커넥션이 샌 것이다.
        verifyNoInteractions(sseConnectionService);
    }

    /**
     * 실제 SSE 클라이언트가 보내는 요청 그대로다.
     *
     * <p>브라우저 {@code EventSource}, {@code EventSourcePolyfill}, OkHttp {@code EventSources}는
     * 모두 {@code Accept: text/event-stream}을 붙인다. Accept를 생략하면 모든 타입을 받겠다는 뜻이라
     * 협상이 통과해 버려 결함을 못 잡는다 — 이 헤더가 있어야 producible 미디어 타입과 충돌한다.
     */
    @DisplayName("Accept: text/event-stream으로 구독해도 실패 응답은 JSON이다")
    @Test
    void SSE_Accept_헤더가_있어도_실패_응답은_JSON이다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/sse-stream")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(sseConnectionService);
    }
}
