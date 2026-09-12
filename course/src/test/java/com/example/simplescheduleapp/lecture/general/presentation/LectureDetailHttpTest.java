package com.example.simplescheduleapp.lecture.general.presentation;

import com.example.simplescheduleapp.common.auth.BearerTokenExtractor;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.lecture.general.application.LectureService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /lectures/{lectureId}} — 강의 상세.
 *
 * <p><b>왜 이 테스트가 필요한가.</b> 이 엔드포인트는 <b>쓰기 응답 타입</b>
 * ({@code LectureCreateResponse})을 읽기에 재사용하고 있었다. 그 record에는
 * {@code enrolledCount}가 없어서, 목록에는 잔여석이 보이는데 상세로 들어가면 사라졌다.
 * 데이터가 없어서가 아니다 — {@code Lecture} 도메인이 이미 갖고 있는 값을 DTO가 버렸다.
 *
 * <p>쓰기 응답을 읽기가 재사용하면 계약이 몰래 묶인다. 생성 응답에 필드를 하나 넣거나 빼면
 * 상세 조회 계약이 함께 움직이는데, 그것을 알아차릴 방법이 없다. 타입을 갈라 두면 그 결합이
 * 사라진다.
 */
@DisplayName("강의 상세 조회 은(는)")
@WebMvcTest(LectureController.class)
class LectureDetailHttpTest {

    private static final String TOKEN = "any-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LectureService lectureService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private BearerTokenExtractor bearerTokenExtractor;

    @DisplayName("잔여석을 계산할 수 있도록 enrolledCount를 함께 준다")
    @Test
    void 상세에도_enrolledCount가_있다() throws Exception {
        givenToken(42L, Role.STUDENT);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        doReturn(Lecture.reconstitute(7L, 0L, "중등 수학 심화", start, start.plusHours(2),
                "3단원", 9L, 20, 13))
                .when(lectureService).findLecture(7L);

        mockMvc.perform(MockMvcRequestBuilders.get("/lectures/7")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lectureId").value(7))
                .andExpect(jsonPath("$.title").value("중등 수학 심화"))
                .andExpect(jsonPath("$.capacity").value(20))
                .andExpect(jsonPath("$.enrolledCount").value(13));
    }

    /**
     * 목록과 상세가 같은 항목 모양이어야 클라이언트가 강의 카드 컴포넌트 하나로 두 화면을 그린다.
     * 필드가 갈리면 화면마다 다른 매핑을 들고 있어야 한다.
     */
    @DisplayName("목록 항목과 같은 필드를 준다")
    @Test
    void 목록_항목과_같은_모양이다() throws Exception {
        givenToken(42L, Role.STUDENT);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        doReturn(Lecture.reconstitute(7L, 0L, "제목", start, start.plusHours(2), "메모", 9L, 20, 13))
                .when(lectureService).findLecture(7L);

        mockMvc.perform(MockMvcRequestBuilders.get("/lectures/7")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lectureId").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.endTime").exists())
                .andExpect(jsonPath("$.memo").exists())
                .andExpect(jsonPath("$.capacity").exists())
                .andExpect(jsonPath("$.enrolledCount").exists());
    }

    @DisplayName("토큰이 없으면 401이다")
    @Test
    void 토큰_없으면_401이다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/lectures/7"))
                .andExpect(status().isUnauthorized());
    }

    private void givenToken(Long memberId, Role role) {
        doReturn(TOKEN).when(bearerTokenExtractor).extract(anyString());
        doReturn(memberId).when(tokenService).extractMemberId(TOKEN);
        doReturn(role).when(tokenService).extractRole(TOKEN);
    }
}
