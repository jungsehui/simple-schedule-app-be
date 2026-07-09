package com.example.simplescheduleapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * {@code /internal/**} 서버 간 API에 대한 공유 시크릿 가드 (다층 방어).
 *
 * <p>notification 서버만 호출해야 하는 내부 API를 app_network 내 임의 컨테이너가
 * 호출하는 것을 막는다. nginx가 외부 요청은 이미 403 처리하지만, 내부망 요청은
 * 프록시를 거치지 않으므로 애플리케이션 계층에서 한 번 더 검증한다.
 *
 * <p>정책: 키가 설정되지 않았으면(로컬/테스트) 통과시키고, 키가 설정된 환경(운영)에서만
 * {@code X-Internal-Api-Key} 헤더 일치를 강제한다(fail-open when unset).
 */
@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Api-Key";
    private static final String INTERNAL_PATH_PREFIX = "/internal/";

    private final String configuredKey;

    public InternalApiKeyFilter(@Value("${internal.api.key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX) || !StringUtils.hasText(configuredKey)) {
            chain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(HEADER);
        if (!configuredKey.equals(provided)) {
            log.warn("Rejected internal API call without valid key. uri: {}", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "internal api key required");
            return;
        }

        chain.doFilter(request, response);
    }
}
