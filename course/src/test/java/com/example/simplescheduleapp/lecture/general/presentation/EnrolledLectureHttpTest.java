package com.example.simplescheduleapp.lecture.general.presentation;

import com.example.simplescheduleapp.common.auth.BearerTokenExtractor;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.lecture.general.application.LectureEnrollmentService;
import com.example.simplescheduleapp.lecture.general.domain.Lecture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /me/lectures} — 내 수강목록.
 *
 * <p>이 테스트의 핵심은 응답 모양이 아니라 <b>조회 주체가 토큰에서만 온다</b>는 것이다.
 * 대칭인 {@code GET /tutors/{tutorId}/lectures}는 경로 변수를 쓰지만, 그 모양은 ADR-0005가
 * SSE에서 걷어낸 바로 그것이다 — 남의 식별자를 넣으면 남의 데이터가 나온다.
 */
@DisplayName("내 수강목록 조회 은(는)")
@WebMvcTest(LectureEnrollmentController.class)
class EnrolledLectureHttpTest {

    private static final String TOKEN = "any-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LectureEnrollmentService lectureEnrollmentService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private BearerTokenExtractor bearerTokenExtractor;

    @DisplayName("토큰이 없으면 401이고, 서비스는 호출조차 되지 않는다")
    @Test
    void 토큰_없으면_401이다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/me/lectures"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(lectureEnrollmentService);
    }

    @DisplayName("강사 토큰이면 403이다 — 학생 전용 조회다")
    @Test
    void 강사_토큰이면_403이다() throws Exception {
        givenToken(7L, Role.TUTOR);

        mockMvc.perform(MockMvcRequestBuilders.get("/me/lectures")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lectureEnrollmentService);
    }

    @DisplayName("조회 대상은 토큰의 주체다 — 경로에도 파라미터에도 식별자가 없다")
    @Test
    void 조회_대상은_토큰_주체다() throws Exception {
        Long tokenMemberId = 42L;
        givenToken(tokenMemberId, Role.STUDENT);
        Lecture lecture = new Lecture("중등 수학 심화", LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2), "3단원", 9L, 20);
        doReturn(List.of(lecture)).when(lectureEnrollmentService).findEnrolledLectures(tokenMemberId);

        mockMvc.perform(MockMvcRequestBuilders.get("/me/lectures")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lectureResponses[0].title").value("중등 수학 심화"))
                .andExpect(jsonPath("$.lectureResponses[0].capacity").value(20));

        // 다른 식별자로는 절대 조회하지 않는다는 증거
        verify(lectureEnrollmentService).findEnrolledLectures(tokenMemberId);
    }

    @DisplayName("아무것도 신청하지 않았으면 빈 목록과 200이다 — 첫 화면이 에러면 안 된다")
    @Test
    void 신청이_없으면_빈_목록과_200이다() throws Exception {
        givenToken(43L, Role.STUDENT);
        doReturn(List.of()).when(lectureEnrollmentService).findEnrolledLectures(43L);

        mockMvc.perform(MockMvcRequestBuilders.get("/me/lectures")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lectureResponses").isArray())
                .andExpect(jsonPath("$.lectureResponses").isEmpty());
    }

    private void givenToken(Long memberId, Role role) {
        doReturn(TOKEN).when(bearerTokenExtractor).extract(anyString());
        doReturn(memberId).when(tokenService).extractMemberId(TOKEN);
        doReturn(role).when(tokenService).extractRole(TOKEN);
    }
}
