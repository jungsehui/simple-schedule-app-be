package com.example.simplescheduleapp.member.presentation;

import com.example.simplescheduleapp.common.auth.Token;
import com.example.simplescheduleapp.common.auth.TokenService;
import com.example.simplescheduleapp.member.application.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void 로그인_성공_컨트롤러_검증() throws Exception {
        when(memberService.login("jungsehui", "Password123!")).thenReturn(1L);
        when(tokenService.createToken(1L)).thenReturn(new Token("randomAccessToken"));

        String json = "{ \"username\" : \"jungsehui\", \"password\" : \"Password123!\" }";

        mockMvc.perform(MockMvcRequestBuilders.post("/login").contentType("application/json").content(json))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.memberId").exists())
                .andExpect(jsonPath("$.accessToken").value("randomAccessToken"))
                .andReturn();
    }
}
