package com.example.simplescheduleapp.exception;

import com.example.simplescheduleapp.common.auth.BearerTokenExtractor;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.member.application.MemberService;
import com.example.simplescheduleapp.member.presentation.MemberController;
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
 * 깨진 요청 본문은 <b>클라이언트 귀책</b>이므로 400이다.
 *
 * <p>{@code HttpMessageNotReadableException} 핸들러가 없으면 {@code CommonExceptionHandler}의
 * 포괄 {@code handleException}이 삼켜 <b>500 + ERROR 스택</b>이 나간다. 클라이언트는 자기 잘못을
 * 서버 장애로 보고받고, 서버 로그는 남의 오타로 오염된다. 500은 알람을 울려야 하는 신호인데
 * 깨진 JSON 한 줄로 울리면 알람이 무의미해진다.
 *
 * <p>{@code POST /login}을 쓰는 이유는 {@code @PublicEndpoint}라 인증 게이트를 통과하고 곧장
 * 본문 파싱 단계까지 도달하기 때문이다 — 401에 가려지지 않고 파싱 실패만 순수하게 검증한다.
 */
@DisplayName("깨진 요청 본문 은(는)")
@WebMvcTest(MemberController.class)
class MalformedRequestBodyHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private BearerTokenExtractor bearerTokenExtractor;

    @DisplayName("문법이 깨진 JSON이면 400과 우리 계약({code, message})으로 응답한다")
    @Test
    void 문법이_깨진_JSON은_400이다() throws Exception {
        // 닫는 중괄호가 없는 JSON — Jackson이 파싱 단계에서 실패한다.
        String malformed = "{ \"username\" : \"jungsehui\" ";

        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformed))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ISE3"))
                .andExpect(jsonPath("$.message").exists());

        // 본문을 읽지도 못했으므로 서비스는 호출되면 안 된다.
        verifyNoInteractions(memberService);
    }

    @DisplayName("본문이 아예 비어 있어도 400이다")
    @Test
    void 빈_본문도_400이다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ISE3"));

        verifyNoInteractions(memberService);
    }
}
