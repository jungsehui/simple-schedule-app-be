package com.example.simplescheduleapp.member.presentation;

import com.example.simplescheduleapp.common.auth.BearerTokenExtractor;
import com.example.simplescheduleapp.common.auth.Role;
import com.example.simplescheduleapp.common.auth.Token;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.lecture.general.application.LectureEnrollmentService;
import com.example.simplescheduleapp.student.application.StudentService;
import com.example.simplescheduleapp.student.presentation.StudentController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 가입 요청 DTO의 {@code @Pattern}이 통합 username 규칙을 <b>정규화 전 관점에서</b> 미러링하는지 본다.
 *
 * <p><b>왜 DTO 정규식이 도메인과 글자 그대로 같지 않은가.</b> 둘은 같은 규칙을 보지만 <b>보는
 * 시점이 다르다</b>. DTO는 사용자가 방금 보낸 <b>정규화 전</b> 입력을 보므로 대문자를 받아야
 * 하고, 도메인({@code Username})은 {@code toLowerCase} 이후의 <b>정준형</b>을 단언하므로
 * 소문자만 받는다. 둘을 하나로 "정리"하면 대문자 입력이 DTO 단계에서 400으로 잘려
 * {@code Username.of}에 도달하지 못한다 — "대문자는 거부가 아니라 정규화" 규칙이 표면에서
 * 무력화된다. 이 테스트가 고정하려는 사고가 정확히 그 통일 리팩터링이다.
 *
 * <p>{@code StudentController}를 대표로 쓴다. 세 가입 DTO({@code Student}/{@code Tutor}/
 * {@code Parent})의 {@code @Pattern} 블록은 현재 서로 동일하므로, 하나가 어긋나면 나머지도
 * 같은 이유로 어긋난다.
 */
@DisplayName("가입 요청의 username 검증 은(는)")
@WebMvcTest(StudentController.class)
class SignUpUsernameContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private LectureEnrollmentService lectureEnrollmentService;

    @MockitoBean
    private BearerTokenExtractor bearerTokenExtractor;

    private String signUpBody(String username) {
        return """
                { "username": "%s", "password": "Password1!", "name": "정세희",
                  "age": 20, "phoneNumber": "01012341234", "school": "학교" }
                """.formatted(username);
    }

    private void stubSuccessfulSignUp() {
        doReturn(1L).when(studentService).signUpStudent(any());
        doReturn(new Token("token")).when(tokenService).createToken(1L, Role.STUDENT);
    }

    @DisplayName("대문자 username을 DTO 단계에서 막지 않는다 (정규화는 도메인의 몫이다)")
    @Test
    void 대문자는_DTO에서_막히지_않는다() throws Exception {
        stubSuccessfulSignUp();

        mockMvc.perform(MockMvcRequestBuilders.post("/students")
                        .contentType(MediaType.APPLICATION_JSON).content(signUpBody("Abc12")))
                .andExpect(status().isOk());
    }

    @DisplayName("밑줄을 허용한다 (통합 규칙이 geekchat 문자셋을 받아들였다)")
    @Test
    void 밑줄을_허용한다() throws Exception {
        stubSuccessfulSignUp();

        mockMvc.perform(MockMvcRequestBuilders.post("/students")
                        .contentType(MediaType.APPLICATION_JSON).content(signUpBody("abc_12")))
                .andExpect(status().isOk());
    }

    @DisplayName("3자를 허용한다 (최소 길이가 4에서 3으로 넓어졌다)")
    @Test
    void 세글자를_허용한다() throws Exception {
        stubSuccessfulSignUp();

        mockMvc.perform(MockMvcRequestBuilders.post("/students")
                        .contentType(MediaType.APPLICATION_JSON).content(signUpBody("ab1")))
                .andExpect(status().isOk());
    }

    /**
     * 전부 숫자인 username은 {@code memberId}와 육안으로 구분되지 않아 로그와 URL에서 혼동을 만든다.
     * 영문자 최소 1자 요구가 그것을 막는다.
     */
    @DisplayName("전부 숫자면 막는다")
    @Test
    void 전부_숫자는_막는다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/students")
                        .contentType(MediaType.APPLICATION_JSON).content(signUpBody("12345")))
                .andExpect(status().isBadRequest());

        // DTO 검증에서 끊겼다는 증거 — 서비스가 불렸다면 잘못된 값이 도메인까지 간 것이다.
        verifyNoInteractions(studentService);
    }

    @DisplayName("허용되지 않는 문자는 막는다")
    @Test
    void 하이픈은_막는다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/students")
                        .contentType(MediaType.APPLICATION_JSON).content(signUpBody("abc-12")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(studentService);
    }

    @DisplayName("21자는 막는다 (상한 경계)")
    @Test
    void 스물한자는_막는다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/students")
                        .contentType(MediaType.APPLICATION_JSON).content(signUpBody("a".repeat(21))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(studentService);
    }

    @DisplayName("2자는 막는다 (하한 경계)")
    @Test
    void 두글자는_막는다() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/students")
                        .contentType(MediaType.APPLICATION_JSON).content(signUpBody("ab")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(studentService);
    }
}
