package com.example.simplescheduleapp.tutor.presentation.request;

import com.example.simplescheduleapp.tutor.application.command.TutorSignUpCommand;
import jakarta.validation.constraints.*;

public record TutorSignUpRequest(
        @NotBlank(message = "아이디를 입력해 주세요.")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9]{4,20}$",
                message = "아이디는 4~20자의 영어 대소문자를 포함해야 하며, 숫자는 선택입니다."
        )
        String username,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,30}$",
                message = "비밀번호는 8~30자의 영어 대소문자, 숫자, 특수문자를 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 20, message = "이름은 최대 20자까지 입력할 수 있습니다.")
        String name,

        @NotNull(message = "나이를 입력해 주세요.")
        @Positive(message = "나이는 양수여야 합니다.")
        int age,

        @NotBlank(message = "휴대폰 번호를 입력해 주세요.")
        @Pattern(
                regexp = "^\\d{11}$",
                message = "전화번호는 11자리 숫자여야 합니다."
        )
        String phoneNumber,

        @NotNull(message = "경력 기간을 입력해 주세요.")
        @PositiveOrZero(message = "경력 기간은 0 이상이어야 합니다.")
        int careerPeriod
) {

    public TutorSignUpCommand toCommand() {
        return new TutorSignUpCommand(username, password, name, age, phoneNumber, careerPeriod);
    }
}
