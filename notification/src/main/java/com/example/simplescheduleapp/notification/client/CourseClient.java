package com.example.simplescheduleapp.notification.client;

import com.example.simplescheduleapp.notification.client.response.GetEnrolledStudentInfosResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
@Component
public class CourseClient {

    private final RestClient courseClient;

    public GetEnrolledStudentInfosResponse getEnrolledStudentInfosByLectureId(
            Long lectureId
    ) {
        log.info("Try to get student infos from course server. lecture ID: {}", lectureId);
        GetEnrolledStudentInfosResponse body = courseClient.get()
                .uri("/lectures/" + lectureId + "/student-ids")
                .retrieve()
                .body(GetEnrolledStudentInfosResponse.class);
        log.info("Successfully get student infos from course server. GetEnrolledStudentInfosResponse: {}", body);
        return body;
    }
}
