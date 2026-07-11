package com.example.simplescheduleapp.lecture.general.presentation.request;

import com.example.simplescheduleapp.lecture.general.application.command.LectureCreateCommand;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record LectureCreateRequest(
        @NotBlank(message = "강의 제목은 필수입니다.")
        String title,

        @NotNull(message = "시작 시간은 필수입니다.")
        @Future(message = "시작 시간은 현재 이후여야 합니다.")
        LocalDateTime startTime,

        @NotNull(message = "종료 시간은 필수입니다.")
        @Future(message = "종료 시간은 현재 이후여야 합니다.")
        LocalDateTime endTime,

        String memo,

        @Min(value = 1, message = "정원은 1명 이상이어야 합니다.")
        @Max(value = 200, message = "정원은 200명 이하여야 합니다.")
        int capacity
) {

    public LectureCreateCommand toCommand(Long tutorId) {
        return new LectureCreateCommand(tutorId, title, startTime, endTime, memo, capacity);
    }
}
