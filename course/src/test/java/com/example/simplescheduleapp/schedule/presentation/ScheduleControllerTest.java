package com.example.simplescheduleapp.schedule.presentation;

import com.example.simplescheduleapp.common.auth.BearerTokenExtractor;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.schedule.application.ScheduleQueryService;
import com.example.simplescheduleapp.schedule.application.port.out.ScheduleType;
import com.example.simplescheduleapp.schedule.application.port.out.ScheduleView;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /me/schedules} — 내 일정(캘린더).
 *
 * <p>핵심은 <b>조회 대상도 역할도 토큰에서만 온다</b>는 것이다. 경로에도 파라미터에도 식별자가
 * 없고, 역할을 클라이언트가 지정할 수도 없다 — 지정할 수 있으면 학생이 강사 일정을 달라고
 * 할 수 있다.
 */
@DisplayName("내 일정 조회 은(는)")
@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

    private static final String TOKEN = "any-token";
    private static final String FROM = "2032-05-01T00:00:00";
    private static final String TO = "2032-05-31T00:00:00";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleQueryService scheduleQueryService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private BearerTokenExtractor bearerTokenExtractor;

    @DisplayName("토큰이 없으면 401이고, 서비스는 호출조차 되지 않는다")
    @Test
    void 토큰_없으면_401이다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/me/schedules")
                        .param("from", FROM).param("to", TO))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(scheduleQueryService);
    }

    @DisplayName("조회 대상과 역할 모두 토큰에서 온다 — 클라이언트가 지정할 수 없다")
    @Test
    void 대상과_역할은_토큰에서_온다() throws Exception {
        givenToken(42L, Role.TUTOR);
        doReturn(List.of(new ScheduleView(9L, ScheduleType.LECTURE, "5월 강의",
                LocalDateTime.of(2032, 5, 3, 10, 0), LocalDateTime.of(2032, 5, 3, 12, 0), "3단원")))
                .when(scheduleQueryService).findMySchedules(eq(42L), eq(Role.TUTOR), any(), any());

        mockMvc.perform(MockMvcRequestBuilders.get("/me/schedules")
                        // 클라이언트가 남의 식별자와 역할을 섞어 보내도 무시된다
                        .param("memberId", "999").param("role", "STUDENT")
                        .param("from", FROM).param("to", TO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleResponses[0].scheduleId").value(9))
                .andExpect(jsonPath("$.scheduleResponses[0].type").value("LECTURE"))
                .andExpect(jsonPath("$.scheduleResponses[0].title").value("5월 강의"));

        verify(scheduleQueryService).findMySchedules(eq(42L), eq(Role.TUTOR), any(), any());
    }

    @DisplayName("역할 클레임이 없는 토큰은 403이다 — 무엇이 내 일정인지 판단할 근거가 없다")
    @Test
    void 역할_없는_토큰은_403이다() throws Exception {
        doReturn(TOKEN).when(bearerTokenExtractor).extract(anyString());
        doReturn(42L).when(tokenService).extractMemberId(TOKEN);
        doReturn(null).when(tokenService).extractRole(TOKEN);

        mockMvc.perform(MockMvcRequestBuilders.get("/me/schedules")
                        .param("from", FROM).param("to", TO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isForbidden());

        verifyNoInteractions(scheduleQueryService);
    }

    @DisplayName("from/to가 없으면 400이다 — 창 없는 조회는 전체 이력을 긁는다")
    @Test
    void 창_파라미터가_없으면_400이다() throws Exception {
        givenToken(42L, Role.STUDENT);

        mockMvc.perform(MockMvcRequestBuilders.get("/me/schedules")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(scheduleQueryService);
    }

    @DisplayName("from/to가 날짜로 파싱되지 않으면 400이다")
    @Test
    void 파싱_불가한_날짜는_400이다() throws Exception {
        givenToken(42L, Role.STUDENT);

        mockMvc.perform(MockMvcRequestBuilders.get("/me/schedules")
                        .param("from", "어제").param("to", TO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ISE3"));

        verifyNoInteractions(scheduleQueryService);
    }

    @DisplayName("일정이 없으면 빈 목록과 200이다")
    @Test
    void 일정이_없으면_빈_목록과_200이다() throws Exception {
        givenToken(43L, Role.PARENT);
        doReturn(List.of()).when(scheduleQueryService).findMySchedules(eq(43L), eq(Role.PARENT), any(), any());

        mockMvc.perform(MockMvcRequestBuilders.get("/me/schedules")
                        .param("from", FROM).param("to", TO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleResponses").isArray())
                .andExpect(jsonPath("$.scheduleResponses").isEmpty());
    }

    private void givenToken(Long memberId, Role role) {
        doReturn(TOKEN).when(bearerTokenExtractor).extract(anyString());
        doReturn(memberId).when(tokenService).extractMemberId(TOKEN);
        doReturn(role).when(tokenService).extractRole(TOKEN);
    }
}
