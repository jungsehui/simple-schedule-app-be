package com.example.simplescheduleapp.tutor.presentation.request;

import com.example.simplescheduleapp.tutor.application.command.TutorSignUpCommand;
import jakarta.validation.constraints.*;

public record TutorSignUpRequest(
        @NotBlank(message = "아이디를 입력해 주세요.")
        // 대문자를 허용하는 것은 의도다. 도메인(Username)의 규칙은 소문자만 받지만, 그건
        // toLowerCase 이후의 정준형을 단언하기 때문이다. 이 @Pattern은 사용자가 방금 보낸
        // 정규화 '전' 입력을 본다. 둘을 하나로 통일하면 대문자 입력이 여기서 400으로 잘려
        // Username.of에 도달하지 못하고, "대문자는 거부가 아니라 정규화" 규칙이 죽는다.
        // 고정 테스트: SignUpUsernameContractTest.
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,20}$",
                message = "아이디는 3~20자의 영문자, 숫자, 밑줄만 쓸 수 있으며 영문자를 최소 하나 포함해야 합니다. 대문자는 소문자로 저장됩니다."
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
