package com.example.simplescheduleapp.notification.client;

import com.example.simplescheduleapp.common.exception.ApplicationException;
import com.example.simplescheduleapp.common.exception.InternalServerExceptionCode;
import com.example.simplescheduleapp.notification.client.response.GetEnrolledStudentInfosResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor
@Component
public class CourseClient {

    private final RestClient courseClient;

    public GetEnrolledStudentInfosResponse getEnrolledStudentInfosByLectureId(Long lectureId) {
        log.info("Try to get student infos from course server. lecture ID: {}", lectureId);
        GetEnrolledStudentInfosResponse body = courseClient.get()
                .uri("/lectures/" + lectureId + "/student-ids")
                .retrieve()
                .onStatus(is4xxClientError(), handleIs4xxClientError())
                .onStatus(is5xxServerError(), handleIs5xxServerError())
                .body(GetEnrolledStudentInfosResponse.class);
        log.info("Successfully get student infos from course server. GetEnrolledStudentInfosResponse: {}", body);
        return body;
    }

    private static Predicate<HttpStatusCode> is4xxClientError() {
        return HttpStatusCode::is4xxClientError;
    }

    private static RestClient.ResponseSpec.ErrorHandler handleIs4xxClientError() {
        return (request, response) -> {
            log.error("Client Error: {}", response.getStatusCode());
            throw new ApplicationException(InternalServerExceptionCode.EXTERNAL_API_ERROR);
        };
    }

    private static Predicate<HttpStatusCode> is5xxServerError() {
        return HttpStatusCode::is5xxServerError;
    }

    private static RestClient.ResponseSpec.ErrorHandler handleIs5xxServerError() {
        return (request, response) -> {
            log.error("Server Error: {}", response.getStatusCode());
            throw new ApplicationException(InternalServerExceptionCode.EXTERNAL_API_ERROR);
        };
    }
}
