package com.example.simplescheduleappback.tutor.presentation.dto.response;

import com.example.simplescheduleappback.tutor.domain.entity.Tutor;

public record ReadTutorResponse(

        String name,
        String phoneNumber,
        String username,
        String password
) {

    public static ReadTutorResponse from(Tutor tutor) {
        return new ReadTutorResponse(
                tutor.getName(),
                tutor.getPhoneNumber(),
                tutor.getUsername(),
                tutor.getPassword()
        );
    }
}
