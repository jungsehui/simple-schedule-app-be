package com.example.simplescheduleapp.auth;

import com.example.simplescheduleapp.common.auth.BearerTokenExtractor;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.lecture.general.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.general.presentation.LectureEnrollmentController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증 게이트가 <b>실제 HTTP 경로에서</b> 작동하는지 검증한다 (ADR-0005).
 *
 * <p><b>왜 단위 테스트로 부족한가.</b> {@code AuthenticationInterceptorTest}는 인터셉터를 직접
 * 호출하므로, 인터셉터가 {@code AuthConfig}에 <em>등록되지 않아도</em> 통과한다. 등록을 빠뜨리면
 * 게이트는 아무 효과가 없는데 테스트는 초록이다 — 그 구멍을 이 테스트가 막는다.
 */
@DisplayName("인증 게이트(HTTP) 은(는)")
@WebMvcTest(LectureEnrollmentController.class)
class AuthGateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LectureEnrollmentService lectureEnrollmentService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private BearerTokenExtractor bearerTokenExtractor;

    @DisplayName("보호 엔드포인트를 토큰 없이 부르면 401이고, 서비스는 호출조차 되지 않는다")
    @Test
    void protectedEndpointWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lectures/1/enrollments"))
                .andExpect(status().isUnauthorized());

        // 게이트가 컨트롤러 진입 전에 끊었다는 증거 — 통과 후 안쪽에서 막힌 것이 아니다.
        verifyNoInteractions(lectureEnrollmentService);
    }
}
