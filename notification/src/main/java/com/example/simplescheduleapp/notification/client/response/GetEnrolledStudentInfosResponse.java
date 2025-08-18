package com.example.simplescheduleapp.notification.client.response;

import java.util.List;

public record GetEnrolledStudentInfosResponse(
        String lectureTitle,
        String lectureMemo,
        List<Long> studentIds
) {
}
