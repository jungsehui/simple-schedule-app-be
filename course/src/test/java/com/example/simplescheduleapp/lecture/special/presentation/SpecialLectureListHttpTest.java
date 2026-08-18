package com.example.simplescheduleapp.lecture.special.presentation;

import com.example.simplescheduleapp.common.auth.BearerTokenExtractor;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.lecture.special.application.SpecialLectureService;
import com.example.simplescheduleapp.lecture.special.domain.SpecialLecture;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /special-lectures} — 특강 목록.
 *
 * <p>새로 내는 엔드포인트가 <b>기본 차단에 걸리는지</b>가 첫 번째 관심사다(ADR-0005). 게이트는
 * {@code @PublicEndpoint}가 없는 모든 핸들러를 막는 구조이지만, 그 구조를 믿는 것과 이 핸들러가
 * 실제로 막히는 것은 다르다 — 과거 이 프로젝트는 애노테이션을 빠뜨린 7개 엔드포인트가 조용히
 * 무방비로 남아 있었다.
 */
@DisplayName("특강 목록 조회 은(는)")
@WebMvcTest(SpecialLectureController.class)
class SpecialLectureListHttpTest {

    private static final String TOKEN = "any-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpecialLectureService specialLectureService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private BearerTokenExtractor bearerTokenExtractor;

    @DisplayName("토큰이 없으면 401이고, 서비스는 호출조차 되지 않는다")
    @Test
    void 토큰_없으면_401이다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/special-lectures"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(specialLectureService);
    }

    @DisplayName("역할 제한은 없다 — 학생도 특강을 둘러볼 수 있어야 신청할 수 있다")
    @Test
    void 학생도_조회할_수_있다() throws Exception {
        givenToken(11L, Role.STUDENT);
        SpecialLecture specialLecture = new SpecialLecture("겨울 특강", LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(3).plusHours(3), "선착순", 5L, 30);
        doReturn(List.of(specialLecture)).when(specialLectureService).findAllSpecialLectures();

        mockMvc.perform(MockMvcRequestBuilders.get("/special-lectures")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialLectureResponses[0].title").value("겨울 특강"))
                .andExpect(jsonPath("$.specialLectureResponses[0].capacity").value(30));
    }

    @DisplayName("특강이 하나도 없으면 빈 목록과 200이다")
    @Test
    void 특강이_없으면_빈_목록과_200이다() throws Exception {
        givenToken(12L, Role.STUDENT);
        doReturn(List.of()).when(specialLectureService).findAllSpecialLectures();

        mockMvc.perform(MockMvcRequestBuilders.get("/special-lectures")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialLectureResponses").isArray())
                .andExpect(jsonPath("$.specialLectureResponses").isEmpty());
    }

    private void givenToken(Long memberId, Role role) {
        doReturn(TOKEN).when(bearerTokenExtractor).extract(anyString());
        doReturn(memberId).when(tokenService).extractMemberId(TOKEN);
        doReturn(role).when(tokenService).extractRole(TOKEN);
    }
}
