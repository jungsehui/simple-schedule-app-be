package com.example.simplescheduleapp.lecture.special.presentation;

import com.example.simplescheduleapp.common.auth.BearerTokenExtractor;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.lecture.special.application.RedisSpecialLectureEnrollmentService;
import com.example.simplescheduleapp.lecture.special.application.command.SpecialLectureEnrollmentCreateCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code DELETE /special-lectures/{id}/enrollments} — 특강 수강신청 취소.
 *
 * <p>종전에는 이 경로가 <b>없었다.</b> 특강 매핑은 생성·목록·신청 셋뿐이었고 취소가 없어
 * 오신청이 영구였다. 일반 강의에는 대응 경로가 있는데 특강만 없던 비대칭이다.
 *
 * <p>이 테스트가 고정하는 것은 응답 모양이 아니라 <b>취소 대상이 토큰의 주체</b>라는 점이다.
 * 경로에도 파라미터에도 남의 식별자를 넣을 자리가 없어야 한다(ADR-0005).
 */
@DisplayName("특강 수강신청 취소 요청 은(는)")
@WebMvcTest(SpecialLectureEnrollmentController.class)
class SpecialLectureCancelHttpTest {

    private static final String TOKEN = "any-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RedisSpecialLectureEnrollmentService redisSpecialLectureEnrollmentService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private BearerTokenExtractor bearerTokenExtractor;

    @DisplayName("토큰이 없으면 401이고 서비스는 호출조차 되지 않는다")
    @Test
    void 토큰_없으면_401이다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/special-lectures/100/enrollments"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(redisSpecialLectureEnrollmentService);
    }

    @DisplayName("강사 토큰이면 403이다 — 학생 전용이다")
    @Test
    void 강사_토큰이면_403이다() throws Exception {
        givenToken(7L, Role.TUTOR);

        mockMvc.perform(MockMvcRequestBuilders.delete("/special-lectures/100/enrollments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isForbidden());

        verifyNoInteractions(redisSpecialLectureEnrollmentService);
    }

    @DisplayName("취소 대상은 토큰의 주체다 — 쿼리 파라미터로 남의 것을 지울 수 없다")
    @Test
    void 취소_대상은_토큰_주체다() throws Exception {
        Long tokenMemberId = 42L;
        givenToken(tokenMemberId, Role.STUDENT);

        mockMvc.perform(MockMvcRequestBuilders.delete("/special-lectures/100/enrollments")
                        .param("studentId", "99999")  // 레거시 파라미터에 남의 식별자를 넣어 본다
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isNoContent());

        // 파라미터가 아니라 토큰 주체로 취소했다는 증거
        verify(redisSpecialLectureEnrollmentService)
                .cancelSpecialLectureEnrollment(SpecialLectureEnrollmentCreateCommand.of(tokenMemberId, 100L));
    }

    private void givenToken(Long memberId, Role role) {
        doReturn(TOKEN).when(bearerTokenExtractor).extract(anyString());
        doReturn(memberId).when(tokenService).extractMemberId(TOKEN);
        doReturn(role).when(tokenService).extractRole(TOKEN);
    }
}
